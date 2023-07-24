package zero.one.botthirdgroup

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
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

    fun update(messageId: Int, executeMessageId: Int?)

    fun editMessage(editMessage: org.telegram.telegrambots.meta.api.objects.Message): Message?

    fun getReplyMessageId(senderChatId: String, messageId: Int): Int?

    fun create(messageDTO: MessageDTO): MessageDTO?

    fun getWaitedMessages(chatId: String): List<MessageDTO>?

    fun getUserFromSession(operatorChatId: String): String

    fun getOperatorFromSession(userChatId: String): String

    fun ratingOperator(sessionId: Long, rate: Byte)

    fun closingSession(operatorChatId: String)

    fun getSessionByOperator(operatorChatId: String): Session?

    fun isThereNotRatedSession(userChatId: String): Session

    fun isThereOneMessageInSession(userChatId: String): Boolean

    fun getMessageById(messageId: Int): Message?

    fun updateMessage(message: Message)

}

interface SessionService {
    fun getOperatorAvgRate(): List<GetOperatorAvgRateDTO>

}

interface MessageSourceService {
    fun getMessage(sourceKey: LocalizationTextKey, languageEnum: LanguageEnum): String
    fun getMessage(sourceKey: LocalizationTextKey, any: Array<String>, languageEnum: LanguageEnum): String
}

@Service
class SessionServiceImpl(
    private val sessionRepo: SessionRepository,
    private val userRepository: UserRepository,
) : SessionService {
    override fun getOperatorAvgRate(): List<GetOperatorAvgRateDTO> {
        val list = sessionRepo.getOperatorAvgRate()
        val response = LinkedList<GetOperatorAvgRateDTO>()
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
    private val messageSourceService: MessageSourceService,
) : UserService {

    @Value("\${telegram.bot.token}")
    private val token: String = ""

    override fun createOrTgUser(chatId: String): User {
        return userRepository.findByChatIdAndDeletedFalse(chatId)
            ?: userRepository.save(
                User(
                    chatId,
                    mutableListOf(languageRepository.findByName(LanguageEnum.UZ))
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

        val message =
            messageSourceService.getMessage(LocalizationTextKey.CLICK_THE_START_COMMAND, user.languages[0].name)
        val restTemplate = RestTemplate()
        restTemplate.getForObject(
            "https://api.telegram.org/bot$token/sendMessage?chat_id=${user.chatId}&text=" + message + " /start",
            String::class.java
        )
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
    override fun update(messageId: Int, executeMessageId: Int?) {
        messageRepo.findByTelegramMessageIdAndDeletedFalse(messageId).run {
            this?.executeTelegramMessageId = executeMessageId
            messageRepo.save(this!!)
        }
    }

    override fun editMessage(editMessage: org.telegram.telegrambots.meta.api.objects.Message): Message? {
        messageRepo.findByTelegramMessageIdAndDeletedFalse(editMessage.messageId)?.run {
            text = editMessage.text
            return messageRepo.save(this)
        }
        return null
    }

    override fun getReplyMessageId(senderChatId: String, messageId: Int): Int? {
        val session: Session = if (userRepo.findByChatIdAndDeletedFalse(senderChatId)?.role == Role.USER)
            sessionRepo.findByStatusTrueAndUserChatId(senderChatId)!!
        else
            sessionRepo.findByStatusTrueAndOperatorChatId(senderChatId)!!

        messageRepo.findBySessionAndTelegramMessageIdAndDeletedFalse(
            session,
            messageId
        )?.executeTelegramMessageId?.let {
            return it
        }
        messageRepo.findBySessionAndExecuteTelegramMessageIdAndDeletedFalse(
            session,
            messageId
        )?.telegramMessageId?.let {
            return it
        }

        return null
    }

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
                                    executeTelegramMessageId,
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
                                    executeTelegramMessageId,
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
                                    executeTelegramMessageId,
                                    time,
                                    sessionRepo.save(
                                        Session(
                                            true,
                                            senderUser.languages[0],
                                            time,
                                            0,
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
                                executeTelegramMessageId,
                                time,
                                sessionRepo.findByStatusTrueAndUser(senderUser) ?: sessionRepo.save(
                                    Session(
                                        true,
                                        senderUser.languages[0],
                                        time,
                                        0,
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
                for (session in it) {
                    if (session?.user?.role == Role.OPERATOR) {
                        session.status = false
                        sessionRepo.save(session)
                    } else {
                        session?.operator = operator
                        sessionRepo.save(session!!)
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

    override fun ratingOperator(sessionId: Long, rate: Byte) {
        sessionRepo.findByIdAndDeletedFalse(sessionId)?.run {
            this.rate = rate
            sessionRepo.save(this)
        }
    }

    override fun closingSession(operatorChatId: String) {
        userRepo.findByChatIdAndDeletedFalse(operatorChatId)?.let {
            sessionRepo.findByStatusTrueAndOperator(it)?.run {
                this.status = false
                sessionRepo.save(this)
            }
        }
    }

    override fun getSessionByOperator(operatorChatId: String): Session? {
        return sessionRepo.findByStatusTrueAndOperatorChatId(operatorChatId)!!
    }

    override fun isThereNotRatedSession(userChatId: String): Session {
        return sessionRepo.findByUserChatIdAndRateAndDeletedFalse(userChatId, 0)
    }

    override fun isThereOneMessageInSession(userChatId: String): Boolean {
        sessionRepo.findByStatusTrueAndUserChatId(userChatId)?.run {
            if (messageRepo.findAllBySessionAndDeletedFalse(this).size == 1)
                return true
        }
        return false
    }

    override fun getMessageById(messageId: Int): Message? {
        return messageRepo.findByTelegramMessageIdAndDeletedFalse(messageId)
    }

    override fun updateMessage(message: Message) {
        messageRepo.save(message)
    }

}

@Service
class MessageSourceServiceImpl(val messageResourceBundleMessageSource: ResourceBundleMessageSource) :
    MessageSourceService {
    override fun getMessage(sourceKey: LocalizationTextKey, languageEnum: LanguageEnum): String {
        return messageResourceBundleMessageSource.getMessage(sourceKey.name, null, languageEnum.toLocale())
    }

    override fun getMessage(sourceKey: LocalizationTextKey, any: Array<String>, languageEnum: LanguageEnum): String {
        return messageResourceBundleMessageSource.getMessage(sourceKey.name, any, languageEnum.toLocale())
    }
}
