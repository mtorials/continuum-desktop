package koma.gui.dialog.room

import com.github.kittinunf.result.Result
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import javafx.util.Callback
import koma.gui.dialog.room.alias.requestAddRoomAlias
import koma.gui.dialog.room.alias.requestSetRoomCanonicalAlias
import koma.gui.element.emoji.keyboard.NoSelectionModel
import koma.matrix.room.naming.RoomAlias
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*
import java.util.concurrent.Callable

class RoomAliasForm(room: Room): Fragment() {
    override val root: Fieldset

    init {
        this.title = "Update Aliases of Room ${room.displayName.value}"

        root = fieldset("Room Aliases") {
            vbox(5) {
                listview(room.aliases) {
                    prefHeight = 200.0
                    selectionModel = NoSelectionModel()
                    cellFactory = object : Callback<ListView<RoomAlias>, ListCell<RoomAlias>> {
                        override fun call(param: ListView<RoomAlias>?): ListCell<RoomAlias> {
                            return RoomAliasCell(room)
                        }
                    }
                    vgrow = Priority.ALWAYS
                }
                hbox(5.0) {
                    val field = TextField()
                    field.promptText = "additional-alias"
                    val servername = appState.serverConf.servername
                    hbox {
                        alignment = Pos.CENTER
                        label("#")
                        add(field)
                        label(":")
                        label(servername)
                    }
                    val getAlias = { "#${field.text}:$servername" }
                    button("Add") { action { requestAddRoomAlias(room, getAlias()) } }
                }
            }
        }
    }
}

private fun deleteRoomAlias(room: Room, alias: RoomAlias?) {
    alias?:return
    val api = appState.apiClient
    api ?: return
    launch {
        val result = api.deleteRoomAlias(alias.str).awaitMatrix()
        if (result is Result.Failure) {
            val message = result.error.message
            launch(JavaFx) {
                Notifications.create()
                        .title("Failed to delete room alias $alias")
                        .text("In room ${room.displayName.get()}\n$message")
                        .owner(FX.primaryStage)
                        .showWarning()
            }
        } else {
            launch(JavaFx) {
                room.aliases.remove(alias)
            }
        }
    }
}

class RoomAliasCell(private val room: Room): ListCell<RoomAlias>() {

    private val roomAlias = SimpleObjectProperty<RoomAlias>()
    private val cell: HBox

    init {
        val text = stringBinding(roomAlias) { value?.str }
        val isCanon = Bindings.createBooleanBinding(Callable{roomAlias.value == room.canonicalAlias.value},
                roomAlias, room.canonicalAlias)
        val star = MaterialIconFactory.get().createIcon(MaterialIcon.STAR)
        val notstar = MaterialIconFactory.get().createIcon(MaterialIcon.STAR_BORDER)
        val deleteIcon = MaterialIconFactory.get().createIcon(MaterialIcon.DELETE)
        cell = hbox(5) {
            prefWidth = 1.0
            minWidth = 1.0
            alignment = Pos.CENTER_LEFT
            stackpane {
                hyperlink(graphic = notstar) {
                    tooltip("Set as Canonical Alias")
                    visibleWhen { this@hbox.hoverProperty() }
                    action { requestSetRoomCanonicalAlias(room, roomAlias.value) }
                }
                hyperlink(graphic = star) {
                    tooltip("Current Canonical Alias")
                    removeWhen { isCanon.not() }
                }
            }
            label {
                textProperty().bind(text)
                style {
                    fontWeight = FontWeight.EXTRA_BOLD
                }
            }
            lazyContextmenu {
                item("Delete").action { deleteRoomAlias(room, roomAlias.value) }
                item("Set Canonical").action { requestSetRoomCanonicalAlias(room, roomAlias.value) }
            }
            hyperlink(graphic = deleteIcon) {
                visibleWhen { this@hbox.hoverProperty() }
                action { deleteRoomAlias(room, roomAlias.value) }
            }
        }
    }

    override fun updateItem(item: RoomAlias?, empty: Boolean) {
        super.updateItem(item, empty)
        roomAlias.set(item)
        if (item != null) graphic = cell else graphic = null
    }
}
