package com.mosquishe.today.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionTest {

    @Test fun `auto-complete on with all items done completes`() {
        assertTrue(Completion.deriveCompleted(false, listOf(true, true, true), autoCompleteEnabled = true))
    }

    @Test fun `auto-complete on with some undone does not complete`() {
        assertFalse(Completion.deriveCompleted(true, listOf(true, false), autoCompleteEnabled = true))
    }

    @Test fun `auto-complete on with empty checklist falls back to manual`() {
        assertTrue(Completion.deriveCompleted(true, emptyList(), autoCompleteEnabled = true))
        assertFalse(Completion.deriveCompleted(false, emptyList(), autoCompleteEnabled = true))
    }

    @Test fun `auto-complete off always uses manual flag`() {
        assertFalse(Completion.deriveCompleted(false, listOf(true, true), autoCompleteEnabled = false))
        assertTrue(Completion.deriveCompleted(true, listOf(true, false), autoCompleteEnabled = false))
    }
}
