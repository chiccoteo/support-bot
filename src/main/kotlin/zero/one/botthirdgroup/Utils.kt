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

    fun askQuestionTxt(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Savol so'rash❓"
            LanguageName.RU -> "Задайте вопрос❓"
            LanguageName.ENG -> "Ask question❓"
        }
    }


    fun chooseMenu(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Kerakli bo'imni tanlang \uD83D\uDC47"
            LanguageName.RU -> "Выберите нужный раздел \uD83D\uDC47"
            LanguageName.ENG -> "Select the desired section \uD83D\uDC47"
        }
    }

    fun settings(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Sozlamalar ⚙️"
            LanguageName.RU -> "Настройки ⚙️"
            LanguageName.ENG -> "Settings ⚙️"
        }
    }

    fun chooseMenuTextReq(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Iltimos kerakli bo'limni tanlang"
            LanguageName.RU -> "Пожалуйста, выберите нужный раздел"
            LanguageName.ENG -> "Please select the desired section️"
        }
    }

    fun pleaseGiveQuestion(lang: LanguageName): String {
        return when (lang) {
            LanguageName.UZ -> "Qanday savolingiz bor?"
            LanguageName.RU -> "Какой у вас вопрос?"
            LanguageName.ENG -> "What question do you have?"
        }
    }
}





