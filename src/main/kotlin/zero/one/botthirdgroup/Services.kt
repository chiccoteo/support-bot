package zero.one.botthirdgroup

import org.springframework.stereotype.Service

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