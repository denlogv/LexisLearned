package dev.denlogv.lexislearned.service

/**
 * What a start of the generation service asks for about pausing: nothing, or one of the two pauses that the notification offers.
 *
 * @property action the action of the intent that carries the request, or null for no request.
 */
enum class PauseRequest(val action: String?) {
    /** No pause: the service was started to follow a generation that begins or goes on. */
    NONE(null),

    /** Pause once the section in progress is done. */
    AFTER_SECTION("dev.denlogv.lexislearned.PAUSE_AFTER_SECTION"),

    /** Pause at once, giving up the request in flight. */
    NOW("dev.denlogv.lexislearned.PAUSE_NOW"),
    ;

    /** Where the requests come from. */
    companion object {
        /**
         * Reads the request from the action of a start intent.
         *
         * @param action the action, or null.
         * @return the request that carries this action, or [NONE] for any other.
         */
        fun fromAction(action: String?): PauseRequest = entries.firstOrNull { it.action != null && it.action == action } ?: NONE
    }
}
