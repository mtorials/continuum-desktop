package koma.controller.requests

import com.github.kittinunf.result.Result
import javafx.scene.control.Alert
import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.koma_app.appState.apiClient
import koma.matrix.UploadResponse
import koma.matrix.event.room_message.chat.FileInfo
import koma.matrix.event.room_message.chat.FileMessage
import koma.matrix.event.room_message.chat.ImageMessage
import koma.matrix.event.room_message.chat.textToMessage
import koma.matrix.room.naming.RoomId
import koma.util.file.guessMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import org.controlsfx.control.Notifications
import tornadofx.*
import kotlinx.coroutines.javafx.JavaFx as UI

fun sendMessage(room: RoomId, message: String) {
    val msg = textToMessage(message)
    GlobalScope.launch(Dispatchers.UI) {
        val result = apiClient!!.sendMessage(room, msg)

        if (result is Result.Failure) {
            val content = result.error.message
            alert(Alert.AlertType.ERROR, "failed to send message", content)
        }
    }
}

fun sendFileMessage(room: RoomId) {
    val dialog = FileChooser()
    dialog.title = "Find a file to send"

    val file = dialog.showOpenDialog(FX.primaryStage)
    file?:return

    val type = file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    GlobalScope.launch {
        val uploadResult = uploadFile(api, file, type)
        if (uploadResult is Result.Success) {
            val up: UploadResponse = uploadResult.value
            println("sending $file ${up.content_uri}")
            val fileinfo = FileInfo(type.toString(), file.length())
            val message = FileMessage(file.name, up.content_uri, fileinfo)
            val r = api.sendMessage(room, message)
            if (r is Result.Failure) {
                withContext(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to send file $file")
                            .text("Error ${r.error}")
                            .showError()
                }
            }

        }
    }
}

fun sendImageMessage(room: RoomId) {
    val dialog = FileChooser()
    dialog.title = "Find an image to send"

    val file = dialog.showOpenDialog(FX.primaryStage)
    file?:return

    val type = file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    GlobalScope.launch {
        val uploadResult = uploadFile(api, file, type)
        if (uploadResult is Result.Success) {
            val up: UploadResponse = uploadResult.value
            println("sending image $file ${up.content_uri}")
            val msg = ImageMessage(file.name, up.content_uri)
            val r = api.sendMessage(room, msg)
            if (r is Result.Failure) {
                withContext(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to send image $file")
                            .text("Error ${r.error}")
                            .showError()
                }
            }
        }
    }
}