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
    CHOOSE_LANG,
    SHARE_CONTACT,
    ONLINE,
    ASK_QUESTION,
    USER_MENU,
    SESSION,
    OFFLINE,
    RATING,
    CHANGE_LANG
}

enum class AttachmentContentType {
    DOCUMENT,
    PHOTO,
    STICKER,
    AUDIO,
    VIDEO,
    VOICE,
    VIDEO_NOTE
}

enum class MessageContentType {
    TEXT,
    DOCUMENT,
    PHOTO,
    STICKER,
    AUDIO,
    VIDEO,
    VOICE,
    VIDEO_NOTE,
    DICE
}

enum class ErrorCode(val code: Int) {
    USER_NOT_FOUND(100)
}

enum class LocalizationTextKey {
    CONNECTED_TRUE,
    RATE_THE_OPERATOR,
    CHANGE_LANGUAGE,
    PLEASE_SHARE_CONTACT,
    CLOSE_BT,
    CLOSE_AND_OFF_BT,
    CLICK_THE_START_COMMAND
}

enum class LanguageEnum(val code: String) {
    UZ("uz"), RU("ru"), ENG("en")
}