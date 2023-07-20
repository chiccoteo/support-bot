package zero.one.botthirdgroup

import org.telegram.telegrambots.meta.api.objects.Update
import java.lang.RuntimeException

fun Update.getChatId(): String {
    return when {
        hasMessage() -> message.chatId.toString()
        hasCallbackQuery() -> callbackQuery.from.id.toString()
        else -> throw RuntimeException("Chat id not found!")
    }
}
