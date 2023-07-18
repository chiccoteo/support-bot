package zero.one.botthirdgroup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.*


@Service
class TelegramBotService(
    private val userService: UserService,
    private val languageUtil: LanguageUtil,
    private val languageRepository: LanguageRepository
) : TelegramLongPollingBot() {

    @Value("\${telegram.bot.username}")
    private val username: String = ""

    @Value("\${telegram.bot.token}")
    private val token: String = ""


    override fun getBotUsername(): String = username


    override fun getBotToken(): String = token

    fun getFromTelegram(fileId: String, token: String) = execute(GetFile(fileId)).run {
        RestTemplate().getForObject<ByteArray>("https://api.telegram.org/file/bot${token}/${filePath}")
    }

    override fun onUpdateReceived(update: Update?) {
        if (update!!.hasMessage()) {
            val message = update.message
            val chatId = message.chatId.toString()

            val user = userService.createOrTgUser(chatId)
            val userLang: LanguageName? = if (user.languages.isEmpty())
                null
            else
                user.languages[0]?.name
            if (message.hasText()) {
                val text = message.text
                when {
                    text.equals("/start") -> {
                        if (userLang == null) {
                            chooseLanguage(user, message.from.firstName)
                        } else if (user.phoneNumber == null) {
                            sendContactRequest(user, languageUtil.contactButtonTxt(userLang))
                        }

                    }


                    user.botState == BotState.CHOOSE_LANG -> {
                        when (text) {
                            "Uz \uD83C\uDDFA\uD83C\uDDFF" -> {
                                user.languages = mutableListOf(languageRepository.findByName(LanguageName.UZ))
                                sendContactRequest(
                                    user,
                                    languageUtil.contactButtonTxt(LanguageName.UZ)
                                )
                            }

                            "Ru \uD83C\uDDF7\uD83C\uDDFA" -> {
                                user.languages = mutableListOf(languageRepository.findByName(LanguageName.RU))
                                sendContactRequest(
                                    user,
                                    languageUtil.contactButtonTxt(LanguageName.RU)
                                )
                            }

                            "Eng \uD83C\uDDFA\uD83C\uDDF8" -> {
                                user.languages = mutableListOf(languageRepository.findByName(LanguageName.ENG))
                                sendContactRequest(
                                    user,
                                    languageUtil.contactButtonTxt(LanguageName.ENG)
                                )
                            }
                        }
                        user.botState = BotState.SHARE_CONTACT
                        userService.update(user)
                    }


                    else -> sendNotification(chatId, "Your text : $text")
                }
            } else if (message.hasContact()) {
                val contact = message.contact
                user.phoneNumber = contact.phoneNumber
                user.name = contact.firstName + " " + contact?.lastName
                userService.update(user)
            }
        }
    }

    private fun sendContactRequest(user: User, contactButtonTxt: String) {
        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val contactButton = KeyboardButton(contactButtonTxt)
        contactButton.requestContact = true
        row1.add(contactButton)
        rows.add(row1)

        sendMessage.text = contactButtonTxt
        user.chatId.also { sendMessage.chatId = it }


        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        execute(sendMessage)


    }

    private fun chooseLanguage(user: User, name: String) {
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

        sendMessage.text = "Assalamu alaykum $name!\n" +
                "Botga xush kelibsiz. Iltimos tilni tanlang\n" +
                "Здрасвуйте $name!\n" +
                "Добро пожаловать в бота. Пожалуйста, выберите язык\n" +
                "Hello $name!\n" +
                "Welcome to the bot. Please select a language"
        sendMessage.chatId = user.chatId


        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        execute(sendMessage)

        user.botState = BotState.CHOOSE_LANG
        userService.update(user)
    }

    private fun sendNotification(chatId: String?, responseText: String) {

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