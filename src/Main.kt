import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.io.File

var gpio: Gpio? = null
val music = Music()

val subscriptions = CompositeSubscription()

fun main(args: Array<String>) {
    setup()

    val scaleString = "C4q D4q E4q F4q G4q A4q B4q C5q"
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

    music.getNotes()
            .observeOn(Schedulers.newThread())
            .subscribe { note ->
                println("Note parsed: tone = ${note.toneString} octave = ${note.octave} value = ${note.value}  duration = ${note.duration}")
            }
            .apply {
                subscriptions.add(this)
            }

    gpio?.subscribeTo(music.getNotes())
}

private fun loop() {
    while (true) {
        val resourceDir = File("./resources")
        val files = resourceDir.listFiles { dir, name -> name.endsWith(".xml") }

        println("Select file")
        files.forEachIndexed { index, file ->
            println("${index + 1}: ${file.name}")
        }

        val input = readLine()
        val selectedIndex = input?.toInt()?.minus(1)

        if (selectedIndex == null || selectedIndex < 0 || selectedIndex >= files.size) {
            println("Invalid input")
            break
        }

        val file = files[selectedIndex]
        music.play(file)
    }
}

