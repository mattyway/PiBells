import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

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

