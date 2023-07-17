package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

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