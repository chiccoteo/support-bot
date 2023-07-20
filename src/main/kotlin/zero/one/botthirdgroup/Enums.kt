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
    VIDEO_NOTE
}
