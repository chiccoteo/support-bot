package zero.one.botthirdgroup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*


@Service
class TelegramBotService(
    private val userService: UserService,
    private val languageUtil: LanguageUtil,
    private val languageRepository: LanguageRepository,
    private val messageService: MessageService,
    private val attachmentRepo: AttachmentRepository,
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
            val userLang: LanguageName = user.languages[0].name

            if (message.hasText()) {
                val text = message.text
                when {
                    text.equals("/start") -> {
                        if (user.botState == BotState.START) {
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

                    user.botState == BotState.USER_MENU -> {
                        if (text.equals("Savol so'rash❓") || text.equals("Задайте вопрос❓") || text.equals("Ask question❓")) {

                            val message1 = SendMessage()
                            message1.chatId = chatId
                            message1.text = languageUtil.pleaseGiveQuestion(userLang)

                            // Create a ReplyKeyboardRemove instance and set it as the reply markup
                            val replyKeyboardRemove = ReplyKeyboardRemove(true)

                            message1.replyMarkup = replyKeyboardRemove
                            execute(message1)

                            user.botState = BotState.ASK_QUESTION
                            userService.update(user)
                        } else if (text.equals("Sozlamalar ⚙️") || text.equals("Настройки ⚙️") || text.equals("Settings ⚙️")) {

                        } else {

                        }

                    }


                    (user.botState == BotState.ASK_QUESTION || user.botState == BotState.ONLINE) -> {
                        val create = messageService.create(
                            MessageDTO(
                                message.messageId,
                                null,
                                Timestamp(System.currentTimeMillis()),
                                user.chatId, null, text, null
                            )
                        )

                        if (create != null)
                            sendText(userService.createOrTgUser(create.toChatId.toString()), create?.text.toString())


                        val operatorUser = userService.createOrTgUser(create?.toChatId.toString())
                        operatorUser.botState = BotState.SESSION
                        userService.update(operatorUser)
                    }


                    (user.role == Role.OPERATOR && user.botState == BotState.OPERATOR_MENU) -> {
                        when (text) {
                            "ONLINE" -> {
                                user.botState = BotState.ONLINE
                                val waitedMessages = messageService.getWaitedMessages(user.chatId)

                                waitedMessages?.let {

                                    for (waitedMessage in it) {
                                        if (waitedMessage.attachment == null) {
                                            waitedMessage.run {
                                                this.text?.let {
                                                    sendText(
                                                        userService.createOrTgUser(it), this.text.toString()
                                                    )
                                                }
                                            }
                                        } else {

                                            waitedMessage.attachment.let { attachment ->
                                                when (attachment.contentType) {

                                                    AttachmentContentType.PHOTO -> {
                                                        execute(
                                                            SendPhoto(
                                                                waitedMessage.toChatId.toString(),
                                                                InputFile(File(attachment.pathName))
                                                            )
                                                        )
                                                    }

                                                    AttachmentContentType.DOCUMENT -> {
                                                        execute(
                                                            SendDocument(
                                                                waitedMessage.toChatId.toString(),
                                                                InputFile(File(attachment.pathName))
                                                            )
                                                        )
                                                    }

                                                    AttachmentContentType.VIDEO -> {

                                                    }

                                                    else -> {}
                                                }
                                            }


                                        }

                                    }
                                }


                            }

                            "OFFLINE" -> {
                                user.botState = BotState.OFFLINE
                                userService.update(user)
                            }
                        }
                    }

//                    else -> sendText(user, languageUtil.chooseMenuTextReq(userLang))
                }
            } else if (message.hasContact()) {
                val contact = message.contact
                user.phoneNumber = contact.phoneNumber
                user.name = contact.firstName + " " + contact?.lastName
                user.botState = BotState.USER_MENU
                userService.update(user)
                userMenu(user, userLang)
            } else if (message.hasPhoto()) {

                val photo = message.photo.last()
                val create = create(photo.fileId, photo.filePath, AttachmentContentType.PHOTO)

                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        create
                    )
                )

                messageDTO.let {
                    execute(SendPhoto(it?.toChatId.toString(), InputFile(it?.attachment?.pathName?.let { it1 -> File(it1) })))
                }

            } else if (message.hasDocument()) {



            } else if (message.hasSticker()) {

            } else if (message.hasVoice()) {

            } else if (message.hasVideoNote()) {

            }
        }
    }


    private fun onlineOfflineMenu(user: User, userLang: LanguageName) {
        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val onButton = KeyboardButton("ONLINE")
        val offButton = KeyboardButton("OFFLINE")


        row1.add(onButton)
        row1.add(offButton)
        rows.add(row1)

        sendMessage.text = languageUtil.chooseMenuTextReq(userLang)
        sendMessage.chatId = user.chatId


        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        execute(sendMessage)
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

    private fun deleteMessage(user: User) {
        val sendMessage = SendMessage()
        sendMessage.chatId = user.chatId
        sendMessage.text = "."
        sendMessage.setReplyMarkup { ReplyKeyboardRemove(true) }
        val execute = execute(sendMessage)
        execute(DeleteMessage(user.chatId, execute.messageId))
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

        sendMessage.text = "\uD83C\uDDFA\uD83C\uDDFFAssalamu alaykum $name!\n" +
                "Botga xush kelibsiz. Iltimos tilni tanlang\n" +
                "\uD83C\uDDF7\uD83C\uDDFAЗдрасвуйте $name!\n" +
                "Добро пожаловать в бота. Пожалуйста, выберите язык\n" +
                "\uD83C\uDDFA\uD83C\uDDF8Hello $name!\n" +
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

    fun sendText(user: User, text: String) {
        val sendMessage = SendMessage()
        sendMessage.text = text
        sendMessage.chatId = user.chatId
        execute(sendMessage)
    }


    fun userMenu(user: User, userLang: LanguageName) {
        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val askQuestionBtn = KeyboardButton(languageUtil.askQuestionTxt(userLang))
        val settingsBtn = KeyboardButton(languageUtil.settings(userLang))
        row1.add(askQuestionBtn)
        row1.add(settingsBtn)
        rows.add(row1)

        sendMessage.text = languageUtil.chooseMenu(userLang)
        sendMessage.chatId = user.chatId

        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        execute(sendMessage)
    }

    fun create(fileId: String, fileName: String, contentType: AttachmentContentType): Attachment {
        val strings = fileName.split(".")
        val fromTelegram = getFromTelegram(fileId, botToken)
        val path = Paths.get(
            "files/" +
                    UUID.randomUUID().toString() + "." + strings[strings.size - 1]
        )
        Files.copy(ByteArrayInputStream(fromTelegram), path)
        return attachmentRepo.save(Attachment(path.toString(), contentType))
    }


}