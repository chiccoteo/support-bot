package zero.one.botthirdgroup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow


@Service
class TelegramBotService : TelegramLongPollingBot() {

    @Value("\${telegram.bot.username}")
    private val username: String = ""

    @Value("\${telegram.bot.token}")
    private val token: String = ""


    override fun getBotUsername(): String = username


    override fun getBotToken(): String = token

    override fun onUpdateReceived(update: Update?) {
        if (update!!.hasMessage()) {
            val message = update.message
            val chatId = message.chatId

            if (message.hasText()) {
                val text = message.text
                when {
                    text.equals("/start") -> {
                        sendNotification(chatId, "Hello ${message.from.firstName}")
                    }

                    text.equals("Uz \uD83C\uDDFA\uD83C\uDDFF") -> sendContactRequest(
                        chatId,
                        LanguageUtil().contactButtonTxt(LanguageName.UZ)
                    )

                    text.equals("Ru \uD83C\uDDF7\uD83C\uDDFA") -> sendContactRequest(
                        chatId,
                        LanguageUtil().contactButtonTxt(LanguageName.RU)
                    )

                    text.equals("Eng \uD83C\uDDFA\uD83C\uDDF8") -> sendContactRequest(
                        chatId,
                        LanguageUtil().contactButtonTxt(LanguageName.ENG)
                    )

                    else -> sendNotification(chatId, "Your text : $text")
                }
            } else {

            }


        }
    }

    private fun sendContactRequest(chatId: Long?, contactButtonTxt: String) {
        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val contactButton = KeyboardButton(contactButtonTxt)
        contactButton.requestContact = true
        row1.add(contactButton)
        rows.add(row1)

        sendMessage.text = contactButtonTxt
        sendMessage.chatId = chatId.toString()


        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        execute(sendMessage)


    }

    private fun sendNotification(chatId: Long?, responseText: String) {

        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val ruButton = KeyboardButton("Ru \uD83C\uDDF7\uD83C\uDDFA")
        val engButton = KeyboardButton("Eng \uD83C\uDDFA\uD83C\uDDF8")
        val uzButton = KeyboardButton("Uz \uD83C\uDDFA\uD83C\uDDFF")
        row1.add(uzButton)
        row1.add(ruButton)
        row1.add(engButton)
        rows.add(row1)

        sendMessage.text = responseText
        sendMessage.chatId = chatId.toString()


        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        execute(sendMessage)

    }

}