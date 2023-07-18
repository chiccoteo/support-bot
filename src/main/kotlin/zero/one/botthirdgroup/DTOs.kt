package zero.one.botthirdgroup

import java.sql.Timestamp

data class MessageCreateDTO(
    val telegramMessageId: Long,
    val replyTelegramMessageId: Long,
    val time: Timestamp,
    val senderChatId: Long,
    val text: String?,
)

data class GetMessageDTO(
    val telegramMessageId: Long,
    val time: Timestamp,
    val sessionId: Long,
    val senderChatId: Long,
    val text: String?,
    val attachmentsId: List<Long>?
)
