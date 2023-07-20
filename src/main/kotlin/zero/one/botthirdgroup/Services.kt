package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import zero.one.botthirdgroup.MessageDTO.Companion.toDTO
import java.util.LinkedList

interface UserService {
    fun createOrTgUser(chatId: String): User
    fun update(user: User)
    fun getAll(pageable: Pageable): Page<GetUserDTO>
    fun getUsers(pageable: Pageable): Page<GetUserDTO>
    fun getOperators(pageable: Pageable): Page<GetUserDTO>
    fun updateRole(phone: String)
    fun updateLang(dto: LanguageUpdateDTO)
}

interface MessageService {

    fun create(messageDTO: MessageDTO): MessageDTO?

    fun getWaitedMessages(chatId: String): List<MessageDTO>?

    fun getUserFromSession(operatorChatId: String): String

    fun getOperatorFromSession(userChatId: String): String

//    fun ratingOperator(operatorChatId: String, rate: Double)

    fun closingSession(operatorChatId: String)

}

interface SessionService {
    fun getOperatorAvgRate(): List<GetOperatorAvgRateDTO>

}

@Service
class SessionServiceImpl(
    private val sessionRepo: SessionRepository,
    private val userRepository: UserRepository
) : SessionService {
    override fun getOperatorAvgRate(): List<GetOperatorAvgRateDTO> {
        val list = sessionRepo.getOperatorAvgRate()
        var response = LinkedList<GetOperatorAvgRateDTO>()
        for (operatorAvgRateMapper in list) {
            val operator = userRepository.findByIdAndDeletedFalse(operatorAvgRateMapper.getOperatorId())
            response.add(GetOperatorAvgRateDTO(operator!!, operatorAvgRateMapper.getAvgRate()))
        }
        return response
    }
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

    override fun getUsers(pageable: Pageable): Page<GetUserDTO> {
        return userRepository.findByRoleAndDeletedFalse(Role.USER, pageable).map {
            GetUserDTO.toDTO(it)
        }
    }

    override fun getOperators(pageable: Pageable): Page<GetUserDTO> {
        return userRepository.findByRoleAndDeletedFalse(Role.OPERATOR, pageable).map {
            GetUserDTO.toDTO(it)
        }
    }

    override fun updateRole(phone: String) {
        val user = userRepository.findByPhoneNumberAndDeletedFalse(phone) ?: throw UserNotFoundException(phone)
        user.role = Role.OPERATOR
        user.botState = BotState.OFFLINE
        userRepository.save(user)
    }

    override fun updateLang(dto: LanguageUpdateDTO) {
        val user = userRepository.findByPhoneNumberAndDeletedFalse(dto.phoneNumber)
            ?: throw UserNotFoundException(dto.phoneNumber)
        user.languages = languageRepository.findAllById(dto.languages)
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
                                    messageType,
                                    text
                                )
                            ), session.user.chatId, attachment
                        )
                        result = toDTO
                    }
                } ?: sessionRepo.findByStatusTrueAndUser(senderUser)?.let { session ->
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
                                    messageType,
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
                                    sessionRepo.save(
                                        Session(
                                            true,
                                            senderUser.languages[0],
                                            time,
                                            0.0,
                                            senderUser,
                                            it[0]
                                        )
                                    ),
                                    senderUser,
                                    attachment,
                                    messageType,
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
                                sessionRepo.findByStatusTrueAndUser(senderUser) ?: sessionRepo.save(
                                    Session(
                                        true,
                                        senderUser.languages[0],
                                        time,
                                        0.0,
                                        senderUser,
                                        null
                                    )
                                ),
                                senderUser,
                                attachment,
                                messageType,
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

    override fun getUserFromSession(operatorChatId: String): String {
        var userChatId = ""
        userRepo.findByChatIdAndDeletedFalse(operatorChatId)?.let {
            sessionRepo.findByStatusTrueAndOperator(it)?.run {
                userChatId = this.user.chatId
            }
        }
        return userChatId
    }

    override fun getOperatorFromSession(userChatId: String): String {
        var operatorChatId = ""
        userRepo.findByChatIdAndDeletedFalse(userChatId)?.let {
            sessionRepo.findByStatusTrueAndUser(it)?.run {
                this.operator?.let { operator ->
                    operatorChatId = operator.chatId
                }
            }
        }
        return operatorChatId
    }

//    override fun ratingOperator(operatorChatId: String, rate: Double) {
//        userRepo.findByChatIdAndDeletedFalse(operatorChatId)?.let {
//            it.rate = (it.rate + rate) / 2
//            userRepo.save(it)
//        }
//    }

    override fun closingSession(operatorChatId: String) {
        userRepo.findByChatIdAndDeletedFalse(operatorChatId)?.let {
            sessionRepo.findByStatusTrueAndOperator(it)?.run {
                this.status = false
                sessionRepo.save(this)
            }
        }
    }

}
