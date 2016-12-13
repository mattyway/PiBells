import org.jfugue.integration.MusicXmlParser
import org.jfugue.midi.MidiParserListener
import org.jfugue.parser.ParserListener
import org.jfugue.parser.ParserListenerAdapter
import org.jfugue.player.ManagedPlayer
import org.jfugue.temporal.TemporalPLP
import org.jfugue.theory.Note
import org.staccato.StaccatoParser
import rx.Emitter
import rx.Observable
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import kotlin.concurrent.thread

class Music() {
    class PassthroughListener : ParserListenerAdapter() {
        var parserListener: ParserListener? = null

        override fun onNoteParsed(note: Note?) {
            parserListener?.onNoteParsed(note)
        }
    }

    val player = ManagedPlayer()
    val passthroughParser = PassthroughListener()

    fun getNotes(): Observable<Note> {
        return Observable.fromEmitter<Note>({ emitter ->
            val listener = object : ParserListenerAdapter() {
                override fun onNoteParsed(note: Note) {
                    emitter.onNext(note)
                }
            }

            emitter.setCancellation {
                passthroughParser.parserListener = listener
            }
            passthroughParser.parserListener = listener

        }, Emitter.BackpressureMode.BUFFER)
    }

    fun play(musicXml: File) {
        val musicXmlParser = MusicXmlParser()
        val midiParserListener = MidiParserListener()
        val temporalParser = TemporalPLP()

        musicXmlParser.addParserListener(midiParserListener)
        musicXmlParser.addParserListener(temporalParser)
        temporalParser.addParserListener(passthroughParser)

        val originalStream = System.out

        val dummyStream = PrintStream(object : OutputStream() {
            override fun write(b: Int) {
                //NO-OP
            }
        })

        // MusicXmlParser is noisy. Redirect System.out so we don't have to read the output of MusicXmlParser
        System.setOut(dummyStream)
        musicXmlParser.parse(musicXml)

        // Restore original System.out so that we can output to the console later
        System.setOut(originalStream)

        thread {
            player.start(midiParserListener.sequence)
        }
        temporalParser.parse()
    }

    fun play(musicString: String) {
        val stacattoParser = StaccatoParser()
        val midiParserListener = MidiParserListener()
        val temporalParser = TemporalPLP()

        stacattoParser.addParserListener(midiParserListener)
        stacattoParser.addParserListener(temporalParser)
        temporalParser.addParserListener(passthroughParser)

        stacattoParser.parse(musicString)

        thread {
            player.start(midiParserListener.sequence)
        }
        temporalParser.parse()
    }
}