import org.jfugue.integration.MusicXmlParser
import org.jfugue.parser.ParserListenerAdapter
import org.jfugue.player.Player
import org.jfugue.theory.Note
import org.staccato.StaccatoParserListener
import java.io.File
import org.jfugue.temporal.TemporalPLP
import java.io.OutputStream
import java.io.PrintStream




fun main(args: Array<String>) {
    val musicXml = File("./resources/jingle_bells.xml")

    val musicXmlParser = MusicXmlParser()
    val temporalParser = TemporalPLP()
    val myParser = MyParserListener()
    val staccatoParser = StaccatoParserListener()
    musicXmlParser.addParserListener(staccatoParser)
    musicXmlParser.addParserListener(temporalParser)
    temporalParser.addParserListener(myParser)

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

    val player = Player()
    val musicXMLPattern = staccatoParser.pattern
    player.delayPlay(0, musicXMLPattern)
    temporalParser.parse()
}

class MyParserListener : ParserListenerAdapter() {
    override fun onNoteParsed(note: Note) {
        println("Note parsed: tone = ${note.toneString} value = ${note.value}  duration = ${note.duration}  onVelocity = ${note.onVelocity}  offVelocity = ${note.offVelocity}")
    }
}