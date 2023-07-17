package zero.one.botthirdgroup

import java.sql.Timestamp

data class MessageCreateDTO(
    val telegramMessageId: Long,
    val time: Timestamp,
    val senderChatId: Long,
    val text: String?,
    val attachmentsId: List<Long>?
)

data class GetMessageDTO(
    val telegramMessageId: Long,
    val time: Timestamp,
    val sessionId: Long,
    val senderChatId: Long,
    val text: String?,
    val attachmentsId: List<Long>?
)

data class GetToMessageDTO(
    val toUserId: Long,
    val text: String?,
    val attachmentsId: List<Long>?
)

data class UserUpdateDto(
    val role: Role?,
    val languages: List<Language>?,
    val botState: BotState?,
    val phoneNumber: String?
)