package com.cintra.app.data.mock

import com.cintra.app.data.ChatRepository
import com.cintra.app.ui.model.ChatSession

/**
 * Clean session store starting fresh without hardcoded template chats.
 */
class MockChatRepository : ChatRepository {

    override suspend fun getInitialSessions(): List<ChatSession> = listOf(
        ChatSession(
            id = "cintra-session-1",
            title = "محادثة جديدة",
            messages = emptyList(),
        ),
    )

    override suspend fun createSession(title: String): ChatSession = ChatSession(
        id = "cintra-${System.nanoTime()}",
        title = title,
        messages = emptyList(),
    )
}