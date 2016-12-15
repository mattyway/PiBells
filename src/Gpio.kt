import com.pi4j.io.gpio.*
import org.jfugue.theory.Note
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.util.concurrent.Executors

class Gpio() {
    data class PinDescriptor(
            val name: String,
            val number: com.pi4j.io.gpio.Pin
    )

    val bellPinDescriptors = listOf(
            PinDescriptor("C", RaspiPin.GPIO_27),
            PinDescriptor("D", RaspiPin.GPIO_26),
            PinDescriptor("E", RaspiPin.GPIO_06),
            PinDescriptor("F", RaspiPin.GPIO_04),
            PinDescriptor("G", RaspiPin.GPIO_01),
            PinDescriptor("A", RaspiPin.GPIO_14),
            PinDescriptor("B", RaspiPin.GPIO_00),
            PinDescriptor("highC", RaspiPin.GPIO_07)
    )

    var gpio: GpioController? = null
    var bellPins: List<GpioPinDigitalOutput> = emptyList()

    var pulseLength: Long = 200

    private var subscription: Subscription? = null

    init {
        println("Setting up GPIO")

        // Create gpio controller
        gpio = GpioFactory.getInstance()

        // Create the pin objects from the pin descriptors
        bellPins = bellPinDescriptors.map {
            gpio!!.provisionDigitalOutputPin(it.number, it.name, PinState.LOW).apply {
                setShutdownOptions(true, PinState.LOW)
            }
        }

    }

    private fun getPinForNote(note: Note): GpioPinDigitalOutput? {
        // Map notes directly to pins.
        // Only uses notes from C4 to C5 excluding sharp/flat notes.
        return when (note.value.toInt()) {
            48 -> bellPins.firstOrNull { it.name == "C" }
            50 -> bellPins.firstOrNull { it.name == "D" }
            52 -> bellPins.firstOrNull { it.name == "E" }
            53 -> bellPins.firstOrNull { it.name == "F" }
            55 -> bellPins.firstOrNull { it.name == "G" }
            57 -> bellPins.firstOrNull { it.name == "A" }
            59 -> bellPins.firstOrNull { it.name == "B" }
            60 -> bellPins.firstOrNull { it.name == "highC" }
            else -> null
        }
    }

    fun subscribeTo(notes: Observable<Note>) {
        val concurrentLimit = 4
        val executor = Executors.newFixedThreadPool(concurrentLimit)
        val scheduler = Schedulers.from(executor)
        subscription = notes
                .onBackpressureDrop({
                    println("Couldn't play a note ${it.value}")
                })
                .flatMap({ it ->
                    Observable.just(it)
                            .subscribeOn(scheduler)
                            .map { note ->
                                if (note.isRest) {
                                    return@map
                                }

                                // Find the first pin with a name that matches the note
                                val pin = getPinForNote(note)

                                if (pin != null) {
                                    // Turn the pin on then turn it off
                                    pin.pulse(pulseLength, true)
                                } else {
                                    println("Couldn't find a pin for note ${note.value}")
                                }
                            }
                }, concurrentLimit)
                .doOnUnsubscribe {
                    executor.shutdown()
                }
                .doAfterTerminate {
                    executor.shutdown()
                }
                .subscribe()
    }

    fun shutdown() {
        println("Shutting down GPIO")
        subscription?.unsubscribe()
        gpio?.shutdown()
    }
}