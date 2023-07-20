package zero.one.botthirdgroup

import org.hibernate.annotations.ColumnDefault
import java.sql.Timestamp
import javax.persistence.*

@MappedSuperclass
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)

@Entity(name = "users")
class User(
    var name: String?,
    var username: String?,
    var phoneNumber: String?,
    var chatId: String,
    @Enumerated(EnumType.STRING) var role: Role = Role.USER,
    @ManyToMany(fetch = FetchType.EAGER) var languages: MutableList<Language>,
    @Enumerated(EnumType.STRING) var botState: BotState = BotState.START
) : BaseEntity() {
    constructor(chatId: String, languages: MutableList<Language>) : this(
        null, null, null, chatId, Role.USER, languages
    )

}

@Entity
class Language(
    @Enumerated(EnumType.STRING) var name: LanguageName
) : BaseEntity()

@Entity
class Session(
    @ColumnDefault(value = "true") var status: Boolean,
    @ManyToOne var sessionLanguage: Language,
    var time: Timestamp,
    var rate: Double = 0.0,
    @ManyToOne var user: User,
    @ManyToOne var operator: User?
) : BaseEntity()

@Entity
class Attachment(
    var pathName: String,
    @Enumerated(EnumType.STRING) var contentType: AttachmentContentType,
) : BaseEntity()

@Entity
class Message(
    var telegramMessageId: Int,
    var replyTelegramMessageId: Int?,
    var executeMessageId: Int,
    var time: Timestamp,
    @ManyToOne var session: Session,
    @ManyToOne var sender: User,
    @OneToOne var attachment: Attachment?,
    @Enumerated(EnumType.STRING) var messageType: MessageContentType,
    var text: String?
) : BaseEntity()