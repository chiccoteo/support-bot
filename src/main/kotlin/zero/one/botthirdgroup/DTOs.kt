package zero.one.botthirdgroup

import java.math.BigDecimal

data class BaseMessage(val code: Int, val message: String?)
data class UserCreateDto(
    val chatId:String
){
//    fun toEntity() = User(chatId)
}
data class UserUpdateDto(
    val userName: String?,
    val fullname: String?,
)

data class GetOneUserDto(
    val userName: String,
    val fullname: String,
    val balance: BigDecimal,
) {
//    companion object {
//        fun toDto(user: User): GetOneUserDto {
//            return user.run {
//                GetOneUserDto(userName, fullName, balance)
//            }
//        }
//    }

}