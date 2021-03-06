import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.io.File

var gpio: Gpio? = null
val music = Music()

val subscriptions = CompositeSubscription()

val enableNoteLogging: Boolean = false

val scaleString = "C4q D4q E4q F4q G4q A4q B4q C5q"

fun main(args: Array<String>) {
    setup()

    music.play(scaleString)

    loop()

    gpio?.shutdown()
    subscriptions.unsubscribe()
}

private fun setup() {
    gpio = try {
        Gpio()
    } catch (e: UnsatisfiedLinkError) {
        println("Failed to setup GPIO. Is Pi4j installed?")
        null
    }

    if (enableNoteLogging) {
        music.getNotes()
                .observeOn(Schedulers.newThread())
                .subscribe { note ->
                    println("Note parsed: tone = ${note.toneString} octave = ${note.octave} value = ${note.value}  duration = ${note.duration}")
                }
                .apply {
                    subscriptions.add(this)
                }
    }

    gpio?.subscribeTo(music.getNotes())
}

private fun loop() {
    while (true) {
        val resourceDir = File("./resources")
        val files: List<File> = resourceDir.listFiles { dir, name -> name.endsWith(".xml") }.sortedBy { it.name }

        println("Select file")
        files.forEachIndexed { index, file ->
            println("${index + 1}: ${file.name}")
        }
        println("${files.size + 1}: Scale")

        val input = readLine()
        val selectedIndex = try {
            input?.toInt()?.minus(1)
        } catch(e: Exception) {
            println("Invalid input")
            break
        }

        if (selectedIndex == null) {
            println("Invalid input")
            break
        }

        if (selectedIndex == files.size) {
            music.play(scaleString)
        } else if (selectedIndex >= 0 && selectedIndex < files.size) {
            val file = files[selectedIndex]
            music.play(file)
        }
    }
}

