# 0006. A deck is generated under a foreground service

## Context

The generation ran in the application's coroutine scope, which survives screen changes but not the process. When the user switched to
another app, Android could kill the cached process, taking the run with it. Together with 0005 this meant a paid-for run could be
lost to nothing more than answering a message.

## Decision

While a generation runs, the app runs `GenerationService` in the foreground, with a notification that shows the progress and has a
Pause button. The manager asks a `KeepAlive` to start it whenever a run starts or resumes, from the user's tap, which is when the system
allows starting a foreground service; the service follows the generation state and stops itself when the state is no longer Running,
leaving a notification that says how the run ended. A partial wake lock with a timeout keeps the processor awake with the screen off.
The service has type `dataSync`; on Android 15 and later the system limits such services to a few hours a day, and when it signals the
timeout the service pauses the run, which is resumable.

WorkManager was rejected: it is meant for deferrable work, adds a dependency, and the run is user-initiated, interactive and
already has its own state and storage. A service that merely hosts the existing manager changes the least.

## Consequences

- Android 13 and later ask for the permission to show notifications; denying it hides the notification but does not stop the service.
- The process can still be killed (low memory, force stop, reboot); then 0005 applies: the run is restored as paused.
- Four permissions are added: foreground service, its data-sync type, wake lock and notifications.
