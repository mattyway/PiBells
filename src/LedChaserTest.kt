fun main(args: Array<String>) {

    val gpio = Gpio()

    var increment = 1
    var index = 0

    var loops = 0

    while (loops < 3) {
        gpio.pins[index].pulse(150, true)

        index += increment

        if (index >= gpio.pins.size) {
            index = gpio.pins.size - 2
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