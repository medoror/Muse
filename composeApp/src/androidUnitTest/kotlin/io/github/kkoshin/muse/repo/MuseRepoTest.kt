package io.github.kkoshin.muse.repo

import io.github.kkoshin.muse.dashboard.Script
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MuseRepoTest {

    private lateinit var fakeScriptDao: FakeScriptDao
    private lateinit var fakeFileManager: FakeFileManager
    private lateinit var tempDir: File
    private lateinit var museRepo: MuseRepo

    @Before
    fun setUp() {
        tempDir = createTempDir("muse_test")
        fakeScriptDao = FakeScriptDao()
        fakeFileManager = FakeFileManager(tempDir)
        museRepo = MuseRepo(fakeScriptDao, fakeFileManager)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
        fakeScriptDao.clear()
    }

    @Test
    fun `insertScript and queryScript should work correctly`() = runTest {
        val script = Script(text = "Test script content")
        
        // Insert the script
        museRepo.insertScript(script)
        
        // Query it back
        val retrievedScript = museRepo.queryScript(script.id)
        
        assertNotNull(retrievedScript)
        assertEquals(script.id, retrievedScript.id)
        assertEquals(script.text, retrievedScript.text)
        assertEquals(script.title, retrievedScript.title)
        assertEquals(script.createAt, retrievedScript.createAt)
    }

    @Test
    fun `queryScript with non-existent ID should return null`() = runTest {
        val nonExistentId = UUID.randomUUID()
        
        val result = museRepo.queryScript(nonExistentId)
        
        assertNull(result)
    }

    @Test
    fun `queryAllScripts should return empty list initially`() = runTest {
        val scripts = museRepo.queryAllScripts()
        
        assertTrue(scripts.isEmpty())
    }

    @Test
    fun `queryAllScripts should return all inserted scripts`() = runTest {
        val script1 = Script(text = "First script")
        val script2 = Script(text = "Second script")
        
        museRepo.insertScript(script1)
        museRepo.insertScript(script2)
        
        val scripts = museRepo.queryAllScripts()
        
        assertEquals(2, scripts.size)
        assertTrue(scripts.any { it.id == script1.id })
        assertTrue(scripts.any { it.id == script2.id })
    }

    @Test
    fun `deleteScript should remove script from database`() = runTest {
        val script = Script(text = "Script to delete")
        
        // Insert and verify it exists
        museRepo.insertScript(script)
        assertNotNull(museRepo.queryScript(script.id))
        
        // Delete and verify it's gone
        museRepo.deleteScript(script.id)
        assertNull(museRepo.queryScript(script.id))
    }

    @Test
    fun `getPcmCache should return correct file path`() {
        val voiceId = "test-voice"
        val phrase = "hello world"
        
        val file = museRepo.getPcmCache(voiceId, phrase)
        
        assertTrue(file.path.contains(voiceId))
        assertEquals("$phrase.pcm", file.name)
        assertTrue(file.path.endsWith("$phrase.pcm"))
    }

    @Test
    fun `getPcmCache should create voice directory`() {
        val voiceId = "new-voice"
        val phrase = "test phrase"
        
        val file = museRepo.getPcmCache(voiceId, phrase)
        
        assertTrue(file.parentFile?.exists() == true)
        assertEquals(voiceId, file.parentFile?.name)
    }

    @Test
    fun `getPcmCache with different voices should create separate directories`() {
        val voice1 = "voice1"
        val voice2 = "voice2"
        val phrase = "same phrase"
        
        val file1 = museRepo.getPcmCache(voice1, phrase)
        val file2 = museRepo.getPcmCache(voice2, phrase)
        
        assertEquals("$phrase.pcm", file1.name)
        assertEquals("$phrase.pcm", file2.name)
        assertEquals(voice1, file1.parentFile?.name)
        assertEquals(voice2, file2.parentFile?.name)
        assertTrue(file1.path != file2.path)
    }


    @Test
    fun `multiple insert and delete operations should work correctly`() = runTest {
        val scripts = listOf(
            Script(text = "Script 1"),
            Script(text = "Script 2"),
            Script(text = "Script 3")
        )
        
        // Insert all scripts
        scripts.forEach { museRepo.insertScript(it) }
        
        // Verify all exist
        assertEquals(3, museRepo.queryAllScripts().size)
        scripts.forEach { 
            assertNotNull(museRepo.queryScript(it.id))
        }
        
        // Delete middle script
        museRepo.deleteScript(scripts[1].id)
        
        // Verify correct scripts remain
        val remaining = museRepo.queryAllScripts()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.id == scripts[0].id })
        assertTrue(remaining.any { it.id == scripts[2].id })
        assertNull(museRepo.queryScript(scripts[1].id))
    }
}