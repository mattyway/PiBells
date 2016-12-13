import java.io.File


fun main(args: Array<String>) {
    val resourceDir = File("./resources")
    val files = resourceDir.listFiles { dir, name -> name.endsWith(".xml") }

    files.forEach { file ->
        println("Processing file ${file.name}")
        var text = file.readText()

        val dtdRegex = Regex("\"http://www\\.musicxml\\.org/dtds/partwise\\.dtd\"")
        text = dtdRegex.replace(text, "\"./musicxmldtd/partwise.dtd\"")

        val divisionsRegex = Regex("<divisions>(\\d*)</divisions>")

        val divisionsResult = divisionsRegex.find(text)
        val divisions: Int?
        divisions = try {
            divisionsResult?.groupValues?.getOrNull(1)?.toInt()
        } catch (e: NumberFormatException) {
            null
        }

        if (divisions != null) {
            var newDivisions = divisions
            var factor = 1
            while (newDivisions > 127) {
                newDivisions /= 2
                factor *= 2
            }

            if (newDivisions != divisions) {
                println("New Divisions = $newDivisions Factor = $factor")

                text = divisionsRegex.replace(text, "<divisions>$newDivisions</divisions>")

                val durationRegex = Regex("<duration>(\\d*)</duration>")
                val durationResults = durationRegex.findAll(text)

                val uniqueDurations: Sequence<Int> = durationResults.map {
                    try {
                        it.groupValues.getOrNull(1)?.toInt()
                    } catch (e: NumberFormatException) {
                        null
                    }
                }
                        .filterNotNull()
                        .distinct()
                        .sorted()

                uniqueDurations.forEach {
                    println("Found duration $it. Will be changed to ${it / factor}")

                    val replaceRegex = Regex("<duration>$it</duration>")
                    text = replaceRegex.replace(text, "<duration>${it / factor}</duration>")
                }
            }
        }

        file.writeText(text)
    }
}