package zero.one.botthirdgroup

import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface UserService {
    fun createOrTgUser(chatId: String): User
    fun update(user: User)

}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun createOrTgUser(chatId: String): User {
        return userRepository.findByChatIdAndDeletedFalse(chatId)
            ?: userRepository.save(User(chatId))
    }

    override fun update(user: User) {
        user.run {
            role?.let { user.role = role }
            botState.let { user.botState = botState }
            languages.let { user.languages = languages }
            phoneNumber?.let { user.phoneNumber = phoneNumber }
        }
        userRepository.save(user)
    }
}

interface MessageService {

    fun create(messageCreateDTO: MessageCreateDTO): String

    fun get(): GetMessageDTO

}

@Service
class MessageServiceImpl(
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository,
    private val messageRepo: MessageRepository,
    private val attachmentRepo: AttachmentRepository,
) : MessageService {
    override fun create(messageCreateDTO: MessageCreateDTO): String {
        /*messageCreateDTO.run {
            val user = userRepo.findByChatIdAndDeletedFalse(senderChatId)
            val language = user.languages?.get(0) ?: Language(LanguageName.ENG)
            sessionRepo.findByChatUserChatIdOrChatOperatorChatIdAndStatusTrue(senderChatId, senderChatId)?.let {
                attachment?.run {
                    messageRepo.save(toEntity(it, user, saveAttachment(this)))
                }
                if (senderChatId != it.chat.operator.chatId)
                    return it.chat.operator.chatId
                else
                    return it.chat.user.chatId
            } ?: userRepo.findAllByBotStateAndLanguagesIsContainingAndDeletedFalse(BotState.ONLINE, language).run {
                this?.forEach {
                    if (!sessionRepo.existsByStatusTrueAndChatOperator(it)) {
                        sessionRepo.findByChatUserChatIdAndChatOperatorChatId(
                            messageCreateDTO.senderChatId,
                            it.chatId
                        )?.let {
                            attachment?.run {
                                messageRepo.save(toEntity(it, user, saveAttachment(this)))
                            }
                        }
                    }
                }
            }

        }*/
        TODO("Not yet implemented")

    }


    override fun get(): GetMessageDTO {
        TODO("Not yet implemented")
    }
}

interface AttachmentService {
    fun create(fileId: String, fileName: String)
}

class AttachmentServiceImpl(
    private val botService: TelegramBotService
) : AttachmentService {
    override fun create(fileId: String, fileName: String) {
        val strings = fileName.split(".")
        val fromTelegram = botService.getFromTelegram(fileId, botService.botToken)
        val path = Paths.get(
            "files/" +
                    UUID.randomUUID().toString() + "." + strings[strings.size - 1]
        )
        Files.copy(ByteArrayInputStream(fromTelegram), path)
        // Save db attachment
    }
}
