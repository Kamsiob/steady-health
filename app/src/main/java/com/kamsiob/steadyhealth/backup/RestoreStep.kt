package com.kamsiob.steadyhealth.backup

/**
 * Where putting a backup back has got to.
 *
 * Reading the file and replacing everything are two steps with a question in
 * between, on purpose. The app reads the file first so that it can say what it is
 * and when it was written, and so that a file it cannot use is turned away before
 * anybody is asked to agree to anything. Nothing is touched until [Asking] is
 * answered.
 */
sealed interface RestoreStep {

    /** Nothing going on. */
    data object Idle : RestoreStep

    /** A backup this app can use is open, and the person has not answered yet. */
    data class Asking(val backup: Backup) : RestoreStep

    /** Answered, and being put back. Not a question any more, so nothing to press. */
    data object Working : RestoreStep

    /** The file cannot be used, and [why] is the sentence to say about it. */
    data class Refused(val why: BackupRead) : RestoreStep

    /** It is back. */
    data object Done : RestoreStep
}
