package zero.one.botthirdgroup

import org.springframework.stereotype.Service
import zero.one.botthirdgroup.MessageDTO.Companion.toDTO
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
    private val userRepository: UserRepository,
    private val languageRepository: LanguageRepository
) : UserService {
    override fun createOrTgUser(chatId: String): User {
        return userRepository.findByChatIdAndDeletedFalse(chatId)
            ?: userRepository.save(
                User(
                    chatId,
                    mutableListOf(languageRepository.findByName(LanguageName.UZ))
                )
            )
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

    fun create(messageDTO: MessageDTO): MessageDTO?

    fun getWaitedMessages(chatId: String): List<MessageDTO>

}

@Service
class MessageServiceImpl(
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository,
    private val messageRepo: MessageRepository,
    private val attachmentService: AttachmentService,
) : MessageService {
    override fun create(messageDTO: MessageDTO): MessageDTO? {
        messageDTO.run {
            var result: MessageDTO? = null
            userRepo.findByChatIdAndDeletedFalse(senderChatId)?.let { senderUser ->
                sessionRepo.findByStatusTrueAndUser(senderUser)?.let { session ->
                    session.operator?.let {
                        val toDTO = toDTO(
                            messageRepo.save(
                                Message(
                                    telegramMessageId,
                                    replyTelegramMessageId,
                                    time,
                                    session,
                                    senderUser,
                                    text
                                )
                            ), session.operator?.chatId, attachment
                        )
                        result = toDTO
                    }
                } ?: userRepo.findAllByBotStateAndLanguagesContainsAndDeletedFalse(
                    BotState.ONLINE,
                    Language(senderUser.languages[0].name)
                ).let {
                    if (it.isNotEmpty()) {
                        val toDTO = toDTO(
                            messageRepo.save(
                                Message(
                                    telegramMessageId,
                                    replyTelegramMessageId,
                                    time,
                                    Session(true, senderUser.languages[0], senderUser, it[0]),
                                    senderUser,
                                    text
                                )
                            ), it[0]?.chatId, attachment
                        )
                        result = toDTO
                    } else {
                        messageRepo.save(
                            Message(
                                telegramMessageId,
                                replyTelegramMessageId,
                                time,
                                Session(true, senderUser.languages[0], senderUser, null),
                                senderUser,
                                text
                            )
                        )
                        result = null
                    }
                }
            }
            return result
        }
    }

    override fun getWaitedMessages(chatId: String): List<MessageDTO> {
        val operator = userRepo.findByChatIdAndDeletedFalse(chatId)
        sessionRepo.findAllByStatusTrueAndSessionLanguageInAndOperatorIsNull(operator?.languages).let {
            it[0]?.let { session ->
                session.operator = operator
                sessionRepo.save(session)
                messageRepo.findAllBySessionAndDeletedFalseOrderByTime(
                    session
                ).forEach {
//                    toDTO(it, chatId, attachmentService.create(it))
                }
            }
        }
        TODO("Not yet implemented")
    }

}

interface AttachmentService {
}

@Service
class AttachmentServiceImpl(
    private val attachmentRepo: AttachmentRepository
) : AttachmentService {

}
