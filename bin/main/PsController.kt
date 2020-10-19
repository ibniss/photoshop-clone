import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import tornadofx.Controller

/**
 * A controller class managing image-processing algorithms.
 *
 * @author Krzysztof Bielikowicz 963560
 * This is my own work.
 */
class PsController : Controller() {
    /**
     * Invert the colours of the given [image].
     */
    fun invert(image: Image): Image {
        // find the width and height of the image to be processed
        val width = image.width.toInt()
        val height = image.height.toInt()

        // create a new image of that size
        val invertedImage = WritableImage(width, height)
        // get an interface to write to that image memory
        val invertedImageWriter = invertedImage.pixelWriter
        // get an interface to read from the original image passed as the parameter to the function
        val imageReader = image.pixelReader

        // iterate over all pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // for each pixel, get the colour
                var color = imageReader.getColor(x, y)

                // invert the colour
                color = Color.color(1.0 - color.red, 1.0 - color.green, 1.0 - color.blue)
                // apply the new colour
                invertedImageWriter.setColor(x, y, color)
            }
        }
        return invertedImage
    }

    /**
     * Converts the given [image] to grey scale.
     */
    fun convertToGreyScale(image: Image): Image {
        // find the width and height of the image to be processed
        val width = image.width.toInt()
        val height = image.height.toInt()

        // create a new image of that size
        val newImage = WritableImage(width, height)
        // get an interface to write to that image memory
        val newImageWriter = newImage.pixelWriter
        // get an interface to read from the original image passed as the parameter to the function
        val imageReader = image.pixelReader

        // iterate over all pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // for each pixel, get the colour
                var color = imageReader.getColor(x, y)

                // calculating 0-255 values for each color
                val red = (color.red * 255).toInt()
                val green = (color.green * 255).toInt()
                val blue = (color.blue * 255).toInt()

                val grey = (red + green + blue) / 3
                // invert the colour
                color = Color.rgb(grey, grey, grey)

                // apply the new colour
                newImageWriter.setColor(x, y, color)
            }
        }
        return newImage
    }

    /**
     * Apply histogram equalization to the given [image] using HSB brightness values.
     * Currently does not work properly (issues with getBrightness?).
     */
    fun equalizeHistogram(image: Image): Image {
        // find the width and height of the image to be processed
        val width = image.width.toInt()
        val height = image.height.toInt()

        // create a new image of that size
        val newImage = WritableImage(width, height)
        // get an interface to write to that image memory
        val newImageWriter = newImage.pixelWriter
        // get an interface to read from the original image passed as the parameter to the function
        val imageReader = image.pixelReader

        val greyValues = IntArray(256)

        // collect amounts of brightness values in the image
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = imageReader.getColor(x, y)
                greyValues[(color.brightness * 255).toInt()]++
            }
        }

        val lookupTable = calculateEqualizationLookupTable(greyValues, image.width * image.height)

        // apply the calculated lookup table
        for (y in 0 until height) {
            for (x in 0 until width) {
                val currentColor = imageReader.getColor(x, y)
                val newColor =
                    Color.hsb(
                        currentColor.hue,
                        currentColor.saturation,
                        lookupTable[(currentColor.brightness * 255).toInt()
                        ]
                    )
                newImageWriter.setColor(x, y, newColor)
            }
        }

        return newImage
    }

    /**
     * Applies a given filter [kernel] to the given [image].
     */
    fun applyFilterKernel(image: Image, kernel: Kernel): Image {
        // find the width and height of the image to be processed
        val width = image.width.toInt()
        val height = image.height.toInt()

        // create a new image of that size
        val newImage = WritableImage(width, height)
        // get an interface to write to that image memory
        val newImageWriter = newImage.pixelWriter
        // get an interface to read from the original image passed as the parameter to the function
        val imageReader = image.pixelReader

        // 2D arrays for new calculated color values
        val redValues = Array(height) { IntArray(width) }
        val greenValues = Array(height) { IntArray(width) }
        val blueValues = Array(height) { IntArray(width) }

        var minVal = Int.MAX_VALUE
        var maxVal = Int.MIN_VALUE
        val offset = (kernel.matrix.size - 1) / 2

        // iterate over all pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // check if the kernel can be centered over the pixel - if it is in the borders
                val canBeCentered =
                    !(x < offset || x > (width - offset - 1) || y < offset || y > (height - offset - 1))

                if (!canBeCentered) {
                    // make the pixel black
                    newImageWriter.setColor(x, y, Color.color(0.0, 0.0, 0.0))
                } else {
                    // calculate the new pixel values using the kernel
                    val newPixelValues = calculatePixelValues(
                        getImageFragment(offset, imageReader, x, y),
                        kernel
                    )

                    // check if there is a larger max or a smaller min
                    for (newValue in newPixelValues.toList()) {
                        if (newValue < minVal) {
                            minVal = newValue
                        }
                        if (newValue > maxVal) {
                            maxVal = newValue
                        }
                    }

                    // put the values into the arrays
                    redValues[y][x] = newPixelValues.first
                    greenValues[y][x] = newPixelValues.second
                    blueValues[y][x] = newPixelValues.third
                }
            }
        }

        // normalize the values
        normalize(redValues, minVal, maxVal)
        normalize(greenValues, minVal, maxVal)
        normalize(blueValues, minVal, maxVal)

        // apply new values to the image
        for (y in offset until (height - offset)) {
            for (x in offset until (width - offset)) {
                newImageWriter.setColor(x, y, Color.rgb(redValues[y][x], greenValues[y][x], blueValues[y][x]))
            }
        }

        return newImage
    }

    /**
     * Normalize values in the given [unNormalized] array depending on the given [min] and [max] values.
     */
    private fun normalize(unNormalized: Array<IntArray>, min: Int, max: Int) {
        for (row in unNormalized) {
            for (i in 0 until row.size) {
                row[i] = ((row[i] - min) * 255) / (max - min)
            }
        }
    }

    /**
     * Calculates new pixel value given the [imageFragment] around the pixel and the [kernel].
     */
    private fun calculatePixelValues(
        imageFragment: ArrayList<ArrayList<Color>>,
        kernel: Kernel
    ): Triple<Int, Int, Int> {
        var redValue = 0
        var greenValue = 0
        var blueValue = 0

        for (i in 0 until kernel.matrix.size) {
            for (j in 0 until kernel.matrix.size) {
                // multiply the pixel color values by respective kernel values and  add to the values
                redValue += kernel.matrix[i][j] * (imageFragment[i][j].red * 255).toInt()
                greenValue += kernel.matrix[i][j] * (imageFragment[i][j].green * 255).toInt()
                blueValue += kernel.matrix[i][j] * (imageFragment[i][j].blue * 255).toInt()
            }
        }

        return Triple(redValue, greenValue, blueValue)
    }

    /**
     * Gets a fragment of the image given by the [imageReader] around the current [x] [y] position
     */
    private fun getImageFragment(offset: Int, imageReader: PixelReader, x: Int, y: Int): ArrayList<ArrayList<Color>> {
        val imageFragment = ArrayList<ArrayList<Color>>()

        for (j in -offset..offset) {
            imageFragment.add(ArrayList())
            for (i in -offset..offset) {
                imageFragment.last() += imageReader.getColor(x + i, y + j)
            }
        }
        return imageFragment
    }

    /**
     * Apply the given [lookupTable] to the given [image].
     */
    fun applyLookupTable(image: Image, lookupTable: DoubleArray): Image {
        // find the width and height of the image to be processed
        val width = image.width.toInt()
        val height = image.height.toInt()

        // create a new image of that size
        val newImage = WritableImage(width, height)
        // get an interface to write to that image memory
        val newImageWriter = newImage.pixelWriter
        // get an interface to read from the original image passed as the parameter to the function
        val imageReader = image.pixelReader

        // iterate over all pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // for each pixel, get the colour
                var color = imageReader.getColor(x, y)

                // calculating new 0-255 values for each color
                val red = (color.red * 255).toInt()
                val green = (color.green * 255).toInt()
                val blue = (color.blue * 255).toInt()

                // calculated the gamma-corrected color by calculating the color 0-1 value to the power of 1/gamma
                color = Color.color(
                    lookupTable[red],
                    lookupTable[green],
                    lookupTable[blue]
                )

                // apply the new colour
                newImageWriter.setColor(x, y, color)
            }
        }
        return newImage
    }

    /**
     * Calculates histogram data from the given [image].
     */
    fun calculateHistogramData(image: Image): HistogramData {
        // find the width and height of the image to be processed
        val width = image.width.toInt()
        val height = image.height.toInt()

        // create arrays to hold the histogram data in
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val grey = IntArray(256)

        // get an interface to read from the original image passed as the parameter to the function
        val imageReader = image.pixelReader

        for (y in 0 until height) {
            for (x in 0 until width) {
                // for each pixel, get the colour
                val color = imageReader.getColor(x, y)

                // calculating new 0-255 values for each color
                val redRGB = (color.red * 255).toInt()
                val greenRGB = (color.green * 255).toInt()
                val blueRGB = (color.blue * 255).toInt()
                val greyRGB = (redRGB + greenRGB + blueRGB) / 3

                // increment the amount of pixels with given rgb values
                red[redRGB]++
                green[greenRGB]++
                blue[blueRGB]++
                grey[greyRGB]++
            }
        }

        return HistogramData(red, green, blue, grey)
    }

    /**
     * Creates a gamma correction lookup table for a given [gamma] value.
     */
    fun createGammaLookupTable(gamma: Double): DoubleArray {
        val gammaCorrection = 1.0 / gamma
        val lookupTable = DoubleArray(256)
        for (i in 0..255) {
            lookupTable[i] = Math.pow(i / 255.0, gammaCorrection)
        }
        return lookupTable
    }

    /**
     * Creates a contrast stretching lookup table given two points [p1] and [p2].
     */
    fun createContrastStretchingLookupTable(p1: Pair<Double, Double>, p2: Pair<Double, Double>): DoubleArray {
        val lookupTable = DoubleArray(256)
        for (i in 0..255) {
            if (i < p1.first) {
                lookupTable[i] = ((p1.second / p1.first) * i) / 255
            } else if (i >= p1.first && i <= p2.first) {
                lookupTable[i] =
                    (((p2.second - p1.second) / (p2.first - p1.first)) * (i - p1.first) + p1.second) / 255
            } else {
                lookupTable[i] = (((255 - p2.second) / (255 - p2.first)) * (i - p2.first) + p2.second) / 255
            }
        }
        return lookupTable
    }

    /**
     * Creates a grey equalization lookup table using given [histogramData] and [imageSize].
     */
    fun createGreyEqualizationLookupTable(histogramData: HistogramData, imageSize: Double): DoubleArray {
        val lookupTable = DoubleArray(256)
        var t = 0 // grey cumulative

        for (i in 0..255) {
            t += histogramData.red[i] // doesn't matter which channel is used since the image is grey
            lookupTable[i] = t / imageSize
        }

        return lookupTable
    }

    /**
     * Creates a equalization lookup table using given [brightnessValues] and [imageSize].
     */
    private fun calculateEqualizationLookupTable(brightnessValues: IntArray, imageSize: Double): DoubleArray {
        val lookupTable = DoubleArray(256)
        var t = 0 // brightness cumulative

        for (i in 0..255) {
            t += brightnessValues[i]
            lookupTable[i] = t / imageSize
        }

        return lookupTable
    }
}