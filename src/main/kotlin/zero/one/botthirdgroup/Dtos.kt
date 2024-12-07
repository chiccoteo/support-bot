package zero.one.botthirdgroup

import java.sql.Timestamp

data class BaseMessage(val code: Int, val message: String?)
data class MessageDTO(
    val telegramMessageId: Int,
    val replyTelegramMessageId: Int?,
    var executeTelegramMessageId: Int?,
    val time: Timestamp,
    val senderChatId: String,
    val toChatId: String?,
    val text: String?,
    val attachment: Attachment?,
    val messageType: MessageContentType
) {
    companion object {
        fun toDTO(message: Message, toChatId: String?, attachment: Attachment?): MessageDTO {
            return message.run {
                MessageDTO(telegramMessageId, replyTelegramMessageId, executeTelegramMessageId, time, sender.chatId, toChatId, text, attachment, messageType)
            }
        }
    }
}

data class GetUserDTO(
    val id: Long,
    val name: String,
    val phone: String,
    val role: Role,
    val languages: MutableList<Language>,
    val botState: BotState
) {
    companion object {
        fun toDTO(user: User): GetUserDTO {
            return user.run { GetUserDTO(id!!, name!!, phoneNumber!!, role, languages, botState) }
        }
    }
}

data class LanguageUpdateDTO(
    val phoneNumber: String,
    val languages: List<Long>
)

data class GetOperatorAvgRateDTO(
    val operator: User,
    val avgRate: Double
)