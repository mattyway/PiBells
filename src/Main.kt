import com.pi4j.io.gpio.*
import org.jfugue.integration.MusicXmlParser
import org.jfugue.parser.ParserListenerAdapter
import org.jfugue.player.Player
import org.jfugue.temporal.TemporalPLP
import org.jfugue.theory.Note
import org.staccato.StaccatoParserListener
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

fun main(args: Array<String>) {
    val musicXml = File("./resources/jingle_bells.xml")

    val musicXmlParser = MusicXmlParser()
    val temporalParser = TemporalPLP()
    val debugParser = DebugParserListener()
    val gpioParser = GpioParserListener().apply { setup() }
    val staccatoParser = StaccatoParserListener()
    musicXmlParser.addParserListener(staccatoParser)
    musicXmlParser.addParserListener(temporalParser)
    temporalParser.addParserListener(debugParser)
    temporalParser.addParserListener(gpioParser)

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

    gpioParser.shutdown()
}

class GpioParserListener : ParserListenerAdapter() {
    data class PinDescriptor(
            val name: String,
            val number: Pin
    )

    val pinDescriptors = listOf(
            PinDescriptor("C", RaspiPin.GPIO_01),
            PinDescriptor("D", RaspiPin.GPIO_02),
            PinDescriptor("E", RaspiPin.GPIO_03),
            PinDescriptor("F", RaspiPin.GPIO_04),
            PinDescriptor("G", RaspiPin.GPIO_05),
            PinDescriptor("A", RaspiPin.GPIO_06),
            PinDescriptor("B", RaspiPin.GPIO_07),
            PinDescriptor("highC", RaspiPin.GPIO_00)
    )

    var gpio: GpioController? = null
    var pins: List<GpioPinDigitalOutput> = emptyList()

    fun setup() {
        println("Setting up pins")

        // Create gpio controller
        gpio = GpioFactory.getInstance()

        // Create the pin objects from the pin descriptors
        pins = pinDescriptors.map {
            gpio!!.provisionDigitalOutputPin(it.number, it.name, PinState.LOW).apply {
                setShutdownOptions(true, PinState.LOW)
            }
        }
    }

    fun shutdown() {
        println("Shutting down pins")

        gpio?.shutdown()
    }

    override fun onNoteParsed(note: Note) {
        // Find the first pin with a name that matches the note
        // TODO: highC isn't detected. Need to take into account the note's octave and not just match on the name
        val pin = pins.firstOrNull { note.toneString?.contains(it.name) ?: false }

        // Turn the pin on for 200ms then turn it off
        pin?.pulse(200)
    }
}