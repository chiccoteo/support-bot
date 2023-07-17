package zero.one.botthirdgroup

import org.springframework.stereotype.Service

interface UserService {
    fun create(chatId: String): User
    fun update(chatId: String, dto: UserUpdateDto)

}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun create(chatId: String): User {
        return userRepository.save(User(chatId))
    }

    override fun update(chatId: String, dto: UserUpdateDto) {
        val user = userRepository.findByChatIdAndDeletedFalse(chatId)
        dto.run {
            role?.let { user.role = role }
            botState?.let { user.botState = botState }
            languages?.let { user.languages = languages as MutableList<Language> }
            phoneNumber?.let { user.phoneNumber = phoneNumber }
        }
        userRepository.save(user)
    }

}


interface MessageService {

    fun create(messageCreateDTO: MessageCreateDTO): GetToMessageDTO

    fun get(): GetMessageDTO

}

@Service
class MessageServiceImpl() : MessageService {
    override fun create(messageCreateDTO: MessageCreateDTO): GetToMessageDTO {
        TODO("Not yet implemented")
    }

    override fun get(): GetMessageDTO {
        TODO("Not yet implemented")
    }
}