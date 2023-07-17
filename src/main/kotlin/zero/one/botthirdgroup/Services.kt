package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

interface UserService {
    fun create(dto: UserCreateDto)
    fun update(id: Long, dto: UserUpdateDto)
    fun getOne(id: Long): GetOneUserDto
    fun getAll(pageable: Pageable): Page<GetOneUserDto>
    fun delete(id: Long)
}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun create(dto: UserCreateDto) {
        dto.run {
            if (userRepository.existsByChatId(chatId)) throw ChatIdExistsException(chatId)
//            userRepository.save(toEntity())
        }
    }

    override fun update(id: Long, dto: UserUpdateDto) {
        TODO("Not yet implemented")
    }

    override fun getOne(id: Long): GetOneUserDto {
        TODO("Not yet implemented")
    }

    override fun getAll(pageable: Pageable): Page<GetOneUserDto> {
        TODO("Not yet implemented")
    }

    override fun delete(id: Long) {
        TODO("Not yet implemented")
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
    private val attachmentContentRepo: AttachmentContentRepository
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

    fun saveAttachment(file: MultipartFile): Attachment {
        val attachment = attachmentRepo.save(
            Attachment(
                file.originalFilename ?: "",
                file.contentType ?: "",
                file.size
            )
        )
        attachmentContentRepo.save(
            AttachmentContent(
                file.bytes,
                attachment
            )
        )
        return attachment
    }


    override fun get(): GetMessageDTO {
        TODO("Not yet implemented")
    }
}