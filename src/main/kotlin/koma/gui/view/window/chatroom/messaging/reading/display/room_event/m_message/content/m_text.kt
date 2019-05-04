package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.addStringWithElements
import koma.matrix.UserId
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.NoticeMessage
import koma.matrix.event.room_message.chat.TextMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.gui.list.user.UserDataStore
import tornadofx.*

class MTextViewNode(): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    fun update(content: TextMessage) {
        node.clear()
        node.addStringWithElements(content.body)
    }
}

class MNoticeViewNode(): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    fun update(content: NoticeMessage) {
        node.clear()
        node.addStringWithElements(content.body)
    }
}


@ExperimentalCoroutinesApi
class MEmoteViewNode(private val userData: UserDataStore): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    private val userLabel = Label()

    private var nameUpdateChannel: ReceiveChannel<String>? = null
    private var nameUpdateJob: Job? = null

    fun update(content: EmoteMessage, sender: UserId) {
        userLabel.text = ""
        node.clear()
        userLabel.textFill = userData.getUserColor(sender)

        nameUpdateChannel?.cancel()
        val u = userData.getNameUpdates(sender)
        nameUpdateChannel = u
        nameUpdateJob?.cancel()
        nameUpdateJob = GlobalScope.launch { updateName(u) }

        node.add(userLabel)
        node.add(Text(" "))
        node.addStringWithElements(content.body)
    }

    private suspend fun updateName(updates: ReceiveChannel<String>) = coroutineScope {
        for (name in updates) {
            withContext(UiDispatcher) {
                userLabel.text = name
            }
        }
    }
}

