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
    @ManyToMany(fetch = FetchType.EAGER) var languages: MutableList<Language?>,
    @Enumerated(EnumType.STRING) var botState: BotState = BotState.START

) : BaseEntity() {
    constructor(chatId: String) : this(null, null, chatId, null, mutableListOf())
}

@Entity
class Language(
    @Enumerated(EnumType.STRING) var name: LanguageName
) : BaseEntity()

@Entity
class Session(
    @ColumnDefault(value = "true") var status: Boolean,
    @ManyToOne var user: User,
    @ManyToOne var operator: User
) : BaseEntity()

@Entity
class Attachment(
    var originalName: String,
    var contentType: String,
    var size: Long,
) : BaseEntity()

@Entity
class Message(
    var telegramMessageId: Long,
    var replyTelegramMessageId: Long,
    var time: Timestamp,
    @ManyToOne var session: Session,
    @ManyToOne var sender: User,
    var text: String?
) : BaseEntity()