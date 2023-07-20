package zero.one.botthirdgroup

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
}

interface UserRepository : BaseRepository<User> {
    fun findByPhoneNumberAndDeletedFalse(phoneNumber: String): User?
    fun findByRoleAndDeletedFalse(role: Role, pageable: Pageable): Page<User>
    fun findAllByDeletedFalse(pageable: Pageable): Page<User>
    fun findByChatIdAndDeletedFalse(chatId: String): User?
    fun findAllByBotStateAndLanguagesContainsAndDeletedFalse(botState: BotState, language: Language): List<User?>
}

interface LanguageRepository : BaseRepository<Language> {
    fun findByName(name: LanguageName): Language
}

interface SessionRepository : BaseRepository<Session> {
    @Query("select operator_id,avg(rate) from Session group by operator_id", nativeQuery = true)
    fun getOperatorAvgRate():List<GetOperatorAvgRateDTO>
    fun findByStatusTrueAndOperator(operator: User): Session?
    fun findByStatusTrueAndUser(user: User): Session?
    fun findAllByStatusTrueAndSessionLanguageInAndOperatorIsNullOrderByTime(operatorLanguages: MutableList<Language>?): List<Session?>
}

interface MessageRepository : BaseRepository<Message> {
    fun findAllBySessionAndDeletedFalseOrderByTime(session: Session): List<Message>
}

interface AttachmentRepository : BaseRepository<Attachment>
