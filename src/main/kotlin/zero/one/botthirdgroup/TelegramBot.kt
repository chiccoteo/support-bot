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
    private val attachmentRepo: AttachmentRepository,
    private val messageSourceService: MessageSourceService
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
            val userLang: LanguageEnum = user.languages[0].name
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
                            (userLang == LanguageEnum.UZ && text.equals("Savol so'rash❓")) -> {
                                message1.text = languageUtil.pleaseGiveQuestion(LanguageEnum.UZ)
                            }

                            (userLang == LanguageEnum.RU && text.equals("Задайте вопрос❓")) -> {
                                message1.text = languageUtil.pleaseGiveQuestion(LanguageEnum.RU)
                            }

                            (userLang == LanguageEnum.ENG && text.equals("Ask question❓")) -> {
                                message1.text = languageUtil.pleaseGiveQuestion(LanguageEnum.ENG)

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
                        val sendMessage = SendMessage()
                        sendMessage.text = create.text.toString()
                        sendMessage.chatId = tgUser.chatId
                        execute(sendMessage)
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
                            /*waitedMessages?.let {
                                // session open now
                                user.botState = BotState.SESSION
                                userService.update(user)
                                val sender = userService.createOrTgUser(waitedMessages[0].senderChatId)
                                getCloseOrCloseAndOff(user).let { connectingMessage ->
                                    if (user.languages[0].name == LanguageEnum.ENG) {
                                        connectingMessage.text = "You have contacted the " + sender.name
                                        execute(connectingMessage)
                                        sendText(sender, "You have contacted the " + user.name)
                                    } else if (user.languages[0].name == LanguageEnum.UZ) {
                                        connectingMessage.text = "Siz " + sender.name + " bilan bog'landingiz"
                                        execute(connectingMessage)
                                        sendText(sender, "Siz " + user.name + " bilan bog'landingiz")
                                    } else {
                                        connectingMessage.text = "вы связались с " + sender.name
                                        execute(connectingMessage)
                                        sendText(sender, "вы связались с " + user.name)
                                    }
                                }
                                for (waitedMessage in it) {


                                    if (waitedMessage.attachment == null) {
                                        waitedMessage.run {
                                            this.text?.let { text ->
                                                sendText(
                                                    userService.createOrTgUser(this.toChatId.toString()),
                                                    text
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
                                                    execute(
                                                        SendVideo(
                                                            waitedMessage.toChatId.toString(),
                                                            InputFile(File(attachment.pathName))
                                                        )
                                                    )
                                                }

                                                AttachmentContentType.AUDIO -> {
                                                    execute(
                                                        SendAudio(
                                                            waitedMessage.toChatId.toString(),
                                                            InputFile(File(attachment.pathName))
                                                        )
                                                    )
                                                }

                                                AttachmentContentType.VIDEO_NOTE -> {
                                                    execute(
                                                        SendVideoNote(
                                                            waitedMessage.toChatId.toString(),
                                                            InputFile(File(attachment.pathName))
                                                        )
                                                    )
                                                }

                                                AttachmentContentType.VOICE -> {
                                                    execute(
                                                        SendVoice(
                                                            waitedMessage.toChatId.toString(),
                                                            InputFile(File(attachment.pathName))
                                                        )
                                                    )
                                                }

                                                AttachmentContentType.STICKER -> {
                                                    execute(
                                                        SendSticker(
                                                            waitedMessage.toChatId.toString(),
                                                            InputFile(File(attachment.pathName))
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }*/
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
                                Timestamp(System.currentTimeMillis()),
                                user.chatId,
                                null,
                                text,
                                null,
                                MessageContentType.TEXT
                            )
                        )

                        create?.let {
                            val tgUser = userService.createOrTgUser(it.toChatId.toString())
                            sendText(tgUser, it.text.toString())
                        }
                    }
                }
            } else if (message.hasContact()) {
                if (user.botState == BotState.SHARE_CONTACT) {
                    val contact = message.contact
                    println("" + contact.userId + "," + message.from.id)
                    if (message.from.id == contact.userId) {
                        getContact(user, contact)
                    } else {
                        sendText(
                            user,
                            messageSourceService.getMessage(
                                LocalizationTextKey.PLEASE_SHARE_CONTACT,
                                user.languages[0].name
                            )
                        )
                    }

                }
            } else if (message.hasPhoto()) {
                val photo = message.photo.first()
                val create = create(photo.fileId, "asd.png", AttachmentContentType.PHOTO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        message.caption,
                        create,
                        MessageContentType.PHOTO
                    )
                )
                messageDTO?.let {
                    execute(
                        SendPhoto(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
                }
            } else if (message.hasDocument()) {
                val document = message.document
                val attachment = create(document.fileId, document.fileName, AttachmentContentType.DOCUMENT)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.DOCUMENT
                    )
                )
                messageDTO?.let {
                    execute(
                        SendDocument(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
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
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.VIDEO
                    )
                )
                messageDTO?.let {
                    execute(
                        SendVideo(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
                }
            } else if (message.hasAudio()) {
                val audio = message.audio
                val attachment = create(audio.fileId, "audio.mp3", AttachmentContentType.AUDIO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.AUDIO
                    )
                )
                messageDTO?.let {
                    execute(
                        SendAudio(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
                }

            } else if (message.hasVoice()) {
                val voice = message.voice
                val attachment = create(voice.fileId, "asd.ogg", AttachmentContentType.VOICE)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.VOICE
                    )
                )
                messageDTO?.let {
                    execute(
                        SendVoice(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
                }
            } else if (message.hasVideoNote()) {
                val videoNote = message.videoNote
                val attachment = create(videoNote.fileId, "video.mp4", AttachmentContentType.VIDEO_NOTE)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        null,
                        attachment,
                        MessageContentType.VIDEO_NOTE
                    )
                )
                messageDTO?.let {
                    execute(
                        SendVideoNote(
                            it.toChatId.toString(),
                            InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                        )
                    )
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
                        user.languages = mutableListOf(languageRepository.findByName(LanguageEnum.UZ))
                        if (user.botState == BotState.CHANGE_LANG) {
                            user.botState = BotState.USER_MENU
                            userService.update(user)
                            userMenu(user)
                        } else
                            sendContactRequest(
                                user,
                                languageUtil.contactButtonTxt(LanguageEnum.UZ)
                            )
                    }

                    "RU" -> {
                        user.languages = mutableListOf(languageRepository.findByName(LanguageEnum.RU))
                        if (user.botState == BotState.CHANGE_LANG) {
                            user.botState = BotState.USER_MENU
                            userService.update(user)
                            userMenu(user)
                        } else
                            sendContactRequest(
                                user,
                                languageUtil.contactButtonTxt(LanguageEnum.RU)
                            )
                    }

                    "ENG" -> {
                        user.languages = mutableListOf(languageRepository.findByName(LanguageEnum.ENG))
                        if (user.botState == BotState.CHANGE_LANG) {
                            user.botState = BotState.USER_MENU
                            userService.update(user)
                            userMenu(user)
                        } else
                            sendContactRequest(
                                user,
                                languageUtil.contactButtonTxt(LanguageEnum.ENG)
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

    private fun executeWaitedMessages(waitedMessages: List<MessageDTO>, user: User) {
        waitedMessages.let {
            // session open now
            user.botState = BotState.SESSION
            userService.update(user)
            val sender = userService.createOrTgUser(waitedMessages[0].senderChatId)
            getCloseOrCloseAndOff(user).let { connectingMessage ->

                /*user.telegramId, messageSourceService.getMessage(
                LocalizationTextKey.START_MESSAGING_MESSAGE,languageService.getLanguageOfUser(message.from.id)
                )*/
                val message =
                    messageSourceService.getMessage(LocalizationTextKey.CONNECTED_TRUE, user.languages[0].name)
                connectingMessage.text = sender.name + " " + message
                execute(connectingMessage)
                sendText(sender, user.name + " " + message)

                /*when {
                    user.languages[0].name == LanguageEnum.ENG -> {
                        connectingMessage.text = "You have contacted the " + sender.name
                        execute(connectingMessage)
                        sendText(sender, "You have contacted the " + user.name)
                    }

                    user.languages[0].name == LanguageEnum.RU -> {
                        connectingMessage.text = "вы связались с " + sender.name
                        execute(connectingMessage)
                        sendText(sender, "вы связались с " + user.name)
                    }

                    else -> {
                        connectingMessage.text = "Siz " + sender.name + " bilan bog'landingiz"
                        execute(connectingMessage)
                        sendText(sender, "Siz " + user.name + " bilan bog'landingiz")
                    }
                }*/
            }
            for (waitedMessage in it) {
                if (waitedMessage.attachment == null) {
                    waitedMessage.run {
                        this.text?.let { text ->
                            sendText(
                                userService.createOrTgUser(this.toChatId.toString()),
                                text
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
                                execute(
                                    SendVideo(
                                        waitedMessage.toChatId.toString(),
                                        InputFile(File(attachment.pathName))
                                    )
                                )
                            }

                            AttachmentContentType.AUDIO -> {
                                execute(
                                    SendAudio(
                                        waitedMessage.toChatId.toString(),
                                        InputFile(File(attachment.pathName))
                                    )
                                )
                            }

                            AttachmentContentType.VIDEO_NOTE -> {
                                execute(
                                    SendVideoNote(
                                        waitedMessage.toChatId.toString(),
                                        InputFile(File(attachment.pathName))
                                    )
                                )
                            }

                            AttachmentContentType.VOICE -> {
                                execute(
                                    SendVoice(
                                        waitedMessage.toChatId.toString(),
                                        InputFile(File(attachment.pathName))
                                    )
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
//        messageSourceService.getMessage(LocalizationTextKey.RATE_THE_OPERATOR, session.sessionLanguage.name)
//        var text = ""
//        if (session.sessionLanguage.name == LanguageEnum.UZ)
//            text = "Operatorni baholang 😀"
//        if (session.sessionLanguage.name == LanguageEnum.ENG)
//            text = "Rate the operator 😀"
//        if (session.sessionLanguage.name == LanguageEnum.RU)
//            text = "Оцените оператора 😀"
        sendMessage.text =
            messageSourceService.getMessage(LocalizationTextKey.RATE_THE_OPERATOR, session.sessionLanguage.name)
        sendMessage.chatId = session.user.chatId
        sendMessage.replyMarkup = inlineKeyboardMarkup
        execute(sendMessage)
    }

    private fun onlineOfflineMenu(user: User, userLang: LanguageEnum) {
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
            text = messageSourceService.getMessage(LocalizationTextKey.CHANGE_LANGUAGE, user.languages[0].name)
//            text = when (user.languages[0].name) {
//                LanguageEnum.UZ -> {
//                    "Til tanlang"
//                }
//
//                LanguageEnum.RU -> {
//                    "Выберите язык"
//                }
//
//                LanguageEnum.ENG -> {
//                    "Choose a language"
//                }
//            }
        }
        sendMessage.text = text
        sendMessage.chatId = user.chatId
        sendMessage.replyMarkup = inlineKeyboardMarkup
        execute(sendMessage)
    }

    fun sendText(user: User, text: String) {
        val sendMessage = SendMessage()
        sendMessage.text = text
        sendMessage.chatId = user.chatId
        execute(sendMessage)
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