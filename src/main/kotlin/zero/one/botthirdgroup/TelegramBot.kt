package zero.one.botthirdgroup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Contact
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAnimation
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException
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
    private val userRepository: UserRepository,
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

    companion object {
        var map: MutableMap<String, Int> = mutableMapOf()
    }

    override fun onUpdateReceived(update: Update) {

        if (update.hasMyChatMember()) {
            val chatMember = update.myChatMember
            if (chatMember.newChatMember.status == "kicked")
                userRepository.findByChatIdAndDeletedFalse(chatMember.from.id.toString())?.let {
                    when (it.botState) {
                        BotState.SESSION -> it.botState = BotState.BLOCK_AFTER_SESSION
                        BotState.ONLINE -> it.botState = BotState.BLOCK_AFTER_ONLINE
                        BotState.ASK_QUESTION -> it.botState = BotState.BLOCK_AFTER_ASK_QUESTION
                        BotState.OFFLINE -> it.botState = BotState.BLOCK_AFTER_OFFLINE
                        else -> {}
                    }
                    userRepository.save(it)
                }
            if (chatMember.newChatMember.status == "member")
                userRepository.findByChatIdAndDeletedFalse(chatMember.from.id.toString())?.let { user ->
                    when (user.botState) {
                        BotState.BLOCK_AFTER_ONLINE -> {
                            user.botState = BotState.ONLINE
                            userRepository.save(user)
                            messageService.getWaitedMessages(chatMember.from.id.toString())?.let {
                                executeWaitedMessages(it, user)
                            }
                        }

                        BotState.BLOCK_AFTER_OFFLINE -> {
                            user.botState = BotState.OFFLINE
                            userRepository.save(user)
                        }

                        BotState.BLOCK_AFTER_SESSION -> {
                            executeWaitedMessages(messageService.getWaitedMessagesBlockTime(user), user)
                            user.botState = BotState.SESSION
                            userRepository.save(user)
                        }

                        BotState.BLOCK_AFTER_ASK_QUESTION -> {
                            executeWaitedMessages(messageService.getWaitedMessagesBlockTime(user), user)
                            user.botState = BotState.ASK_QUESTION
                            userRepository.save(user)
                        }

                        else -> {}
                    }
                }
        } else {

            val user = userService.createOrTgUser(update.getChatId())

            if (update.hasEditedMessage()) {
                val editMessage = update.editedMessage
                if (editMessage.hasText()) {
                    val editedMessage = messageService.editMessage(editMessage)
                    val editMessageText = EditMessageText()
                    if (editedMessage?.session?.user != editedMessage?.sender)
                        editMessageText.chatId = editedMessage?.session?.user?.chatId
                    else
                        editMessageText.chatId = editedMessage?.session?.operator?.chatId
                    editMessageText.messageId = editedMessage?.executeTelegramMessageId
                    editMessageText.text = editedMessage?.text!!
                    try {
                        execute(editMessageText)
                    } catch (ex: TelegramApiValidationException) {
                        //
                    }
                } else {
                    sendSavedAndUpdateAttachments(editMessage, user)
                }
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
                            else {
                                val closeOrCloseAndOff = getCloseOrCloseAndOff(user)
                                closeOrCloseAndOff.text = "."
                                execute(closeOrCloseAndOff)
                            }
                        } else {
                            when (user.botState) {
                                BotState.START -> {
                                    user.botState = BotState.CHOOSE_LANG
                                    userService.update(user)
                                    chooseLanguage(user, message.from.firstName)
                                }

                                BotState.CHOOSE_LANG -> {
                                    execute(DeleteMessage(update.getChatId(), map[update.getChatId()]!!))
                                chooseLanguage(user, message.from.firstName)
                                }

                                BotState.CHANGE_LANG -> {
                                    execute(DeleteMessage(update.getChatId(), map[update.getChatId()]!!))
                                chooseLanguage(user, message.from.firstName)
                                }

                                BotState.SHARE_CONTACT -> {
                                    sendContactRequest(user, languageUtil.contactButtonTxt(userLang))
                                }

                                BotState.USER_MENU -> {
                                    userMenu(user)
                                }

                                BotState.RATING -> {
                                    execute(DeleteMessage(user.chatId, map[user.chatId]!!))
                                    rateOperator(messageService.getSessionByUser(update.getChatId()))
                                }

                                else -> {}
                            }
                        }
                    } else if (user.botState == BotState.CHOOSE_LANG) {
                        execute(DeleteMessage(update.getChatId(), map[update.getChatId()]!!))
                    chooseLanguage(user, message.from.firstName)
                    } else if (user.botState == BotState.CHANGE_LANG) {
                        execute(DeleteMessage(update.getChatId(), map[update.getChatId()]!!))
                    chooseLanguage(user, message.from.firstName)
                    } else if (user.botState == BotState.RATING) {
                        execute(DeleteMessage(user.chatId, map[user.chatId]!!))
                        rateOperator(messageService.getSessionByUser(update.getChatId()))
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
                            initializeConnect(user, create)
                            sendMessage(create, tgUser.chatId)
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
                            if (create != null)
                                sendMessage(create, create.toChatId!!)
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
                } else {
                    if (user.botState == BotState.SESSION || user.botState == BotState.ASK_QUESTION)
                        sendSavedAndUpdateAttachments(message, user)
                }

            } else if (update.hasCallbackQuery()) {
                val data = update.callbackQuery.data
                val deletingMessageId = update.callbackQuery.message.messageId
                if (user.botState == BotState.CHOOSE_LANG || user.botState == BotState.CHANGE_LANG) {
                    execute(DeleteMessage(update.getChatId(), deletingMessageId))
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
    }

    private fun sendSavedAndUpdateAttachments(message: Message, user: User) {
        if (message.hasPhoto()) {
            val savedMessage = messageService.getMessageById(message.messageId)
            val photo = message.photo.last()
            if (savedMessage == null) {
                val create = create(photo.fileId, photo.fileUniqueId, "asd.png", AttachmentContentType.PHOTO)
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
                    initializeConnect(user, messageDTO)
                    val sendPhoto = SendPhoto(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendPhoto.caption = it.text
                    sendPhoto.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                    it.executeTelegramMessageId = execute(sendPhoto).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else {
                val attachment = create(
                    photo.fileId,
                    photo.fileUniqueId,
                    savedMessage.attachment?.pathName!!,
                    AttachmentContentType.PHOTO
                )
                editMessageByType(message.caption, savedMessage, attachment!!)
            }
        } else if (message.hasDocument()) {
            val savedMessage = messageService.getMessageById(message.messageId)
            val document = message.document
            if (savedMessage == null) {
                val attachment =
                    create(document.fileId, document.fileUniqueId, document.fileName, AttachmentContentType.DOCUMENT)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        message.caption,
                        attachment,
                        MessageContentType.DOCUMENT
                    )
                )
                val pathName = attachment?.pathName!!
                messageDTO?.let {
                    initializeConnect(user, messageDTO)
                    val sendDocument = SendDocument()
                    sendDocument.chatId = it.toChatId!!
                    if (pathName.endsWith("MOV"))
                        sendDocument.document = InputFile(attachment.fileId)
                    else
                        sendDocument.document = InputFile(File(pathName))
                    sendDocument.caption = it.text
                    sendDocument.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                    it.executeTelegramMessageId = execute(sendDocument).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else {
                val attachment = create(
                    document.fileId,
                    document.fileUniqueId,
                    savedMessage.attachment?.pathName!!,
                    AttachmentContentType.DOCUMENT
                )
                editMessageByType(message.caption, savedMessage, attachment!!)
            }
        } else if (message.hasSticker()) {
            val sticker = message.sticker
            val stickerType: String = if (sticker.isAnimated) "tgs"
            else if (sticker.isVideo) "webm"
            else "webs"
            val attachment =
                create(sticker.fileId, sticker.fileUniqueId, "sticker.$stickerType", AttachmentContentType.STICKER)
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
            if (messageDTO != null) {
                initializeConnect(user, messageDTO)
                if (sticker.isVideo) {
                    val sendSticker = SendSticker().apply {
                        chatId = messageDTO.toChatId.toString()
                    }
                    sendSticker.sticker = InputFile(sticker.fileId)
                    sendSticker.replyToMessageId =
                        getReplyToMessageId(messageDTO.replyTelegramMessageId, messageDTO.senderChatId)
                    messageDTO.executeTelegramMessageId = execute(sendSticker).messageId
                    messageService.update(messageDTO.telegramMessageId, messageDTO.executeTelegramMessageId!!)
                } else {
                    val sendSticker = SendSticker(
                        messageDTO.toChatId.toString(),
                        InputFile(messageDTO.attachment?.pathName?.let { File(it) })
                    )
                    sendSticker.replyToMessageId =
                        getReplyToMessageId(messageDTO.replyTelegramMessageId, messageDTO.senderChatId)
                    messageDTO.executeTelegramMessageId = execute(sendSticker).messageId
                    messageService.update(messageDTO.telegramMessageId, messageDTO.executeTelegramMessageId!!)
                }
            }
        } else if (message.hasDice()) {
            val dice = message.dice
            val messageDTO = messageService.create(
                MessageDTO(
                    message.messageId,
                    getReplyMessageTgId(message),
                    null,
                    Timestamp(System.currentTimeMillis()),
                    user.chatId,
                    null,
                    dice.emoji,
                    null,
                    MessageContentType.DICE
                )
            )
            messageDTO?.let {
                initializeConnect(user, messageDTO)
                val sendDice = SendDice().apply {
                    chatId = it.toChatId.toString()
                    emoji = it.text
                }
                sendDice.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                it.executeTelegramMessageId = execute(sendDice).messageId
                messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
            }
        } else if (message.hasVideo()) {
            val savedMessage = messageService.getMessageById(message.messageId)
            val video = message.video
            if (savedMessage == null) {
                val attachment = create(video.fileId, video.fileUniqueId, video.fileName, AttachmentContentType.VIDEO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        message.caption,
                        attachment,
                        MessageContentType.VIDEO
                    )
                )
                messageDTO?.let {
                    initializeConnect(user, messageDTO)
                    val sendVideo = SendVideo(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendVideo.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                    it.executeTelegramMessageId = execute(sendVideo).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else {
                val attachment = create(
                    video.fileId,
                    video.fileUniqueId,
                    savedMessage.attachment?.pathName!!,
                    AttachmentContentType.VIDEO
                )
                editMessageByType(message.caption, savedMessage, attachment!!)
            }
        } else if (message.hasAudio()) {
            val savedMessage = messageService.getMessageById(message.messageId)
            val audio = message.audio
            if (savedMessage == null) {
                val attachment = create(audio.fileId, audio.fileUniqueId, "audio.mp3", AttachmentContentType.AUDIO)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        message.caption,
                        attachment,
                        MessageContentType.AUDIO
                    )
                )
                messageDTO?.let {
                    initializeConnect(user, messageDTO)
                    val sendAudio = SendAudio(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendAudio.caption = it.text
                    sendAudio.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                    it.executeTelegramMessageId = execute(sendAudio).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else {
                val attachment = create(
                    audio.fileId,
                    audio.fileUniqueId,
                    savedMessage.attachment?.pathName!!,
                    AttachmentContentType.AUDIO
                )
                editMessageByType(message.caption, savedMessage, attachment!!)
            }
        } else if (message.hasVoice()) {
            val savedMessage = messageService.getMessageById(message.messageId)
            val voice = message.voice
            if (savedMessage == null) {
                val attachment = create(voice.fileId, voice.fileUniqueId, "asd.ogg", AttachmentContentType.VOICE)
                val messageDTO = messageService.create(
                    MessageDTO(
                        message.messageId,
                        getReplyMessageTgId(message),
                        null,
                        Timestamp(System.currentTimeMillis()),
                        user.chatId,
                        null,
                        message.caption,
                        attachment,
                        MessageContentType.VOICE
                    )
                )
                messageDTO?.let {
                    initializeConnect(user, messageDTO)
                    val sendVoice = SendVoice(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendVoice.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                    it.executeTelegramMessageId = execute(sendVoice).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else {
                val toChatId: String = if (savedMessage.session.user != savedMessage.sender)
                    savedMessage.session.user.chatId
                else
                    savedMessage.session.operator?.chatId!!
                try {
                    val sendEditedCaption = EditMessageCaption()
                    sendEditedCaption.caption = message.caption
                    sendEditedCaption.chatId = toChatId
                    sendEditedCaption.messageId = savedMessage.executeTelegramMessageId
                    execute(sendEditedCaption)
                } catch (ex: TelegramApiRequestException) {
                    //
                }
                savedMessage.text = message.caption
                messageService.updateMessage(savedMessage)
            }
        } else if (message.hasVideoNote()) {
            val savedMessage = messageService.getMessageById(message.messageId)
            val videoNote = message.videoNote
            if (savedMessage == null) {
                val attachment =
                    create(videoNote.fileId, videoNote.fileUniqueId, "video.mp4", AttachmentContentType.VIDEO_NOTE)
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
                    initializeConnect(user, messageDTO)
                    val sendVideoNote = SendVideoNote(
                        it.toChatId.toString(),
                        InputFile(it.attachment?.pathName?.let { it1 -> File(it1) })
                    )
                    sendVideoNote.replyToMessageId = getReplyToMessageId(it.replyTelegramMessageId, it.senderChatId)
                    it.executeTelegramMessageId = execute(sendVideoNote).messageId
                    messageService.update(it.telegramMessageId, it.executeTelegramMessageId!!)
                }
            } else {
                val toChatId: String = if (savedMessage.session.user != savedMessage.sender)
                    savedMessage.session.user.chatId
                else
                    savedMessage.session.operator?.chatId!!
                try {
                    val sendEditedCaption = EditMessageCaption()
                    sendEditedCaption.caption = message.caption
                    sendEditedCaption.chatId = toChatId
                    sendEditedCaption.messageId = savedMessage.executeTelegramMessageId
                    execute(sendEditedCaption)
                } catch (ex: TelegramApiRequestException) {
                    //
                }
                savedMessage.text = message.caption
                messageService.updateMessage(savedMessage)
            }
        }
    }

    private fun initializeConnect(user: User, createdMessage: MessageDTO?) {
        val tgUser = userService.createOrTgUser(createdMessage?.toChatId.toString())
        if (messageService.isThereOneMessageInSession(user.chatId)) {
            val connectingMessage: SendMessage
            val langMessage: String =
                messageSourceService.getMessage(
                    LocalizationTextKey.CONNECTED_TRUE,
                    user.languages[0].name
                )
            getCloseOrCloseAndOff(tgUser).let {
                it.text = user.name + " " + langMessage
                connectingMessage = it
            }
            execute(connectingMessage)
            sendText(user, "Operator $langMessage")
        }
        if (tgUser.role == Role.OPERATOR) {
            tgUser.botState = BotState.SESSION
            userService.update(tgUser)
        }
    }

    private fun editMessageByType(
        caption: String?,
        savedMessage: zero.one.botthirdgroup.Message,
        attachment: Attachment
    ) {
        val toChatId: String = if (savedMessage.session.user != savedMessage.sender)
            savedMessage.session.user.chatId
        else
            savedMessage.session.operator?.chatId!!

        if (savedMessage.attachment?.fileUniqueId != attachment.fileUniqueId) {
            val sendPhoto = EditMessageMedia()
            sendPhoto.media = when (attachment.contentType) {
                AttachmentContentType.PHOTO -> {
                    InputMediaPhoto().apply {
                        media = attachment.fileId
                    }
                }

                AttachmentContentType.DOCUMENT -> {
                    InputMediaDocument().apply {
                        media = attachment.fileId
                    }
                }

                AttachmentContentType.VIDEO -> {
                    InputMediaVideo().apply {
                        media = attachment.fileId
                    }
                }

                AttachmentContentType.AUDIO -> {
                    InputMediaAudio().apply {
                        media = attachment.fileId
                    }
                }

                else -> {
                    InputMediaAnimation()
                }
            }
            sendPhoto.messageId = savedMessage.executeTelegramMessageId
            sendPhoto.chatId = toChatId
            execute(sendPhoto)
        }
        try {
            val sendEditedCaption = EditMessageCaption()
            sendEditedCaption.caption = caption
            sendEditedCaption.chatId = toChatId
            sendEditedCaption.messageId = savedMessage.executeTelegramMessageId
            execute(sendEditedCaption)
        } catch (ex: TelegramApiRequestException) {
            //
        }
        savedMessage.text = caption
        savedMessage.attachment = attachment
        messageService.updateMessage(savedMessage)
    }

    private fun sendMessage(messageDTO: MessageDTO, userChatId: String) {
        val sendMessage = SendMessage()
        sendMessage.text = messageDTO.text.toString()
        sendMessage.chatId = userChatId
        if (messageDTO.replyTelegramMessageId != null) {
            val replyMessageId =
                messageService.getReplyMessageId(messageDTO.senderChatId, messageDTO.replyTelegramMessageId)
            // replyMessageId will be null if this message does not exist in database
            if (replyMessageId != null)
                sendMessage.replyToMessageId = replyMessageId
            try {
                messageDTO.executeTelegramMessageId = execute(sendMessage).messageId
                messageService.update(messageDTO.telegramMessageId, messageDTO.executeTelegramMessageId)
            } catch (e: TelegramApiRequestException) {
                sendMessage.replyToMessageId = null
                execute(sendMessage)
            }
        } else {
            messageDTO.executeTelegramMessageId = execute(sendMessage).messageId
            messageService.update(messageDTO.telegramMessageId, messageDTO.executeTelegramMessageId)
        }
    }

    private fun getReplyToMessageId(replyTelegramMessageId: Int?, senderChatId: String): Int? {
        if (replyTelegramMessageId != null) {
            return messageService.getReplyMessageId(senderChatId, replyTelegramMessageId)
        }
        return null
    }

    private fun executeWaitedMessages(waitedMessages: List<MessageDTO>, user: User) {
        waitedMessages.let {
            // session open now
            if (user.botState != BotState.BLOCK_AFTER_SESSION && user.botState != BotState.BLOCK_AFTER_ASK_QUESTION) {
                val sender = userService.createOrTgUser(waitedMessages[0].senderChatId)
                getCloseOrCloseAndOff(user).let { connectingMessage ->
                    val message =
                        messageSourceService.getMessage(LocalizationTextKey.CONNECTED_TRUE, user.languages[0].name)
                    connectingMessage.text = sender.name + " " + message
                    execute(connectingMessage)
                    sendText(sender, "Operator $message")
                }
            }
            if (user.role == Role.OPERATOR) {
                user.botState = BotState.SESSION
                userService.update(user)
            } else {
                user.botState = BotState.ASK_QUESTION
                userService.update(user)
            }
            for (waitedMessage in it) {
                if (waitedMessage.attachment == null) {
                    waitedMessage.run {
                        if (this.messageType == MessageContentType.DICE) {
                            val sendDice = SendDice()
                            sendDice.chatId = toChatId!!
                            sendDice.emoji = text
                            executeTelegramMessageId = execute(sendDice).messageId
                            messageService.update(telegramMessageId, executeTelegramMessageId)
                        } else
                            this.text?.let {
                                sendMessage(this, this.toChatId!!)
                            }
                    }
                } else {
                    val replyToMessageId =
                        getReplyToMessageId(waitedMessage.replyTelegramMessageId, waitedMessage.senderChatId)
                    waitedMessage.attachment.let { attachment ->
                        when (attachment.contentType) {
                            AttachmentContentType.PHOTO -> {
                                val sendPhoto = SendPhoto(
                                    waitedMessage.toChatId.toString(),
                                    InputFile(File(attachment.pathName))
                                )
                                sendPhoto.caption = waitedMessage.text
                                sendPhoto.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendPhoto).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.DOCUMENT -> {
                                val sendDocument = SendDocument()
                                sendDocument.chatId = waitedMessage.toChatId!!
                                if (attachment.pathName.endsWith("MOV"))
                                    sendDocument.document = InputFile(attachment.fileId)
                                else
                                    sendDocument.document = InputFile(File(attachment.pathName))
                                sendDocument.caption = waitedMessage.text
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
                                sendVideo.caption = waitedMessage.text
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
                                sendAudio.caption = waitedMessage.text
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
                                sendVoice.caption = waitedMessage.text
                                sendVoice.replyToMessageId = replyToMessageId
                                waitedMessage.executeTelegramMessageId = execute(sendVoice).messageId
                                messageService.update(
                                    waitedMessage.telegramMessageId,
                                    waitedMessage.executeTelegramMessageId!!
                                )
                            }

                            AttachmentContentType.STICKER -> {
                                val sendSticker: SendSticker = if (waitedMessage.attachment.pathName.endsWith("webm")) {
                                    SendSticker(
                                        waitedMessage.toChatId.toString(),
                                        InputFile(waitedMessage.attachment.fileId)
                                    )
                                } else {
                                    SendSticker(
                                        waitedMessage.toChatId.toString(),
                                        InputFile(File(attachment.pathName))
                                    )
                                }
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
        map[session.user.chatId] = execute.messageId
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
        map[user.chatId] = execute.messageId
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

    fun create(
        fileId: String,
        fileUniqueId: String,
        fileName: String?,
        contentType: AttachmentContentType
    ): Attachment? {
        if (fileName == null || fileName.endsWith("MOV")) {
            val fromTelegram = getFromTelegram(fileId, botToken) //execute( GetFile(fileId))
            val path = Paths.get(
                "files/" +
                        UUID.randomUUID().toString() + "." + "mp4"
            )
            Files.copy(ByteArrayInputStream(fromTelegram), path)
            return attachmentRepo.save(Attachment(path.toString(), fileId, fileUniqueId, contentType))
        } else {
            val attachment = attachmentRepo.findByPathNameAndDeletedFalse(fileName)
            if (attachment == null) {
                val strings = fileName.split(".")
                val fromTelegram = getFromTelegram(fileId, botToken) //execute( GetFile(fileId))
                val path = Paths.get(
                    "files/" +
                            UUID.randomUUID().toString() + "." + strings[strings.size - 1]
                )
                Files.copy(ByteArrayInputStream(fromTelegram), path)
                return attachmentRepo.save(Attachment(path.toString(), fileId, fileUniqueId, contentType))
            } else {
                val file = File(fileName)
                if (file.exists()) {
                    try {
                        file.delete()
                        val fromTelegram = getFromTelegram(fileId, botToken)
                        Files.copy(ByteArrayInputStream(fromTelegram), Paths.get(fileName))
                    } catch (e: SecurityException) {
                        //
                    }
                }
            }
            attachment.fileId = fileId
            attachment.fileUniqueId = fileUniqueId
            return attachmentRepo.save(attachment)
        }
    }

    fun getFromTelegram(fileId: String, token: String) = execute(GetFile(fileId)).run {
        RestTemplate().getForObject<ByteArray>("https://api.telegram.org/file/bot${token}/${filePath}")
    }

    fun updateLang(dto: LanguageUpdateDTO) {
        val user = userRepository.findByPhoneNumberAndDeletedFalse(dto.phoneNumber)
            ?: throw UserNotFoundException(dto.phoneNumber)
        user.languages = languageRepository.findAllById(dto.languages)
        userRepository.save(user)
        if (user.botState == BotState.ONLINE)
            messageService.getWaitedMessages(user.chatId)?.let {
                executeWaitedMessages(it, user)
            }
    }

}
