package zero.one.botthirdgroup

import java.math.BigDecimal

data class BaseMessage(val code: Int, val message: String?)
data class UserCreateDto(
    val chatId: String
) {
    fun toEntity() = User(chatId)
}

data class UserUpdateDto(
    val role: Role?,
    val languages: List<Language>?,
    val botState: BotState?
)

data class GetOneUserDto(
    val name: String,
    val chatId: String,
    val phoneNumber: String,
    val role: Role
) {
    companion object {
        fun toDto(user: User): GetOneUserDto {
            return user.run {
                GetOneUserDto(name!!, chatId, phoneNumber!!, role!!)
            }
        }
    }

}