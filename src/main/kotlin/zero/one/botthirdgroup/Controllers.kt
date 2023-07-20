package zero.one.botthirdgroup

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@ControllerAdvice
class ExceptionHandlers(
    private val errorMessageSource: ResourceBundleMessageSource
) {
    @ExceptionHandler(BotException::class)
    fun handleException(exception: BotException): ResponseEntity<*> {
        return when (exception) {

            is UserNotFoundException -> ResponseEntity.badRequest()
                .body(exception.getErrorMessage(errorMessageSource, exception.phone))
        }
    }
}
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(private val service: UserService) {
    @GetMapping
    fun getAll(pageable: Pageable): Page<GetUserDTO> = service.getAll(pageable)

    @GetMapping("/users")
    fun getUsers(pageable: Pageable): Page<GetUserDTO> =
        service.getUsers(pageable)
    @GetMapping("/operators")
    fun getOperators(pageable: Pageable): Page<GetUserDTO> =
        service.getOperators(pageable)

    @PutMapping("/{phone}")
    fun updateRole(@PathVariable phone: String) = service.updateRole(phone)

    @PutMapping
    fun updateLang(@RequestBody dto : LanguageUpdateDTO) = service.updateLang(dto)
}