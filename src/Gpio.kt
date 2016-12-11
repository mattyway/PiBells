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

    private var subscription: Subscription? = null

    init {
        println("Setting up GPIO")

        // Create gpio controller
        gpio = GpioFactory.getInstance()

        // Create the pin objects from the pin descriptors
        pins = pinDescriptors.map {
            gpio!!.provisionDigitalOutputPin(it.number, it.name, PinState.LOW).apply {
                setShutdownOptions(true, PinState.LOW)
            }
        }

    }

    fun subscribeTo(notes: Observable<Note>) {
        val concurrentLimit = 4
        val executor = Executors.newFixedThreadPool(concurrentLimit)
        val scheduler = Schedulers.from(executor)
        subscription = notes
                .onBackpressureDrop({
                    println("Had to drop a note")
                })
                .flatMap({ it ->
                    Observable.just(it)
                            .subscribeOn(scheduler)
                            .map { note ->
                                // Find the first pin with a name that matches the note
                                // TODO: highC isn't detected. Need to take into account the note's octave and not just match on the name
                                val pin = pins.firstOrNull { note.toneString?.contains(it.name) ?: false }

                                // Turn the pin on for 200ms then turn it off
                                pin?.pulse(200, true)
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