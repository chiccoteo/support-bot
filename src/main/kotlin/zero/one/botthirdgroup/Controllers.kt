package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(private val service: UserService) {
    @GetMapping
    fun getAll(pageable: Pageable): Page<GetUserDTO> = service.getAll(pageable)

    @GetMapping("{role}")
    fun getUserByRole(@PathVariable role: Role, pageable: Pageable): Page<GetUserDTO> =
        service.getUserByRole(role, pageable)

    @PutMapping("update-role/{phone}")
    fun updateRole(@PathVariable phone: String) = service.updateRole(phone)

    @PutMapping("update-lang/{phone}")
    fun updateLang(@PathVariable phone: String, languages:List<Language>) = service.updateLang(phone,languages)
}