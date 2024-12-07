package zero.one.botthirdgroup

import org.telegram.telegrambots.meta.api.objects.Update
import java.lang.RuntimeException
import java.util.*

fun Update.getChatId(): String {
    return when {
        hasMessage() -> message.chatId.toString()
        hasEditedMessage() -> editedMessage.chatId.toString()
        hasCallbackQuery() -> callbackQuery.from.id.toString()
        else -> throw RuntimeException("Chat id not found!")
    }
}

fun LanguageEnum.toLocale() = Locale(this.code)


