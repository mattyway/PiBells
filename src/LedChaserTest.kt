import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.Pin
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin

fun main(args: Array<String>) {

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

    // create gpio controller
    val gpio = GpioFactory.getInstance()

    println("Setting up pins")
    val pins = pinDescriptors.map {
        gpio.provisionDigitalOutputPin(it.number, it.name, PinState.LOW).apply {
            setShutdownOptions(true, PinState.LOW)
        }
    }

    var increment = 1
    var index = 0

    var loops = 0

    while (loops < 3) {
        pins[index].pulse(150, true)

        index += increment

        if (index >= pins.size) {
            index = pins.size - 2
            increment = -1
        }
        if (index < 0) {
            index = 1
            increment = 1

            loops++
        }
    }

    println("Shutting down")
    gpio.shutdown()
}