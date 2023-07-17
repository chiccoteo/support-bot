package zero.one.botthirdgroup

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/message")
class MessageController(
    private var service: MessageService
) {

    @GetMapping
    fun get(): GetMessageDTO = service.get()

}