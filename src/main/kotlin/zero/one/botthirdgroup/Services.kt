package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

interface UserService {
    fun create(dto: UserCreateDto)
    fun update(chatId: String, dto: UserUpdateDto)

    fun getUserByRole(role: Role): List<GetOneUserDto>

    //    fun getUserByLang(language: Language): List<GetOneUserDto>
    fun getUserByBotState(botState: BotState): List<GetOneUserDto>

}

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun create(dto: UserCreateDto) {
        dto.run {
            if (userRepository.existsByChatId(chatId)) throw ChatIdExistsException(chatId)
            userRepository.save(toEntity())
        }
    }

    override fun update(chatId: String, dto: UserUpdateDto) {
        val user = userRepository.findByChatId(chatId) ?: throw UserNotFoundException(chatId)
        dto.run {
            role?.let { user.role = role }
            botState?.let { user.botState = botState }
            languages?.let { user.languages = languages as MutableList<Language> }
        }
        userRepository.save(user)
    }

    override fun getUserByRole(role: Role) =
        userRepository.getUserByRole(role).map { GetOneUserDto.toDto(it) }


//    override fun getUserByLang(language: Language): List<GetOneUserDto> {
//
//    }

    override fun getUserByBotState(botState: BotState) =
        userRepository.getUserByBotState(botState).map { GetOneUserDto.toDto(it) }

    override fun getAll(pageable: Pageable): Page<GetOneUserDto> {
        TODO("Not yet implemented")
    }

    override fun delete(id: Long) {
        TODO("Not yet implemented")
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