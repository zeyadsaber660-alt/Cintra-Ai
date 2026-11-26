package com.cintra.app.data

import com.cintra.app.ui.model.AiStreamChunk
import com.cintra.app.ui.model.AiStreamRequest
import kotlinx.coroutines.flow.Flow

/**
 * Clean abstraction over the AI backend. Phase 1 ships a realistic Arabic/Kotlin
 * streaming simulator (see [mock.MockAiStreamService]); Phase 2 swaps in a Ktor
 * SSE client with zero UI changes.
 */
interface AiStreamService {

    /** Streams a reply for [request] as incremental [AiStreamChunk]s. */
    fun streamReply(request: AiStreamRequest): Flow<AiStreamChunk>
}