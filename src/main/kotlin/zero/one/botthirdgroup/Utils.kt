package zero.one.botthirdgroup

import org.springframework.stereotype.Component

@Component

class LanguageUtil {


    fun contactButtonTxt(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Kontaktingizni yuboring \uD83D\uDCDE"
            LanguageName.RU -> "Отправьте свой контакт \uD83D\uDCDE"
            LanguageName.ENG -> "Send your contact \uD83D\uDCDE"
        }
    }

    fun langBtnTxt(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Kontaktingizni yuboring \uD83D\uDCDE"
            LanguageName.RU -> "Отправьте свой контакт \uD83D\uDCDE"
            LanguageName.ENG -> "Send your contact \uD83D\uDCDE"
        }
    }


}





