package com.cintra.app.data

import com.cintra.app.ui.model.ChatSession

/**
 * Clean data abstraction for conversations. Phase 1 ships an in-memory mock
 * (see [mock.MockChatRepository]); Phase 2 swaps in a Room-backed repository
 * without touching the UI.
 */
interface ChatRepository {

    /** Initial list of sessions (seeded demo conversation + empty chat). */
    suspend fun getInitialSessions(): List<ChatSession>

    /** Creates and persists a fresh empty session. */
    suspend fun createSession(title: String): ChatSession
}