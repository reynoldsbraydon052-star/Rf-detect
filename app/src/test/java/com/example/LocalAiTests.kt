package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalAiTests {

    private lateinit var db: AiMemoryDatabase
    private lateinit var dao: AiMemoryDao
    private lateinit var repository: AiMemoryRepository
    private lateinit var embeddingProvider: EmbeddingProvider
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AiMemoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.aiMemoryDao()
        embeddingProvider = DevelopmentEmbeddingProvider()
        repository = AiMemoryRepositoryImpl(dao, embeddingProvider, memoryHistoryLimit = 5)
    }

    @After
    fun teardown() {
        db.close()
    }

    // ==========================================
    // 1. MODEL CATALOG & CONFIGURATION TESTS
    // ==========================================

    @Test
    fun testModelCatalogValidation() {
        assertNotNull(ModelCatalog.MODELS)
        assertEquals(4, ModelCatalog.MODELS.size)
        
        // Assert keys exist
        val ids = ModelCatalog.MODELS.map { it.id }
        assertTrue(ids.contains(ModelCatalog.MODEL_ID_LLAMA_3_2_3B))
        assertTrue(ids.contains(ModelCatalog.MODEL_ID_PHI_3_5_MINI))
        assertTrue(ids.contains(ModelCatalog.MODEL_ID_GEMMA_2_2B))
        assertTrue(ids.contains(ModelCatalog.MODEL_ID_QWEN_2_5_1_5B))
    }

    @Test
    fun testInvalidModelUrlsAndConfiguration() {
        for (model in ModelCatalog.MODELS) {
            assertTrue("URL should be a secure HuggingFace link", model.url.startsWith("https://huggingface.co/"))
            assertTrue("URL should end with the GGUF model filename", model.url.endsWith(model.filename))
            assertTrue("Expected extension must be .gguf", model.filename.endsWith(".gguf"))
        }
    }

    // ==========================================
    // 2. MODEL MANAGER & DOWNLOAD STATE TESTS
    // ==========================================

    @Test
    fun testDownloadStateTransitions() {
        val manager = ModelManager(context)
        val testModel = ModelCatalog.MODELS.first()
        
        // Ensure initial state is NOT_INSTALLED
        val info = manager.getModelStatus(testModel)
        assertEquals(DownloadStatus.NOT_INSTALLED, info.status)
        assertFalse(manager.isModelInstalled(testModel))
    }

    @Test
    fun testDuplicateDownloadPrevention() {
        val manager = ModelManager(context)
        val testModel = ModelCatalog.MODELS.first()
        
        // Enqueuing once
        val started = manager.startDownload(testModel)
        assertTrue(started)

        val dId = manager.getDownloadId(testModel.id)
        val status = manager.getModelStatus(testModel)
        System.out.println("DIAGNOSTIC - Download ID: " + dId + ", Status: " + status.status)
        
        // Enqueuing duplicate when already active should be prevented
        val startedAgain = manager.startDownload(testModel)
        assertFalse("Duplicate download was not prevented! ID: " + dId + ", Status: " + status.status, startedAgain)
    }

    @Test
    fun testMissingModelFile() {
        val manager = ModelManager(context)
        val testModel = ModelCatalog.MODELS.first()
        
        // Ensure undownloaded model correctly reports false for file verification
        assertFalse(manager.isModelInstalled(testModel))
        val file = manager.getModelFile(testModel)
        assertFalse(file.exists())
    }

    // ==========================================
    // 3. ROOM MEMORY OPERATIONS TESTS
    // ==========================================

    @Test
    fun testRoomMemoryInsertionAndRetrieval(): Unit = runBlocking {
        val embedding = FloatArray(128) { 0.5f }
        val entity = AiMemoryEntity(
            targetId = "test_mac_address",
            deviceType = "ROUTER",
            protocol = "WIFI_6",
            displayName = "Gateway Router",
            sanitizedAddress = "00:11:22:33:44:55",
            rssi = -45,
            timestamp = System.currentTimeMillis(),
            anomalySummary = "High RSSI Jump detected",
            measurementSummary = "Distance: 1.2m",
            embedding = embedding
        )

        repository.saveMemory(entity)

        val all = repository.getAllMemories()
        assertEquals(1, all.size)
        assertEquals("test_mac_address", all[0].targetId)
        assertEquals("Gateway Router", all[0].displayName)
        assertArrayEquals(embedding, all[0].embedding, 0.001f)
    }

    @Test
    fun testHistoryLimitAndStaleMemoryPruning(): Unit = runBlocking {
        // limit is 5 in setup
        for (i in 1..7) {
            val entity = AiMemoryEntity(
                targetId = "mac_$i",
                deviceType = "DEVICE",
                protocol = "BLE",
                displayName = "Beacon $i",
                sanitizedAddress = "00:11:22:33:44:0$i",
                rssi = -70,
                timestamp = i.toLong(), // Order by timestamp
                anomalySummary = null,
                measurementSummary = null,
                embedding = FloatArray(128) { i.toFloat() / 10f }
            )
            repository.saveMemory(entity)
        }

        val all = repository.getAllMemories()
        // Must contain maximum of 5 records, pruning the 2 oldest ones
        assertEquals(5, all.size)
        // Ensure oldest ones (timestamp 1 and 2) are removed, leaving timestamps 3 to 7
        val targets = all.map { it.targetId }
        assertFalse(targets.contains("mac_1"))
        assertFalse(targets.contains("mac_2"))
        assertTrue(targets.contains("mac_7"))
    }

    // ==========================================
    // 4. EMBEDDINGS & SIMILARITY CHECK TESTS
    // ==========================================

    @Test
    fun testEmbeddingNormalization() {
        val text = "SignalRadar local RF scanning co-pilot query"
        val embedding = runBlocking { embeddingProvider.embed(text) }
        
        assertEquals(128, embedding.size)
        
        // Assert L2 normalized unit vector (sum of squares is close to 1)
        var sumSquares = 0f
        for (v in embedding) {
            sumSquares += v * v
        }
        assertEquals(1.0f, sumSquares, 0.01f)
    }

    @Test
    fun testCosineSimilarityValidMatrix() {
        val vec1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vec2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        val vec3 = floatArrayOf(1.0f, 0.0f, 0.0f)

        // Orthogonal
        assertEquals(0.0f, cosineSimilarity(vec1, vec2), 0.001f)
        // Same vector
        assertEquals(1.0f, cosineSimilarity(vec1, vec3), 0.001f)
    }

    @Test
    fun testEmbeddingDimensionMismatch() {
        val vec1 = floatArrayOf(0.1f, 0.2f)
        val vec2 = floatArrayOf(0.1f, 0.2f, 0.3f)

        assertThrows(IllegalArgumentException::class.java) {
            cosineSimilarity(vec1, vec2)
        }
    }

    @Test
    fun testNaNAndInfinityEmbeddingRejection() {
        val vecNormal = floatArrayOf(0.1f, 0.2f, 0.3f)
        val vecNaN = floatArrayOf(0.1f, Float.NaN, 0.3f)
        val vecInf = floatArrayOf(Float.POSITIVE_INFINITY, 0.2f, 0.3f)

        assertThrows(IllegalArgumentException::class.java) {
            cosineSimilarity(vecNormal, vecNaN)
        }

        assertThrows(IllegalArgumentException::class.java) {
            cosineSimilarity(vecNormal, vecInf)
        }
    }

    // ==========================================
    // 5. RAG RETRIEVAL & PROMPT ASSEMBLY TESTS
    // ==========================================

    @Test
    fun testTopKRetrievalAndTargetFiltering(): Unit = runBlocking {
        val baseVector = embeddingProvider.embed("Specific sensor node")
        
        // Add 3 matches
        val memory1 = AiMemoryEntity(
            targetId = "target_A",
            deviceType = "SENSOR",
            protocol = "UWB",
            displayName = "Sensor Node Delta",
            sanitizedAddress = "FF:FF:FF:01",
            rssi = -30,
            timestamp = 100L,
            anomalySummary = "Channel saturation",
            measurementSummary = "Distance: 0.5m",
            embedding = baseVector
        )
        val memory2 = AiMemoryEntity(
            targetId = "target_B",
            deviceType = "CONTROLLER",
            protocol = "WIFI",
            displayName = "Substation Controller",
            sanitizedAddress = "FF:FF:FF:02",
            rssi = -85,
            timestamp = 200L,
            anomalySummary = null,
            measurementSummary = null,
            embedding = baseVector
        )

        repository.saveMemory(memory1)
        repository.saveMemory(memory2)

        // Filter by targetId A only
        val matchesForA = repository.getMemoriesByTargetId("target_A")
        assertEquals(1, matchesForA.size)
        assertEquals("Sensor Node Delta", matchesForA[0].displayName)
    }

    @Test
    fun testEmptyRagContextPromptConstruction(): Unit = runBlocking {
        val mockInference = object : LocalInferenceEngine {
            var lastSys: String = ""
            var lastUser: String = ""
            override suspend fun generate(systemPrompt: String, userPrompt: String): String {
                lastSys = systemPrompt
                lastUser = userPrompt
                return "Mock generated response"
            }
        }

        val ragService = LocalAiRagService(repository, mockInference, embeddingProvider)
        
        // Query empty repository
        val response = ragService.query("Scan for hidden rogue drones", similarityThreshold = 0.5f)
        
        assertEquals("Mock generated response", response)
        assertTrue(mockInference.lastSys.contains("SignalRadar Local AI"))
        // If no relevant memory exists, use no historical context section
        assertFalse(mockInference.lastUser.contains("HISTORICAL CONTEXT:"))
        assertFalse(mockInference.lastUser.contains("MODEL LIMITATIONS:"))
        assertTrue(mockInference.lastUser.contains("CURRENT OBSERVATION:"))
    }

    @Test
    fun testContextLimitsAndPromptSeparations(): Unit = runBlocking {
        val vector = embeddingProvider.embed("Unregistered Wi-Fi AP detected")
        
        // Save matching memories
        repository.saveMemory(
            AiMemoryEntity(
                targetId = "rogue_ap_01",
                deviceType = "ROUTER",
                protocol = "WIFI_5",
                displayName = "Rogue Office Router",
                sanitizedAddress = "DE:AD:BE:EF:01:02",
                rssi = -80,
                timestamp = 500L,
                anomalySummary = "Channel conflict on 2.4GHz",
                measurementSummary = "Distance: 12.3m",
                embedding = vector
            )
        )

        val mockInference = object : LocalInferenceEngine {
            var lastUser: String = ""
            override suspend fun generate(systemPrompt: String, userPrompt: String): String {
                lastUser = userPrompt
                return "Offline audit completed"
            }
        }

        val ragService = LocalAiRagService(repository, mockInference, embeddingProvider)
        ragService.query("Unregistered Wi-Fi AP detected", similarityThreshold = 0.2f)

        // Ensure distinct headers
        assertTrue("User prompt must isolate CURRENT OBSERVATION", mockInference.lastUser.contains("CURRENT OBSERVATION:"))
        assertTrue("User prompt must isolate HISTORICAL CONTEXT", mockInference.lastUser.contains("HISTORICAL CONTEXT:"))
        assertTrue("Prompt must include MODEL LIMITATIONS warning", mockInference.lastUser.contains("MODEL LIMITATIONS:"))
        assertTrue("Prompt must list matching historical event parameters", mockInference.lastUser.contains("Rogue Office Router"))
    }

    @Test
    fun testInferenceFailureHandling(): Unit = runBlocking {
        val mockFailInference = object : LocalInferenceEngine {
            override suspend fun generate(systemPrompt: String, userPrompt: String): String {
                throw IllegalStateException("Model weights corrupt or file inaccessible.")
            }
        }

        val ragService = LocalAiRagService(repository, mockFailInference, embeddingProvider)
        val result = ragService.query("Trigger immediate diagnostic audit")
        assertTrue(result.contains("ERROR: Local RAG Pipeline failed - Inference Engine Exception"))
    }

    @Test
    fun testEmbeddingFailureHandling(): Unit = runBlocking {
        val failingEmbedding = object : EmbeddingProvider {
            override suspend fun embed(text: String): FloatArray {
                throw RuntimeException("Out of memory during vectorization")
            }
        }
        val mockInference = object : LocalInferenceEngine {
            override suspend fun generate(systemPrompt: String, userPrompt: String): String = "Ok"
        }
        val ragService = LocalAiRagService(repository, mockInference, failingEmbedding)
        val result = ragService.query("Trigger immediate diagnostic audit")
        assertTrue(result.contains("ERROR: Local RAG Pipeline failed - Embedding Generation Failure"))
    }

    @Test
    fun testIrrelevantMemoryPruning(): Unit = runBlocking {
        val vectorA = floatArrayOf(1f, 0f, 0f)
        val vectorB = floatArrayOf(0f, 1f, 0f) // Orthogonal (irrelevant)

        val dummyEmbedding = object : EmbeddingProvider {
            override suspend fun embed(text: String): FloatArray = vectorA
        }

        repository.saveMemory(
            AiMemoryEntity(
                targetId = "irrelevant_target",
                deviceType = "PHONE",
                protocol = "BLE",
                displayName = "Far Beacon",
                sanitizedAddress = "00:11:22",
                rssi = -90,
                timestamp = 100L,
                anomalySummary = null,
                measurementSummary = null,
                embedding = vectorB
            )
        )

        val mockInference = object : LocalInferenceEngine {
            var lastUser: String = ""
            override suspend fun generate(systemPrompt: String, userPrompt: String): String {
                lastUser = userPrompt
                return "Ok"
            }
        }

        val ragService = LocalAiRagService(repository, mockInference, dummyEmbedding)
        ragService.query("My query", similarityThreshold = 0.5f)

        assertFalse("Irrelevant memories must be excluded", mockInference.lastUser.contains("Far Beacon"))
        assertFalse("Prompt must omit historical context entirely when empty", mockInference.lastUser.contains("HISTORICAL CONTEXT:"))
    }
}
