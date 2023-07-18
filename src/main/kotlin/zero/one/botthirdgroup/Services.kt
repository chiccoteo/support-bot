package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import zero.one.botthirdgroup.MessageDTO.Companion.toDTO
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface UserService {
    fun createOrTgUser(chatId: String): User
    fun update(user: User)
    fun getAll(pageable: Pageable): Page<GetUserDTO>
    fun getUserByRole(role: Role, pageable: Pageable): Page<GetUserDTO>
    fun updateRole(phone: String)
    fun updateLang(phone: String, languages: List<Language>)

}

interface MessageService {

    fun create(messageDTO: MessageDTO): MessageDTO?

    fun getWaitedMessages(chatId: String): List<MessageDTO>?

    fun ratingAndClosingSession(userChatId: String, rate: Double)

}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val languageRepository: LanguageRepository,
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
            role.let { user.role = role }
            botState.let { user.botState = botState }
            languages.let { user.languages = languages }
            phoneNumber?.let { user.phoneNumber = phoneNumber }
        }
        userRepository.save(user)
    }


    override fun getAll(pageable: Pageable) =
        userRepository.findAllByDeletedFalse(pageable).map { GetUserDTO.toDTO(it) }

    override fun getUserByRole(role: Role, pageable: Pageable): Page<GetUserDTO> {
        return userRepository.findByRoleAndDeletedFalse(role, pageable).map { GetUserDTO.toDTO(it) }
    }

    override fun updateRole(phone: String) {
        val user = userRepository.findByPhoneNumberAndDeletedFalse(phone)
        user.role = Role.OPERATOR
        userRepository.save(user)
    }

    override fun updateLang(phone: String, languages: List<Language>) {
        val user = userRepository.findByPhoneNumberAndDeletedFalse(phone)
        user.languages = languages.toMutableList()
        userRepository.save(user)
    }
}

@Service
class MessageServiceImpl(
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository,
    private val messageRepo: MessageRepository,
    private val languageRepository: LanguageRepository,
) : MessageService {
    override fun create(messageDTO: MessageDTO): MessageDTO? {
        messageDTO.run {
            var result: MessageDTO? = null
            userRepo.findByChatIdAndDeletedFalse(senderChatId)?.let { senderUser ->
                sessionRepo.findByStatusTrueAndOperator(senderUser)?.let { session ->
                    session.operator?.let {
                        val toDTO = toDTO(
                            messageRepo.save(
                                Message(
                                    telegramMessageId,
                                    replyTelegramMessageId,
                                    time,
                                    session,
                                    senderUser,
                                    attachment,
                                    text
                                )
                            ), session.operator?.chatId, attachment
                        )
                        result = toDTO
                    }
                }
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
                                    attachment,
                                    text
                                )
                            ), session.operator?.chatId, attachment
                        )
                        result = toDTO
                    }
                } ?: userRepo.findAllByBotStateAndLanguagesContainsAndDeletedFalse(
                    BotState.ONLINE,
                    languageRepository.findByName(senderUser.languages[0].name)
                ).let {
                    if (it.isNotEmpty()) {
                        val toDTO = toDTO(
                            messageRepo.save(
                                Message(
                                    telegramMessageId,
                                    replyTelegramMessageId,
                                    time,
                                    sessionRepo.save(Session(true, senderUser.languages[0], time, senderUser, it[0])),
                                    senderUser,
                                    attachment,
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
                                sessionRepo.save(Session(true, senderUser.languages[0], time, senderUser, null)),
                                senderUser,
                                attachment,
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

    override fun getWaitedMessages(chatId: String): List<MessageDTO>? {
        val operator = userRepo.findByChatIdAndDeletedFalse(chatId)
        var messageDTOs: List<MessageDTO>? = null
        sessionRepo.findAllByStatusTrueAndSessionLanguageInAndOperatorIsNullOrderByTime(operator?.languages).let {
            if (it.isNotEmpty()) {
                it[0]?.let { session ->
                    session.operator = operator
                    sessionRepo.save(session)
                    val messages = messageRepo.findAllBySessionAndDeletedFalseOrderByTime(session)
                    messageDTOs = messages.map { message ->
                        toDTO(
                            message,
                            chatId,
                            message.attachment
                        )
                    }
                }
            }
        }
        return messageDTOs
    }

    override fun ratingAndClosingSession(userChatId: String, rate: Double) {
        userRepo.findByChatIdAndDeletedFalse(userChatId)?.let {
            sessionRepo.findByStatusTrueAndUser(it)?.run {
                this.operator?.run {
                    this.rate = (this.rate + rate) / 2
                    userRepo.save(this)
                }
                this.status = false
                sessionRepo.save(this)
            }
        }
    }

}

@Service
interface AttachmentService {
//    fun download()
}

@Service
class AttachmentServiceImpl(
    private val attachmentRepo: AttachmentRepository,
) : AttachmentService {

}
