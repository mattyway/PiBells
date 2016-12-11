import rx.Observable
import rx.schedulers.Schedulers
import java.io.File

fun main(args: Array<String>) {
    val musicXml = File("./resources/jingle_bells.xml")

    val music = Music()

    music.getNotes()
            .observeOn(Schedulers.newThread())
            .subscribe { note ->
                println("Note parsed: tone = ${note.toneString} octave = ${note.octave} value = ${note.value}  duration = ${note.duration}")
            }

    val gpio: Gpio?
    gpio = try {
        Gpio()
    } catch (e: java.lang.UnsatisfiedLinkError) {
        println("Failed to setup GPIO. Is Pi4j installed?")
        null
    }

    gpio?.subscribeTo(music.getNotes())

    val scaleString = "C3q D3q E3q F3q G3q A3q B3q C4q"

    val musicObservable: Observable<Any> = Observable.from(listOf(scaleString, musicXml))

    musicObservable
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

