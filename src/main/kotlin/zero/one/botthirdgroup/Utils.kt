package zero.one.botthirdgroup


class LanguageUtil {

    fun contactButtonTxt(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Kontaktingizni yuboring \uD83D\uDCDE"
            LanguageName.RU -> "Отправьте свой контакт \uD83D\uDCDE"
            LanguageName.ENG -> "Send your contact \uD83D\uDCDE"
        }
    }


}





