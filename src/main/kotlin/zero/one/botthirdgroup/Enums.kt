package zero.one.botthirdgroup

enum class Role {
    ADMIN,
    OPERATOR,
    USER
}

enum class LanguageName {
    UZ,
    ENG,
    RU
}

enum class BotState {
    START,
    ONLINE
}

enum class ErrorCode(val code: Int){
    CHAT_ID_EXISTS(100)
}