package zero.one.botthirdgroup

import org.springframework.stereotype.Service

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