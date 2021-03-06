package link.continuum.desktop.gui.icon.avatar

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.scene.text.Text
import link.continuum.desktop.gui.*
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging
import java.util.*
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

class InitialIcon(
) {
    private val radii =  CornerRadii( 0.2, true)

    private val charL = Text().apply {
        fill = Color.WHITE
    }
    private val charR = Text().apply { fill = Color.WHITE }
    private val charC = Text().apply { fill = Color.WHITE }

    private val two = HBox().apply {
        style {
            fontSize = 1.em
        }
        alignment = Pos.CENTER
        vbox {
            alignment = Pos.CENTER_RIGHT
            add(charL)
        }
        vbox {
            style {
                prefWidth = 0.1.em
            }
        }
        vbox {
            alignment = Pos.CENTER_LEFT
            add(charR)
        }
    }
    private val one = HBox().apply {
        alignment = Pos.CENTER
        children.add(charC)
        style { fontSize = 1.8.em }
    }
    val root = StackPane().apply {
        style = avStyle
    }

    fun show() {
        debugAssertUiThread()
        this.root.isManaged = true
        this.root.isVisible = true
    }

    fun hide() {
        this.root.isManaged = false
        this.root.isVisible = false
    }

    fun updateColor(color: Color) {
        debugAssertUiThread()
        root.background = backgrounds.computeIfAbsent(color) {
            logger.debug { "initial icon $charL $charR $color" }
            Background(BackgroundFill(it, radii, Insets.EMPTY))
        }
    }

    fun updateItem(charL: String, charR: String, color: Color) {
        updateColor(color)
        updateCharPair(charL, charR)
    }

    fun updateCharPair(charL: String, charR: String) {
        debugAssertUiThread()
        this.charL.text = charL
        this.charR.text = charR
        root.children.setAll(two)
    }

    fun updateCenter(char: String, color: Color) {
        updateColor(color)
        updateCharSingle(char)
    }

    fun updateCharSingle(char: String) {
        debugAssertUiThread()
        charC.text = char
        root.children.setAll(one)
    }

    fun updateString(input: String) {
        val (c1, c2) = extractKeyChar(input)
        if (c2 != null) {
            updateCharPair(c1, c2)
        } else {
            updateCharSingle(c1)
        }
    }
    fun updateItem(input: String, color: Color) {
        val (c1, c2) = extractKeyChar(input)
        if (c2 != null) {
            updateItem(c1, c2, color)
        } else {
            updateCenter(c1, color)
        }
    }

    companion object {
        private val backgrounds = WeakHashMap<Color, Background>()
        private val avStyle = StyleBuilder().apply {
            val s = 2.em
            prefHeight = s
            prefWidth = s
            fontFamily = GenericFontFamily.sansSerif
        }.toString()
    }
}

internal fun extractKeyChar(input: String): Pair<String, String?> {
    val trim = input.replace("(IRC)", "").trim()
    val cps = trim.codePoints().toList()
    val ideo = cps.find { Character.isIdeographic(it)  }
    if (ideo != null) {
        return String(Character.toChars(ideo)) to null
    }
    val first = cps.firstOrNull()?.let { String(Character.toChars(it)) } ?: ""
    val i2 = cps.indexOfFirst { Character.isSpaceChar(it)  }.let { if (it < 0) null else it + 1} ?: 1
    val second = cps.getOrNull(i2)?.let { String(Character.toChars(it))  }
    return first to second
}
