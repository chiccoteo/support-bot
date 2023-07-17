package zero.one.botthirdgroup

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestControllerAdvice
class ExceptionHandlers(
    private val errorMessageSource: ResourceBundleMessageSource,
) {
    @ExceptionHandler(BotException::class)
    fun handleException(exception: BotException): ResponseEntity<*> {
        return when (exception) {
            is ChatIdExistsException -> ResponseEntity.badRequest()
                .body(exception.getErrorMessage(errorMessageSource, exception.chatId))

            is UserNotFoundException -> ResponseEntity.badRequest()
                .body(exception.getErrorMessage(errorMessageSource, exception.chatId))
        }
    }
}

@RestController
@RequestMapping("api/v1/user")
class UserController(private val service: UserService) {
    @PostMapping("{chatId}")
    fun create(@PathVariable chatId: String) {
        val dto = UserCreateDto(chatId)
        service.create(dto)
    }

    @PutMapping("{chatId}")
    fun update(@PathVariable chatId: String, @RequestBody dto: UserUpdateDto) = service.update(chatId, dto)

    @GetMapping("get-by-role/{role}")
    fun getUserByRole(@PathVariable role: Role) = service.getUserByRole(role)

   @GetMapping("get-by-bot-state/{botState}")
   fun getUserByBotState(@PathVariable botState: BotState) = service.getUserByBotState(botState)
}