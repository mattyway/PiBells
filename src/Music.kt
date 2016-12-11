import org.jfugue.integration.MusicXmlParser
import org.jfugue.parser.ParserListener
import org.jfugue.parser.ParserListenerAdapter
import org.jfugue.player.Player
import org.jfugue.temporal.TemporalPLP
import org.jfugue.theory.Note
import org.staccato.StaccatoParser
import org.staccato.StaccatoParserListener
import rx.Emitter
import rx.Observable
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

class Music() {
    class PassthroughListener : ParserListenerAdapter() {
        var parserListener: ParserListener? = null

        override fun onNoteParsed(note: Note?) {
            parserListener?.onNoteParsed(note)
        }
    }

    val player = Player()
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
        val temporalParser = TemporalPLP()
        val staccatoParser = StaccatoParserListener()

        musicXmlParser.addParserListener(staccatoParser)
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

        val musicXMLPattern = staccatoParser.pattern
        player.delayPlay(0, musicXMLPattern)
        temporalParser.parse()
    }

    fun play(musicString: String) {
        val stacattoParser = StaccatoParser()
        val temporalParser = TemporalPLP()

        stacattoParser.addParserListener(temporalParser)
        temporalParser.addParserListener(passthroughParser)

        stacattoParser.parse(musicString)

        player.delayPlay(0, musicString)
        temporalParser.parse()
    }
}