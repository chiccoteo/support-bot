package zero.one.botthirdgroup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
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

        if (update.hasEditedMessage()) {
            val editMessage = update.editedMessage
            val editedMessage = messageService.editMessage(editMessage)
            val editMessageText = EditMessageText()
            if (editedMessage?.session?.user != editedMessage?.sender)
                editMessageText.chatId = editedMessage?.session?.user?.chatId
            else
                editMessageText.chatId = editedMessage?.session?.operator?.chatId
            editMessageText.messageId = editedMessage?.executeTelegramMessageId
            editMessageText.text = editedMessage?.text!!
            execute(editMessageText)
        }

        if (update.hasMessage()) {
            val message = update.message
            val userLang: LanguageEnum = user.languages[0].name
            if (message.hasText()) {
                val text = message.text
                if (text.equals("/start")) {
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
                } else if (user.botState == BotState.RATING) {
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
                            val message: String =
                                messageSourceService.getMessage(
                                    LocalizationTextKey.CONNECTED_TRUE,
                                    user.languages[0].name
                                )
                            getCloseOrCloseAndOff(tgUser).let {
                                it.text = user.name + " "+message
                                connectingMessage = it
                            }
                            execute(connectingMessage)
                            sendText(user, "Operator $message")
                            /*                            getCloseOrCloseAndOff(tgUser).let {
                                                            it.text = "Siz " + user.name + " bilan bog'landingiz"
                                                            connectingMessage = it
                                                        }
                                                        execute(connectingMessage)
                                                       sendText(user, "Siz " + tgUser.name + " bilan bog'landingiz")
                                      */
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
                else if (user.botState == BotState.SESSION) {
                    val sessionByOperator = messageService.getSessionByOperator(user.chatId)
                    val close = messageSourceService.getMessage(
                        LocalizationTextKey.CLOSE_BT,
                        sessionByOperator?.sessionLanguage?.name!!
                    )
                    val closeAndOff =
                        messageSourceService.getMessage(
                            LocalizationTextKey.CLOSE_AND_OFF_BT,
                            sessionByOperator.sessionLanguage.name
                        )
                    if (text.equals(close) || text.equals(closeAndOff)) {
                        if (text.equals(close)) {
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
                    sendPhoto.caption = it.text
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
                var stickerType = "";
                stickerType = if (sticker.isAnimated) "tgs"
                else "webp"
                val attachment = create(sticker.fileId, "sticker.$stickerType", AttachmentContentType.STICKER)
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
                        MessageContentType.STICKER
                    )
                )
                messageDTO?.let {
                    val sendSticker = SendSticker(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendSticker.replyToMessageId = getReplyToMessageId(it)
                    it.executeTelegramMessageId = execute(sendSticker).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
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
            val deletingMessageId = update.callbackQuery.message.messageId
            if (user.botState == BotState.START || user.botState == BotState.CHANGE_LANG) {
                execute(DeleteMessage(update.getChatId(), deletingMessageId))
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
                execute(DeleteMessage(update.getChatId(), deletingMessageId))
                user.botState = BotState.USER_MENU
                userService.update(user)
                userMenu(user)
            }
        }
    }

    private fun sendMessage(messageDTO: MessageDTO, userChatId: String) {
        val sendMessage = SendMessage()
        if (messageDTO.replyTelegramMessageId != null) {
            val replyMessageId = messageService.getReplyMessageId(messageDTO.senderChatId, messageDTO.replyTelegramMessageId)
            // replyMessageId will be null if this message does not exist in database
            if (replyMessageId != null)
                sendMessage.replyToMessageId = replyMessageId
        }
        sendMessage.text = messageDTO.text.toString()
        sendMessage.chatId = userChatId
        messageDTO.executeTelegramMessageId = execute(sendMessage).messageId
        messageService.update(messageDTO.telegramMessageId, messageDTO.executeTelegramMessageId!!)
    }

    private fun getReplyToMessageId(messageDTO: MessageDTO): Int? {
        if (messageDTO.replyTelegramMessageId != null) {
            return messageService.getReplyMessageId(messageDTO.senderChatId, messageDTO.replyTelegramMessageId)
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
                val message =
                    messageSourceService.getMessage(LocalizationTextKey.CONNECTED_TRUE, user.languages[0].name)
                connectingMessage.text = sender.name + " " + message
                execute(connectingMessage)
                sendText(sender, user.name + " " + message)
                sendText(sender, "Operator $message")
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
                                val sendSticker = SendSticker(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendSticker.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendSticker).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
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
        val row: MutableList<InlineKeyboardButton> = mutableListOf()
        var button = InlineKeyboardButton()

        button.text = "1"
        button.callbackData = "1" + session.id
        row.add(button)



        button = InlineKeyboardButton()
        button.text = "2"
        button.callbackData = "2" + session.id
        row.add(button)



        button = InlineKeyboardButton()
        button.text = "3"
        button.callbackData = "3" + session.id
        row.add(button)



        button = InlineKeyboardButton()
        button.text = "4"
        button.callbackData = "4" + session.id
        row.add(button)


        button = InlineKeyboardButton()
        button.text = "5"
        button.callbackData = "5" + session.id
        row.add(button)
        rows.add(row)

        inlineKeyboardMarkup.keyboard = rows

        val sendMessage = SendMessage()
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

        val sessionByOperator = messageService.getSessionByOperator(user.chatId)
        val bt1 =
            messageSourceService.getMessage(LocalizationTextKey.CLOSE_BT, sessionByOperator?.sessionLanguage?.name!!)
        val bt2 = messageSourceService.getMessage(
            LocalizationTextKey.CLOSE_AND_OFF_BT,
            sessionByOperator.sessionLanguage.name
        )
        val closeButton = KeyboardButton(bt1)
        val closeAndOffButton = KeyboardButton(bt2)

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
        val row: MutableList<InlineKeyboardButton> = mutableListOf()
        button.text = "UZ \uD83C\uDDFA\uD83C\uDDFF"
        button.callbackData = "UZ"

        row.add(button)


        button = InlineKeyboardButton()
        button.text = "RU \uD83C\uDDF7\uD83C\uDDFA"
        button.callbackData = "RU"
        row.add(button)


        button = InlineKeyboardButton()
        button.text = "ENG \uD83C\uDDFA\uD83C\uDDF8"
        button.callbackData = "ENG"
        row.add(button)
        rows.add(row)

        inlineKeyboardMarkup.keyboard = rows
        var text = "\uD83C\uDDFA\uD83C\uDDFFAssalomu alaykum $name!\n" +
                "Botga xush kelibsiz. Iltimos tilni tanlang"
        if (user.botState == BotState.CHANGE_LANG) {
            text = messageSourceService.getMessage(LocalizationTextKey.CHANGE_LANGUAGE, user.languages[0].name)
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

}
