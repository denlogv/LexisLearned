# AGENTS.md

Rules for anyone (human or AI agent) changing this repository. They are enforced by tooling, and the same checks run on every
pull request and before every release (see `.github/workflows/`). A change is not done until the checklist at the bottom passes.

## Project in one paragraph

LexisLearned is an Android vocabulary trainer (Kotlin, Jetpack Compose, Room). Decks are organised as book → part → chapter →
word. A study session takes every word through every selected mode. Decks come from `.lexis` files or from an EPUB via an
LLM with the user's own API key. See `README.md` for users and `CONTRIBUTING.md` for the layout.

## Commands

Use a JDK between 17 and 22 (Gradle 8.9 does not run on newer JDKs; the JDK bundled with Android Studio may be newer, so check).
After editing the custom detekt rule in `detekt-rules/`, run `./gradlew --stop`: the detekt worker caches the old rule.

| Task | Command |
| --- | --- |
| Format everything (like `ruff format`) | `./gradlew ktlintFormat` |
| Lint and static analysis (like `ruff check`); includes the documentation and size rules | `./gradlew ktlintCheck detekt` |
| Tests of the custom detekt rule | `./gradlew :detekt-rules:test` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Coverage report (HTML and XML) | `./gradlew jacocoTestReport` → `app/build/reports/jacoco/` |
| Coverage gate (fails below 80% lines) | `./gradlew jacocoTestCoverageVerification` |
| Build | `./gradlew assembleDebug` |

## Code rules

### 1. Document everything

Every class, object, interface and function has a KDoc comment, whether it is public or private, an override or a composable.

- Start with a one-sentence summary of what it is or does and why it exists; do not just repeat the name.
- Document every parameter with `@param name`, every primary-constructor property with `@param` or `@property`, the result with
  `@return` (unless `Unit`), and thrown exceptions with `@throws`.
- Explain non-obvious behaviour, units, nullability and edge cases; skip the obvious.

detekt fails on any undocumented declaration or missing `@param`: `KDocCompleteness` is this project's own rule (`detekt-rules/`),
and detekt's `OutdatedDocumentation` catches documentation that no longer matches the signature.

### 2. No god methods, no god classes

- A function has at most **30 lines of code** (blank lines and comments do not count), cyclomatic complexity at most 12 and
  nesting depth at most 4. Composables follow the same limit: extract sub-composables.
- A class or object has at most **200 lines** and a single responsibility. If its description needs "and", split it.
- Prefer small pure functions with explicit inputs and outputs. Pass dependencies in (constructor or parameter), do not reach for
  globals, so tests can substitute them.
- One concern per file. Screens, view models and pure helpers live in separate files.

### 3. No logic in composables

Composables only render state and forward events. Anything that decides something (filtering, grouping, counting, formatting,
scheduling, parsing) belongs in a view model or a plain function that unit tests can reach.

### 4. Formatting and lint

The code is formatted by ktlint (configured in `.editorconfig`) and analysed by detekt (configured in `config/detekt.yml`).
Run `./gradlew ktlintFormat` before committing. Do not silence a warning without a comment that says why; prefer fixing it.

## Tests and coverage

- **Line coverage must be at least 80%**, enforced by `jacocoTestCoverageVerification`.
- The gate measures all code except what cannot run in a JVM unit test: generated code (Room, serialization), Compose screens
  and theme (`ui/**/*Screen*`, `*Components*`, `Theme*`), `MainActivity`, `LexisLearnedApp` and `KeystoreSecrets` (Android Keystore).
  Keep those thin (rule 3) so little logic hides there. The exclusion list lives in `app/build.gradle.kts`.
- Every new or changed function gets tests, including failure paths. A bug fix starts with a failing test.
- Android APIs (Room, SharedPreferences, `Context`) run under Robolectric; prefer a real in-memory Room database and small fakes over
  mocking frameworks. Network code is tested against a local test server on a plain socket, never a real provider.
- Tests are deterministic: inject the clock and `Random`, use `kotlinx-coroutines-test`, no sleeps.
- Test data is generated. **Never put text from copyrighted books into the repository**, including test fixtures and screenshots.

## Commits and pull requests

The history of `main` is meant to be read, so it is curated before it is merged (rebase-and-merge, no squash, no merge commits).

- **One logical change per commit.** Do not bundle unrelated changes. Each commit builds and passes the checks on its own, so
  `git bisect` works. Code, its tests and its documentation go in the same commit.
- **Conventional Commits**: `type(scope): subject` with type `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `build`, `ci`,
  `style`, `chore` or `revert`. The subject is imperative, at most 72 characters, without a trailing period.
- **The body says why**, not what the diff already shows: the problem, the choice made and what was rejected. `feat`, `fix`,
  `refactor` and `perf` commits must have one.
- **No leftovers**: no `fixup!`, `squash!`, `wip` or "address review comments" commits. Fold fixes into the commit they belong to
  (`git commit --fixup` then `git rebase --autosquash`) before asking for a merge.
- Keep pull requests small enough to review in one sitting. A large change is split into a series of pull requests, or at least of
  commits that can be reviewed one by one.
- Check the series locally with `tools/verify-commits.sh origin/main..HEAD`, which runs the checks on every commit.
- `tools/check-commits.sh` (also run in CI) checks the commit messages.

## Secrets and releases

- Never commit keystores, passwords, API keys or `keystore.properties`. Never print or echo a secret, and do not read files that may
  contain one. Release signing reads passwords from the macOS Keychain or environment variables (`docs/RELEASING.md`).
- Releases are built by the `release` workflow from a `v*` tag after the same checks as pull requests. A tag containing a hyphen
  (for example `v0.1.0-pre1`) is published as a pre-release.
- Database changes need a Room migration and a test. Existing users' progress must survive updates.

## Working as an agent

### No shortcuts: clean code, clean design, clean architecture

- Prefer the clean solution over the quick one. Do not trade clean code, a clean design or a clean architecture for a smaller diff,
  fewer files or less work.
- A lint, size or complexity limit that a change runs into is a signal about the design, not an obstacle. Fix the design: split the
  class, extract the function, give the responsibility a home of its own. Do not raise a threshold, add a `@Suppress` or tuck the code
  somewhere convenient. A suppression is only for a warning that is wrong in this case, with a comment saying why it is wrong;
  "the proper fix is bigger than my change" is not a reason.
- At every level from a function up to a module, just do it correctly, without asking: split a class that has grown a second
  responsibility, rename what is misleading, remove duplication you introduced, give a new concern its own class, move code to where it
  belongs, change an interface and update all its callers. Do it as part of the change, in a commit of its own where it is a separate
  step. How many files this touches is not a reason to ask or to cut a corner.
- Ask first only when the request would mean refactoring the entire codebase: a sweeping change to how most of the app is built, such
  as replacing the architecture pattern, the state management, the persistence or networking stack, or the navigation approach
  everywhere. Say so before starting. State plainly what it costs (how much of the code is touched, risk to existing behaviour,
  effort), name the realistic options including the proper one, recommend one, and let the user choose. Never quietly pick the cheap
  variant, and never quietly take the expensive one.
- Be upfront afterwards too: if something is a shortcut, a compromise or unfinished, say so plainly in the report and the pull request.

### Everyday rules

- Keep the scope of a change small: solve the task and do not refactor unrelated code in the same change. Scope is not quality: in the
  code the change touches, follow the rules above.
- Do not commit, push, tag or publish unless asked to.
- Say in the pull request that an AI agent wrote the change (see the pull request template); do not hide it and do not overstate
  how much was verified.
- Report honestly: say what was checked, what was not, and what failed.

## Definition of done

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck detekt :detekt-rules:test testDebugUnitTest jacocoTestCoverageVerification lintDebug assembleDebug
```

All of it passes, new behaviour has tests, and `README.md` / `CONTRIBUTING.md` are updated if the change is visible to users or
contributors.
