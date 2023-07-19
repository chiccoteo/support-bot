package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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