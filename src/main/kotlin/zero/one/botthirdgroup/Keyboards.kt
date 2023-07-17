package zero.one.botthirdgroup

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

class InlineKeyboardMarkupBuilder(
    private val inlineKeyboardMarkup: InlineKeyboardMarkup = InlineKeyboardMarkup(),
    private var rows: MutableList<List<InlineKeyboardButton>> = mutableListOf()
) {

    companion object {
        fun emptyInlineMarkup() = InlineKeyboardMarkupBuilder().build()

        fun button(text: String, callbackData: String) = InlineKeyboardButton().apply {
            this.text = text
            this.callbackData = callbackData
        }

        fun paymentButton(text: String, callbackData: String) = InlineKeyboardButton().apply {
            this.text = text
            this.callbackData = callbackData
            this.pay = true
        }

        fun buttonUrl(text: String, url: String) = InlineKeyboardButton().apply {
            this.text = text
            this.url = url
        }

        fun buttons(map: Map<String, String>): List<InlineKeyboardButton> {//map key is query value is text
            val list = arrayListOf<InlineKeyboardButton>()
            map.map { (key: String, value: String) ->
                {
                    list.add(InlineKeyboardButton().apply {
                        this.text = value
                        this.callbackData = key
                    })
                }
            }
            return list
        }

    }

    fun row(vararg buttons: InlineKeyboardButton): InlineKeyboardMarkupBuilder {
        rows.addAll(listOf(buttons.asList()))
        return this
    }

    fun addRowButton(text: String, callbackData: String): InlineKeyboardMarkupBuilder {
        rows.add(listOf(button(text, callbackData)))
        return this
    }

    fun rowSize(): Int {
        return rows.size
    }

    fun rows(buttons: List<InlineKeyboardButton>): InlineKeyboardMarkupBuilder {
        rows.addAll(buttons.map { listOf(it) })
        return this
    }

    fun build(): InlineKeyboardMarkup {
        inlineKeyboardMarkup.keyboard = rows
        return inlineKeyboardMarkup
    }
}
