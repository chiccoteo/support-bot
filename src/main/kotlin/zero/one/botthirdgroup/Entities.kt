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
    var phoneNumber: String?,
    var chatId: String,
    @Enumerated(EnumType.STRING) var role: Role?,
    @ManyToMany var languages: MutableList<Language>?,
    @Enumerated(EnumType.STRING) var botState: BotState?


) : BaseEntity()

@Entity
class Language(
    @Enumerated(EnumType.STRING) var name: LanguageName
) : BaseEntity()

@Entity
class Chat(
    @ManyToOne var user: User,
    @ManyToOne var operator: User
) : BaseEntity()

@Entity
class Session(
    @ColumnDefault(value = "false") var status: Boolean,
    @ManyToOne var chat: Chat
) : BaseEntity()

@Entity
class Attachment(
    var originalName: String,
    var contentType: String,
    var size: Long,
) : BaseEntity()

@Entity
class AttachmentContent(
    var byte: ByteArray,
    @OneToOne var attachment: Attachment
) : BaseEntity()

@Entity
class Message(
    var telegramMessageId: Long,
    var time: Timestamp,
    @ManyToOne var session: Session,
    @ManyToOne var user: User,
    var text: String?,
    @OneToOne var attachment: Attachment?
) : BaseEntity()