import com.pi4j.io.gpio.*
import com.pi4j.io.gpio.event.GpioPinListenerDigital
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

    val keypadOutputPinDescriptors = listOf(
            PinDescriptor("column1", RaspiPin.GPIO_21),
            PinDescriptor("column2", RaspiPin.GPIO_23),
            PinDescriptor("column3", RaspiPin.GPIO_25)
    )

    val keypadInputPinDescriptors = listOf(
            PinDescriptor("row1", RaspiPin.GPIO_22),
            PinDescriptor("row2", RaspiPin.GPIO_29),
            PinDescriptor("row3", RaspiPin.GPIO_28),
            PinDescriptor("row4", RaspiPin.GPIO_24)
    )

    var gpio: GpioController? = null
    var bellPins: List<GpioPinDigitalOutput> = emptyList()
    var keypadOutputPins: List<GpioPinDigitalOutput> = emptyList()
    var keypadInputPins: List<GpioPinDigitalInput> = emptyList()

    var pulseLength: Long = 200
    var enablePinLogging: Boolean = false

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

        keypadOutputPins = keypadOutputPinDescriptors.map {
            gpio!!.provisionDigitalOutputPin(it.number, it.name, PinState.LOW).apply {
                setShutdownOptions(true, PinState.LOW)
            }
        }

        keypadInputPins = keypadInputPinDescriptors.map {
            gpio!!.provisionDigitalInputPin(it.number, it.name).apply {
                setShutdownOptions(true)
                addListener(GpioPinListenerDigital { event ->
                    if (enablePinLogging) {
                        println("GPIO PIN STATE CHANGE: ${event.pin.name} = ${event.state}")
                    }
                })
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
                            .doOnNext { note ->
                                val pin = getPinForNote(note)
                                if (pin != null) {

                                    if (enablePinLogging) {
                                        println("Setting pin ${pin.name} to high on thread ${Thread.currentThread().name} at ${System.currentTimeMillis()}")
                                    }
                                    pin.high()

                                    Thread.sleep(pulseLength)

                                    pin.low()
                                    if (enablePinLogging) {
                                        println("Setting pin ${pin.name} to low on thread ${Thread.currentThread().name}  at ${System.currentTimeMillis()}")
                                    }
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