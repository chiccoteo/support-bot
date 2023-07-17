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
    START
}

enum class ErrorCode(val code: Int) {
    CHAT_ID_EXISTS(100),
    USER_NOT_FOUND(101)
}