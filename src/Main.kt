import java.io.File

fun main(args: Array<String>) {
    val musicXml = File("./resources/jingle_bells.xml")

    val music = Music()

    music.getNotes().subscribe { note ->
        println("Note parsed: tone = ${note.toneString} value = ${note.value}  duration = ${note.duration}  onVelocity = ${note.onVelocity}  offVelocity = ${note.offVelocity}")
    }

    val gpio: Gpio?
    gpio = try {
        Gpio()
    } catch (e: java.lang.UnsatisfiedLinkError) {
        println("Failed to setup GPIO. Is Pi4j installed?")
        null
    }

    gpio?.subscribeTo(music.getNotes())

    music.play(musicXml)

    gpio?.shutdown()
}

