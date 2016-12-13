import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

var gpio: Gpio? = null
val music = Music()

fun main(args: Array<String>) {
    setup()

    val resourceDir = File("./resources")
    val files = resourceDir.listFiles { dir, name -> name.endsWith(".xml") }

    println("Select file")
    files.forEachIndexed { index, file ->
        println("${index + 1}: ${file.name}")
    }

    val input = readLine()
    val selectedIndex = input?.toInt()?.minus(1)

    if(selectedIndex == null || selectedIndex < 0 || selectedIndex >= files.size) {
        println("Invalid input")
        return
    }

    val musicXml = files[selectedIndex]

    println("Playing ${musicXml.name}")

    val scaleString = "C4q D4q E4q F4q G4q A4q B4q C5q"

    val musicObservable: Observable<Any> = Observable.from(listOf(scaleString, musicXml))

    musicObservable
            .concatMap { Observable.just(it).delay(1, TimeUnit.SECONDS) }
            .toBlocking()
            .subscribe({ it ->
                if (it is File) {
                    music.play(it)
                } else if (it is String) {
                    music.play(it)
                }
            }, { e ->
                println("Error playing back music: $e")
            })


    gpio?.shutdown()
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

    gpio?.subscribeTo(music.getNotes())
}
