package com.perceptionparadox.oneura.data.reflection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaReflectionTest {

    @Test
    fun parsesEachKindFromRawJson() {
        val observation = MetaReflection.parse(
            """{"kind":"OBSERVATION","note":"The user is asking for shorter answers.","strategy":""}"""
        )
        assertEquals(MetaReflectionKind.OBSERVATION, observation!!.kind)
        assertEquals("The user is asking for shorter answers.", observation.note)

        val critique = MetaReflection.parse(
            """{"kind":"critique","content":"Replies have been too abstract.","strategy":""}"""
        )
        assertEquals(MetaReflectionKind.CRITIQUE, critique!!.kind)
        assertEquals("Replies have been too abstract.", critique.note)

        val shift = MetaReflection.parse(
            """{"kind":"strategy shift","note":"Match their pace.","strategy":"Answer in two sentences unless asked for more."}"""
        )
        assertEquals(MetaReflectionKind.STRATEGY_SHIFT, shift!!.kind)
        assertEquals("Answer in two sentences unless asked for more.", shift.strategy)
    }

    @Test
    fun stripsFencesAndSurroundingChatter() {
        val raw = """
            Here is the reflection:
            ```json
            {"kind":"CRITIQUE","note":"You interrupted the user.","strategy":""}
            ```
        """.trimIndent()
        val parsed = MetaReflection.parse(raw)
        assertEquals(MetaReflectionKind.CRITIQUE, parsed!!.kind)
        assertEquals("You interrupted the user.", parsed.note)
    }

    @Test
    fun missingKindDefaultsToObservationAndUnknownKindToCritique() {
        val missing = MetaReflection.parse("""{"note":"A quiet pattern.","strategy":""}""")
        assertEquals(MetaReflectionKind.OBSERVATION, missing!!.kind)

        val unknown = MetaReflection.parse("""{"kind":"mood","note":"Something else.","strategy":""}""")
        assertEquals(MetaReflectionKind.CRITIQUE, unknown!!.kind)
    }

    @Test
    fun proseWithoutJsonIsKeptAsCritique() {
        val parsed = MetaReflection.parse("I have been answering before I understand the question.")
        assertEquals(MetaReflectionKind.CRITIQUE, parsed!!.kind)
        assertEquals("I have been answering before I understand the question.", parsed.note)
    }

    @Test
    fun emptyAndBrokenJsonAreRejected() {
        assertNull(MetaReflection.parse("   "))
        assertNull(MetaReflection.parse("""{"kind":"OBSERVATION","note":"","strategy":""}"""))
        assertNull(MetaReflection.parse("{not json"))
    }

    @Test
    fun strategyShiftStoresTheDirectiveOnItsOwnLine() {
        val stored = MetaReflection.storedContent(
            ParsedMetaReflection(
                kind = MetaReflectionKind.STRATEGY_SHIFT,
                note = "The last few replies wandered.",
                strategy = "Stay with the user's last question."
            )
        )
        assertEquals(
            "The last few replies wandered.\n\nStrategy: Stay with the user's last question.",
            stored
        )
        assertEquals("Stay with the user's last question.", MetaReflection.strategyLine(stored!!))
    }

    @Test
    fun triggerSourceRoundTripsAndLabels() {
        val source = MetaReflection.triggerSource(MetaReflection.ORIGIN_AUTO, MetaReflectionKind.STRATEGY_SHIFT)
        assertEquals("META_AUTO:STRATEGY_SHIFT", source)
        assertEquals(MetaReflectionKind.STRATEGY_SHIFT, MetaReflection.kindOf(source))
        assertTrue(MetaReflection.isMeta(source))
        assertEquals("Strategy shift", MetaReflection.displayLabel(source))
        assertEquals("Inner voice", MetaReflection.displayLabel("AUTO_MONOLOGUE"))
        assertNull(MetaReflection.kindOf("AUTO_MONOLOGUE"))
    }

    @Test
    fun guidanceUsesTheNewestStrategyAndSkipsInnerVoice() {
        val addendum = MetaReflection.guidanceAddendum(
            listOf(
                TaggedReflection("META_MANUAL:STRATEGY_SHIFT", "Old habit.\n\nStrategy: Ask one clarifying question."),
                TaggedReflection("META_AUTO:CRITIQUE", "Too many metaphors."),
                TaggedReflection("AUTO_MONOLOGUE", "This monologue should not enter the prompt."),
                TaggedReflection("META_AUTO:OBSERVATION", "They prefer concrete examples."),
                TaggedReflection("META_AUTO:STRATEGY_SHIFT", "Strategy: Be verbose and abstract.")
            )
        )
        assertTrue(addendum.contains("Strategy: Ask one clarifying question."))
        assertTrue(addendum.contains("Critique: Too many metaphors."))
        assertTrue(addendum.contains("Observation: They prefer concrete examples."))
        assertFalseContained(addendum, "Be verbose")
        assertFalseContained(addendum, "monologue")
    }

    @Test
    fun guidanceIsEmptyWhenThereAreNoMetaEntries() {
        assertEquals(
            "",
            MetaReflection.guidanceAddendum(listOf(TaggedReflection("AUTO_MONOLOGUE", "hidden motive")))
        )
    }

    @Test
    fun systemPromptNamesTheThreeTags() {
        assertTrue(MetaReflection.SYSTEM_PROMPT.contains("OBSERVATION"))
        assertTrue(MetaReflection.SYSTEM_PROMPT.contains("CRITIQUE"))
        assertTrue(MetaReflection.SYSTEM_PROMPT.contains("STRATEGY_SHIFT"))
        assertEquals(4, MetaReflection.EVERY_N_TURNS)
    }

    private fun assertFalseContained(text: String, snippet: String) {
        assertTrue("Did not expect [$snippet] in:\n$text", !text.contains(snippet))
    }
}
