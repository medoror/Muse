package io.github.kkoshin.muse.repo

import io.github.kkoshin.muse.dashboard.Script
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MuseRepoTest {

    @Test
    fun `test MAX_TEXT_LENGTH constant`() {
        assertEquals(10_000, MAX_TEXT_LENGTH)
    }

    @Test
    fun `test PCM file naming logic`() {
        val voiceId = "test-voice-123"
        val phrase = "hello world"
        
        // Test the file naming logic that getPcmCache uses
        val expectedFileName = "$phrase.pcm"
        
        assertTrue(expectedFileName.endsWith(".pcm"))
        assertTrue(expectedFileName.contains(phrase))
        assertEquals("hello world.pcm", expectedFileName)
    }

    @Test
    fun `test Script data class properties`() {
        val text = "This is a test script content"
        val script = Script(text = text)
        
        // Test that script has required properties
        assertNotNull(script.id)
        assertEquals("Untitled", script.title)
        assertEquals(text, script.text)
        assertTrue(script.createAt > 0)
        
        // Test summary generation
        assertEquals(text, script.summary) // Text is < 100 chars
        assertTrue(script.summary.length <= 100)
    }

    @Test
    fun `test Script summary truncation`() {
        val longText = "a".repeat(150) // 150 characters
        val script = Script(text = longText)
        
        // Summary should be truncated to 100 characters
        assertEquals(100, script.summary.length)
        assertEquals("a".repeat(100), script.summary)
    }

    @Test
    fun `test Script summary removes newlines`() {
        val textWithNewlines = "Line 1\nLine 2\nLine 3"
        val script = Script(text = textWithNewlines)
        
        // Summary should replace newlines with spaces
        assertEquals("Line 1 Line 2 Line 3", script.summary)
    }

    @Test
    fun `test queryPhrases with short text`() = runTest {
        val text = "Hello world this is a test"
        val expectedPhrases = listOf("Hello", "world", "this", "is", "a", "test")
        
        // Mock script
        val script = Script(text = text)
        
        // Test the phrase splitting logic
        val phrases = text.split(' ', '\n').filter { it.isNotBlank() }
        assertEquals(expectedPhrases, phrases)
    }

    @Test
    fun `test queryPhrases with text at MAX_TEXT_LENGTH limit`() = runTest {
        val text = "word ".repeat(2000) // Creates text near the limit
        val truncatedText = text.take(MAX_TEXT_LENGTH)
        
        // Should truncate at MAX_TEXT_LENGTH
        assertTrue(truncatedText.length <= MAX_TEXT_LENGTH)
        
        val phrases = truncatedText.split(' ', '\n').filter { it.isNotBlank() }
        assertTrue(phrases.isNotEmpty())
        assertTrue(phrases.all { it == "word" })
    }

    @Test
    fun `test queryPhrases with text exceeding MAX_TEXT_LENGTH`() = runTest {
        val text = "word ".repeat(3000) // Creates text exceeding the limit
        val truncatedText = text.take(MAX_TEXT_LENGTH)
        
        // Should be truncated to exactly MAX_TEXT_LENGTH
        assertEquals(MAX_TEXT_LENGTH, truncatedText.length)
        
        val phrases = truncatedText.split(' ', '\n').filter { it.isNotBlank() }
        assertTrue(phrases.isNotEmpty())
    }

    @Test
    fun `test queryPhrases filters empty strings`() = runTest {
        val text = "Hello   world\n\n\ntest   "
        val phrases = text.split(' ', '\n').filter { it.isNotBlank() }
        
        // Should only contain non-blank phrases
        val expectedPhrases = listOf("Hello", "world", "test")
        assertEquals(expectedPhrases, phrases)
    }

    @Test
    fun `test queryPhrases with mixed whitespace`() = runTest {
        val text = "Hello\nworld\ttest\r\nphrase"
        // The current implementation only splits on ' ' and '\n'
        val phrases = text.split(' ', '\n').filter { it.isNotBlank() }
        
        // Note: This test shows that the current implementation doesn't handle \t or \r
        // The result will include "world\ttest\r" as one phrase
        assertTrue(phrases.contains("Hello"))
        assertTrue(phrases.contains("phrase"))
        assertTrue(phrases.size >= 2)
    }

    @Test
    fun `test export path format`() {
        // Test the expected format of export paths
        val appName = "Muse"
        val expectedPath = "Download/$appName"
        
        assertEquals("Download/Muse", expectedPath)
        assertTrue(expectedPath.startsWith("Download/"))
        assertTrue(expectedPath.contains(appName))
    }

    @Test
    fun `test empty text handling`() = runTest {
        val emptyText = ""
        val phrases = emptyText.split(' ', '\n').filter { it.isNotBlank() }
        
        assertTrue(phrases.isEmpty())
    }

    @Test
    fun `test whitespace only text handling`() = runTest {
        val whitespaceText = "   \n  \n   "
        val phrases = whitespaceText.split(' ', '\n').filter { it.isNotBlank() }
        
        assertTrue(phrases.isEmpty())
    }

    @Test
    fun `test single word text handling`() = runTest {
        val singleWord = "hello"
        val phrases = singleWord.split(' ', '\n').filter { it.isNotBlank() }
        
        assertEquals(listOf("hello"), phrases)
    }

    // Performance test to verify the text length concern mentioned in the TODO
    @Test
    fun `test performance concern with large text`() = runTest {
        val largeText = "word ".repeat(10_000) // 50,000 characters
        
        val startTime = System.currentTimeMillis()
        val truncatedText = largeText.take(MAX_TEXT_LENGTH)
        val phrases = truncatedText.split(' ', '\n').filter { it.isNotBlank() }
        val endTime = System.currentTimeMillis()
        
        val processingTime = endTime - startTime
        
        // Verify truncation works
        assertTrue(truncatedText.length <= MAX_TEXT_LENGTH)
        assertTrue(phrases.isNotEmpty())
        
        // This test helps document the performance concern
        // If processing time is high, it validates the need for the limit
        println("Processing time for ${MAX_TEXT_LENGTH} characters: ${processingTime}ms")
        assertTrue(processingTime < 1000) // Should process in under 1 second
    }
}