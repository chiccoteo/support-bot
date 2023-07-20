package zero.one.botthirdgroup

import org.apache.logging.log4j.message.TimestampMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.objects.Contact
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Timestamp
import java.util.*


@Service
class TelegramBot(
    private val userService: UserService,
    private val languageUtil: LanguageUtil,
    private val languageRepository: LanguageRepository,
    private val messageService: MessageService,
    private val attachmentRepo: AttachmentRepository
) : TelegramLongPollingBot() {

    @Value("\${telegram.bot.username}")
    private val username: String = ""

    @Value("\${telegram.bot.token}")
    private val token: String = ""

    override fun getBotUsername(): String = username

    override fun getBotToken(): String = token

    override fun onUpdateReceived(update: Update) {

        val user = userService.createOrTgUser(update.getChatId())

        if (update.hasMessage()) {
            val message = update.message
            val userLang: LanguageName = user.languages[0].name
            if (message.hasText()) {
                val text = message.text

                println(message)
                println(message.isReply)

                val replyToMessage = message.replyToMessage



                if (text.equals("/start")) {


//                    val execute = execute(SendMessage(user.chatId, "Salom"))
//
//                    val sendMessage = SendMessage(user.chatId, "Alik")
//                    sendMessage.replyToMessageId = execute.messageId
//                    execute(sendMessage)


                    println(message)

//                    val message1 = Message()
//                    message1.replyToMessage = Message()


                    if (user.role == Role.OPERATOR) {
                        if (user.botState != BotState.SESSION)
                            onlineOfflineMenu(user, userLang)
                    } else {
                        when (user.botState) {
                            BotState.START -> {
                                chooseLanguage(user, message.from.firstName)
                            }

                            BotState.SHARE_CONTACT -> {
                                sendContactRequest(user, languageUtil.contactButtonTxt(userLang))
                            }

                            BotState.USER_MENU -> {
                                userMenu(user)
                            }

                            BotState.RATING -> {
                                rateOperator(messageService.isThereNotRatedSession(update.getChatId()))
                                messageService.closingSession(update.getChatId())
                            }

                            else -> {}
                        }
                    }
                }

                if (user.botState == BotState.RATING) {
                    rateOperator(messageService.isThereNotRatedSession(update.getChatId()))
                    messageService.closingSession(update.getChatId())
                }

                if (user.botState == BotState.USER_MENU) {
                    if (text.equals("Savol so'rash❓") || text.equals("Задайте вопрос❓") || text.equals("Ask question❓")) {

                        val message1 = SendMessage()
                        message1.chatId = update.getChatId()

                        when {
                            (userLang == LanguageName.UZ && text.equals("Savol so'rash❓")) -> {
                                message1.text = languageUtil.pleaseGiveQuestion(LanguageName.UZ)
                            }

                            (userLang == LanguageName.RU && text.equals("Задайте вопрос❓")) -> {
                                message1.text = languageUtil.pleaseGiveQuestion(LanguageName.RU)
                            }

                            (userLang == LanguageName.ENG && text.equals("Ask question❓")) -> {
                                message1.text = languageUtil.pleaseGiveQuestion(LanguageName.ENG)

                            }

                            else -> {
                                message1.text = languageUtil.errorLang(userLang)
                            }
                        }

                        val replyKeyboardRemove = ReplyKeyboardRemove(true)
                        message1.replyMarkup = replyKeyboardRemove
                        execute(message1)
                        user.botState = BotState.ASK_QUESTION
                        userService.update(user)
                    } else if (text.equals("Sozlamalar ⚙️") || text.equals("Настройки ⚙️") || text.equals("Settings ⚙️")) {
                        user.botState = BotState.CHANGE_LANG
                        userService.update(user)
                        chooseLanguage(user, message.from.firstName)
                    }
                }

                // Sending messages for User
                else if (user.botState == BotState.ASK_QUESTION) {
                    val create = messageService.create(
                        MessageDTO(
                            message.messageId,
                            getReplyMessageTgId(message),
                            null,
                            Timestamp(System.currentTimeMillis()),
                            user.chatId, null, text, null,
                            MessageContentType.TEXT
                        )
                    )
                    if (create != null) {
                        val tgUser = userService.createOrTgUser(create.toChatId.toString())
                        if (messageService.isThereOneMessageInSession(update.getChatId())) {
                            val connectingMessage: SendMessage
                            getCloseOrCloseAndOff(tgUser).let {
                                it.text = "Siz " + user.name + " bilan bog'landingiz"
                                connectingMessage = it
                            }
                            execute(connectingMessage)
                            sendText(user, "Siz " + tgUser.name + " bilan bog'landingiz")
                        }
                        sendMessage(create, tgUser.chatId)
                        tgUser.botState = BotState.SESSION
                        userService.update(tgUser)
                    }
                }

                if (user.role == Role.OPERATOR && user.botState != BotState.SESSION) {
                    when (text) {
                        "ONLINE" -> {
                            user.botState = BotState.ONLINE
                            userService.update(user)
                            messageService.getWaitedMessages(user.chatId)?.let {
                                executeWaitedMessages(it, user)
                            }
                            if (user.botState != BotState.SESSION)
                                onlineOfflineMenu(user, userLang)
                        }

                        "OFFLINE" -> {
                            user.botState = BotState.OFFLINE
                            userService.update(user)
                            if (user.botState != BotState.SESSION)
                                onlineOfflineMenu(user, userLang)
                        }
                    }
                }

                // Sending messages for Operator
                if (user.botState == BotState.SESSION) {
                    if (text.equals("CLOSE") || text.equals("CLOSE AND OFF")) {
                        if (text.equals("CLOSE")) {
                            val session = messageService.getSessionByOperator(update.getChatId())!!
                            rateOperator(session)
                            messageService.closingSession(update.getChatId())
                            user.botState = BotState.ONLINE
                            userService.update(user)
                            userService.createOrTgUser(session.user.chatId).run {
                                this.botState = BotState.RATING
                                userService.update(this)
                            }
                            messageService.getWaitedMessages(user.chatId)?.let {
                                executeWaitedMessages(it, user)
                            }
                        } else {
                            user.botState = BotState.OFFLINE
                            userService.update(user)
                            val session = messageService.getSessionByOperator(update.getChatId())!!
                            rateOperator(session)
                            messageService.closingSession(update.getChatId())
                            userService.createOrTgUser(session.user.chatId).run {
                                this.botState = BotState.RATING
                                userService.update(this)
                            }
                        }
                        if (user.botState != BotState.SESSION) {
                            onlineOfflineMenu(user, userLang)
                        }
                    } else {
                        // ONLINE text not send
                        val create = messageService.create(
                            MessageDTO(
                                message.messageId,
                                getReplyMessageTgId(message),
                                null,
                                Timestamp(System.currentTimeMillis()),
                                user.chatId,
                                null,
                                text,
                                null,
                                MessageContentType.TEXT
                            )
                        )
                        sendMessage(create!!, create.toChatId!!)
                    }
                }
            } else if (message.hasContact()) {
                if (user.botState == BotState.SHARE_CONTACT) {
                    val contact = message.contact
                    if (message.from.id == contact.userId) {
                        getContact(user, contact)
                    } else {
                        sendText(user, "Iltimos share contact")
                    }

                }
            } else if (message.hasPhoto()) {
                val photo = message.photo.last()
                val create = create(photo.fileId, "asd.png", AttachmentContentType.PHOTO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        message.caption,
                        create,
                        MessageContentType.PHOTO
                    )
                )
                messageDTO?.let {
                    val sendPhoto = SendPhoto(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendPhoto.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendPhoto).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else if (message.hasDocument()) {
                val document = message.document
                val attachment = create(document.fileId, document.fileName, AttachmentContentType.DOCUMENT)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.DOCUMENT
                    )
                )
                messageDTO?.let {
                    val sendDocument = SendDocument(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendDocument.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendDocument).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else if (message.hasSticker()) {
                val sticker = message.sticker

                println(sticker)


                var stickerType = "";

                stickerType = if (sticker.isAnimated) "apng"
                else "webp"


                val attachment = create(sticker.fileId, "sticker.$stickerType", AttachmentContentType.STICKER)

                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        null,
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.STICKER
                    )
                )

                messageDTO?.let {
                    execute(
                        SendSticker(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
                }

            } else if (message.hasVideo()) {
                val video = message.video
                val attachment = create(video.fileId, video.fileName, AttachmentContentType.VIDEO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.VIDEO
                    )
                )
                messageDTO?.let {
                    val sendVideo = SendVideo(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendVideo.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendVideo).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else if (message.hasAudio()) {
                val audio = message.audio
                val attachment = create(audio.fileId, "audio.mp3", AttachmentContentType.AUDIO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.AUDIO
                    )
                )
                messageDTO?.let {
                    val sendAudio = SendAudio(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendAudio.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendAudio).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }

            } else if (message.hasVoice()) {
                val voice = message.voice
                val attachment = create(voice.fileId, "asd.ogg", AttachmentContentType.VOICE)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.VOICE
                    )
                )
                messageDTO?.let {
                    val sendVoice = SendVoice(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendVoice.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendVoice).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else if (message.hasVideoNote()) {
                val videoNote = message.videoNote
                val attachment = create(videoNote.fileId, "video.mp4", AttachmentContentType.VIDEO_NOTE)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.VIDEO_NOTE
                    )
                )
                messageDTO?.let {
                    val sendVideoNote = SendVideoNote(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendVideoNote.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendVideoNote).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            }

        } else if (update.hasCallbackQuery()) {
            val data = update.callbackQuery.data
            if (user.botState == BotState.START || user.botState == BotState.CHANGE_LANG) {
                if (user.botState == BotState.START) {
                    user.botState = BotState.CHOOSE_LANG
                    userService.update(user)
                }
                when (data) {
                    "UZ" -> {
                        user.languages = mutableListOf(languageRepository.findByName(LanguageName.UZ))
                        if (user.botState == BotState.CHANGE_LANG) {
                            user.botState = BotState.USER_MENU
                            userService.update(user)
                            userMenu(user)
                        } else
                            sendContactRequest(
                                user,
                                languageUtil.contactButtonTxt(LanguageName.UZ)
                            )
                    }

                    "RU" -> {
                        user.languages = mutableListOf(languageRepository.findByName(LanguageName.RU))
                        if (user.botState == BotState.CHANGE_LANG) {
                            user.botState = BotState.USER_MENU
                            userService.update(user)
                            userMenu(user)
                        } else
                            sendContactRequest(
                                user,
                                languageUtil.contactButtonTxt(LanguageName.RU)
                            )
                    }

                    "ENG" -> {
                        user.languages = mutableListOf(languageRepository.findByName(LanguageName.ENG))
                        if (user.botState == BotState.CHANGE_LANG) {
                            user.botState = BotState.USER_MENU
                            userService.update(user)
                            userMenu(user)
                        } else
                            sendContactRequest(
                                user,
                                languageUtil.contactButtonTxt(LanguageName.ENG)
                            )
                    }

                }
            }
            if (user.botState == BotState.RATING) {
                messageService.ratingOperator(data.substring(1).toLong(), data.substring(0, 1).toByte())
                user.botState = BotState.USER_MENU
                userService.update(user)
                userMenu(user)
            }
        }
    }

    private fun sendMessage(messageDTO: MessageDTO, userChatId: String) {
        val sendMessage = SendMessage()
        if (messageDTO.replyTelegramMessageId != null) {
            sendMessage.replyToMessageId =
                messageService.getReplyMessageId(messageDTO.replyTelegramMessageId)
        }
        sendMessage.text = messageDTO.text.toString()
        sendMessage.chatId = userChatId
        messageDTO.executeTelegramMessageId = execute(sendMessage).messageId
        messageService.update(messageDTO.telegramMessageId, messageDTO.executeTelegramMessageId!!)
    }

    private fun getReplyToMessageId(messageDTO: MessageDTO): Int? {
        if (messageDTO.replyTelegramMessageId != null) {
            return messageService.getReplyMessageId(messageDTO.replyTelegramMessageId)
        }
        return null
    }

    private fun executeWaitedMessages(waitedMessages: List<MessageDTO>, user: User) {
        waitedMessages.let {
            // session open now
            user.botState = BotState.SESSION
            userService.update(user)
            val sender = userService.createOrTgUser(waitedMessages[0].senderChatId)
            getCloseOrCloseAndOff(user).let { connectingMessage ->
                when {
                    user.languages[0].name == LanguageName.ENG -> {
                        connectingMessage.text = "You have contacted the " + sender.name
                        execute(connectingMessage)
                        sendText(sender, "You have contacted the " + user.name)
                    }

                    user.languages[0].name == LanguageName.RU -> {
                        connectingMessage.text = "вы связались с " + sender.name
                        execute(connectingMessage)
                        sendText(sender, "вы связались с " + user.name)
                    }

                    else -> {
                        connectingMessage.text = "Siz " + sender.name + " bilan bog'landingiz"
                        execute(connectingMessage)
                        sendText(sender, "Siz " + user.name + " bilan bog'landingiz")
                    }
                }
            }
            for (waitedMessage in it) {
                if (waitedMessage.attachment == null) {
                    waitedMessage.run {
                        this.text?.let {
                            sendMessage(this, this.toChatId!!)
                        }
                    }
                } else {
                    val replyToMessageId = getReplyToMessageId(waitedMessage)
                    waitedMessage.attachment.let { attachment ->
                        when (attachment.contentType) {
                            AttachmentContentType.PHOTO -> {
                                val sendPhoto = SendPhoto(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendPhoto.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendPhoto).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.DOCUMENT -> {
                                val sendDocument = SendDocument(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendDocument.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendDocument).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.VIDEO -> {
                                val sendVideo = SendVideo(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendVideo.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendVideo).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.AUDIO -> {
                                val sendAudio = SendAudio(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendAudio.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendAudio).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.VIDEO_NOTE -> {
                                val sendVideoNote = SendVideoNote(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendVideoNote.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendVideoNote).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.VOICE -> {
                                val sendVoice = SendVoice(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendVoice.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendVoice).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.STICKER -> {

                            }
                        }
                    }
                }
            }
        }
    }

    private fun getReplyMessageTgId(message: Message): Int? {
        return if (message.isReply)
            message.replyToMessage.messageId
        else
            null
    }

    private fun getContact(tgUser: User, contact: Contact) {
        val phoneNumber = contact.phoneNumber
//        tgUser.name = contact.firstName + " " + contact.lastName
        contact.run {
            tgUser.name = ""
            firstName?.let { tgUser.name += firstName }
            lastName?.let { tgUser.name += lastName }
        }
        tgUser.phoneNumber = phoneNumber
        tgUser.botState = BotState.USER_MENU
        userService.update(tgUser)
        userMenu(tgUser)
    }

    private fun rateOperator(session: Session) {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rows: MutableList<MutableList<InlineKeyboardButton>> = mutableListOf()
        var row: MutableList<InlineKeyboardButton> = mutableListOf()
        var button = InlineKeyboardButton()

        button.text = "1"
        button.callbackData = "1" + session.id
        row.add(button)
        rows.add(row)

        row = mutableListOf()
        button = InlineKeyboardButton()
        button.text = "2"
        button.callbackData = "2" + session.id
        row.add(button)
        rows.add(row)

        row = mutableListOf()
        button = InlineKeyboardButton()
        button.text = "3"
        button.callbackData = "3" + session.id
        row.add(button)
        rows.add(row)

        row = mutableListOf()
        button = InlineKeyboardButton()
        button.text = "4"
        button.callbackData = "4" + session.id
        row.add(button)
        rows.add(row)

        row = mutableListOf()
        button = InlineKeyboardButton()
        button.text = "5"
        button.callbackData = "5" + session.id
        row.add(button)
        rows.add(row)

        inlineKeyboardMarkup.keyboard = rows

        val sendMessage = SendMessage()
        var text = ""
        if (session.sessionLanguage.name == LanguageName.UZ)
            text = "Operatorni baholang 😀"
        if (session.sessionLanguage.name == LanguageName.ENG)
            text = "Rate the operator 😀"
        if (session.sessionLanguage.name == LanguageName.RU)
            text = "Оцените оператора 😀"
        sendMessage.text = text
        sendMessage.chatId = session.user.chatId
        sendMessage.replyMarkup = inlineKeyboardMarkup
        execute(sendMessage)
    }

    private fun onlineOfflineMenu(user: User, userLang: LanguageName) {
        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val onButton = KeyboardButton("ONLINE")
        val offButton = KeyboardButton("OFFLINE")


        if (user.botState == BotState.OFFLINE)
            row1.add(onButton)
        if (user.botState == BotState.ONLINE)
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

    private fun getCloseOrCloseAndOff(user: User): SendMessage {
        val sendMessage = SendMessage()
        val rows: MutableList<KeyboardRow> = mutableListOf()

        val row1 = KeyboardRow()

        val closeButton = KeyboardButton("CLOSE")
        val closeAndOffButton = KeyboardButton("CLOSE AND OFF")

        row1.add(closeButton)
        row1.add(closeAndOffButton)
        rows.add(row1)

        sendMessage.text = ""
        sendMessage.chatId = user.chatId


        val markup = ReplyKeyboardMarkup()
        markup.resizeKeyboard = true
        markup.keyboard = rows
        sendMessage.replyMarkup = markup
        return sendMessage
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
        user.botState = BotState.SHARE_CONTACT
        userService.update(user)
    }

    private fun chooseLanguage(user: User, name: String) {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val sendMessage = SendMessage()

        val rows: MutableList<MutableList<InlineKeyboardButton>> = mutableListOf()

        var button = InlineKeyboardButton()
        var row: MutableList<InlineKeyboardButton> = mutableListOf()
        button.text = "UZ \uD83C\uDDFA\uD83C\uDDFF"
        button.callbackData = "UZ"

        row.add(button)
        rows.add(row)

        button = InlineKeyboardButton()
        row = mutableListOf()
        button.text = "RU \uD83C\uDDF7\uD83C\uDDFA"
        button.callbackData = "RU"
        row.add(button)
        rows.add(row)

        button = InlineKeyboardButton()
        row = mutableListOf()
        button.text = "ENG \uD83C\uDDFA\uD83C\uDDF8"
        button.callbackData = "ENG"
        row.add(button)
        rows.add(row)

        inlineKeyboardMarkup.keyboard = rows
        var text = "\uD83C\uDDFA\uD83C\uDDFFAssalomu alaykum $name!\n" +
                "Botga xush kelibsiz. Iltimos tilni tanlang"
        if (user.botState == BotState.CHANGE_LANG) {
            text = when (user.languages[0].name) {
                LanguageName.UZ -> {
                    "Til tanlang"
                }

                LanguageName.RU -> {
                    "Выберите язык"
                }

                LanguageName.ENG -> {
                    "Choose a language"
                }
            }
        }
        sendMessage.text = text
        sendMessage.chatId = user.chatId
        sendMessage.replyMarkup = inlineKeyboardMarkup
        execute(sendMessage)
    }

    fun sendText(user: User, text: String): Message {
        val sendMessage = SendMessage()
        sendMessage.text = text
        sendMessage.chatId = user.chatId
        return execute(sendMessage)
    }

    fun userMenu(user: User) {
        val userLang = user.languages[0].name
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
        val fromTelegram = getFromTelegram(fileId, botToken) //execute( GetFile(fileId))
        val path = Paths.get(
            "files/" +
                    UUID.randomUUID().toString() + "." + strings[strings.size - 1]
        )
        Files.copy(ByteArrayInputStream(fromTelegram), path)
        return attachmentRepo.save(Attachment(path.toString(), contentType))
    }

    fun getFromTelegram(fileId: String, token: String) = execute(GetFile(fileId)).run {
        RestTemplate().getForObject<ByteArray>("https://api.telegram.org/file/bot${token}/${filePath}")
    }


    fun sendNotificationToOperator(chatId: String) {



    }
}