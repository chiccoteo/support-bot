package zero.one.botthirdgroup

import org.springframework.stereotype.Component

@Component

class LanguageUtil {


    fun contactButtonTxt(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Kontaktingizni yuboring \uD83D\uDCDE"
            LanguageEnum.RU -> "Отправьте свой контакт \uD83D\uDCDE"
            LanguageEnum.ENG -> "Send your contact \uD83D\uDCDE"
        }
    }

    fun langBtnTxt(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Kontaktingizni yuboring \uD83D\uDCDE"
            LanguageEnum.RU -> "Отправьте свой контакт \uD83D\uDCDE"
            LanguageEnum.ENG -> "Send your contact \uD83D\uDCDE"
        }
    }

    fun askQuestionTxt(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Savol so'rash❓"
            LanguageEnum.RU -> "Задайте вопрос❓"
            LanguageEnum.ENG -> "Ask question❓"
        }
    }


    fun chooseMenu(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Kerakli bo'imni tanlang \uD83D\uDC47"
            LanguageEnum.RU -> "Выберите нужный раздел \uD83D\uDC47"
            LanguageEnum.ENG -> "Select the desired section \uD83D\uDC47"
        }
    }

    fun settings(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Sozlamalar ⚙️"
            LanguageEnum.RU -> "Настройки ⚙️"
            LanguageEnum.ENG -> "Settings ⚙️"
        }
    }

    fun chooseMenuTextReq(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Iltimos kerakli bo'limni tanlang"
            LanguageEnum.RU -> "Пожалуйста, выберите нужный раздел"
            LanguageEnum.ENG -> "Please select the desired section️"
        }
    }

    fun pleaseGiveQuestion(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Qanday savolingiz bor?"
            LanguageEnum.RU -> "Какой у вас вопрос?"
            LanguageEnum.ENG -> "What question do you have?"
        }
    }
    fun closeSessionBtnTxt(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Sessiyani yopish"
            LanguageEnum.RU -> "Закрыть сеанс"
            LanguageEnum.ENG -> "Close the session"
        }
    }

    fun closeAndCloseOff(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Sessiya ochildi"
            LanguageEnum.RU -> "Сессия открыта"
            LanguageEnum.ENG -> "Session opened"
        }
    }

    fun errorLang(lang: LanguageEnum): String {
        return when (lang) {
            LanguageEnum.UZ -> "Iltimos o'zbek tilida xabar yozing"
            LanguageEnum.RU -> "Пожалуйста, напишите сообщение на русском языке"
            LanguageEnum.ENG -> "Please write in English"
        }
    }
}









