package com.mosquishe.today.domain

/** Auto-complete logic for a to-do that owns a checklist. Pure → unit-testable. */
object Completion {

    /**
     * Final completed state for a to-do.
     *
     * When [autoCompleteEnabled] and the to-do has a checklist, completion is driven entirely by the
     * checklist: complete iff every item is done (unchecking an item reopens the to-do). Otherwise
     * the user's [manualCompleted] toggle wins.
     */
    fun deriveCompleted(
        manualCompleted: Boolean,
        checklistDone: List<Boolean>,
        autoCompleteEnabled: Boolean,
    ): Boolean =
        if (autoCompleteEnabled && checklistDone.isNotEmpty()) {
            checklistDone.all { it }
        } else {
            manualCompleted
        }
}
