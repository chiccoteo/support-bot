package zero.one.botthirdgroup

import java.sql.Timestamp

data class MessageDTO(
    val telegramMessageId: Long,
    val replyTelegramMessageId: Long,
    val time: Timestamp,
    val senderChatId: String,
    val toChatId: String?,
    val text: String?,
    val attachment: Attachment?
) {
    companion object {
        fun toDTO(message: Message, toChatId: String?, attachment: Attachment?): MessageDTO {
            return message.run {
                MessageDTO(telegramMessageId, replyTelegramMessageId, time, sender.chatId, toChatId, text, attachment)
            }
        }
    }
}
