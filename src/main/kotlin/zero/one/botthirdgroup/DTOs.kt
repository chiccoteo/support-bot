package zero.one.botthirdgroup

import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.sql.Timestamp

data class MessageCreateDTO(
    val telegramMessageId: Long,
    val time: Timestamp,
    val senderChatId: String,
    val text: String?,
    val attachment: MultipartFile?
) {
    fun toEntity(session: Session, user: User, attachment: Attachment) =
        Message(telegramMessageId, time, session, user, text, attachment)
}

data class GetMessageDTO(
    val telegramMessageId: Long,
    val time: Timestamp,
    val sessionId: Long,
    val senderChatId: String,
    val text: String?,
    val attachmentId: Long?
)


data class BaseMessage(val code: Int, val message: String?)
data class UserCreateDto(
    val chatId: String
) {
//    fun toEntity() = User(chatId)
}

data class UserUpdateDto(
    val userName: String?,
    val fullname: String?,
)

data class GetOneUserDto(
    val userName: String,
    val fullname: String,
    val balance: BigDecimal,
) {
//    companion object {
//        fun toDto(user: User): GetOneUserDto {
//            return user.run {
//                GetOneUserDto(userName, fullName, balance)
//            }
//        }
//    }

}