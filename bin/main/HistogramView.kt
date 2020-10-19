import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.paint.Color
import tornadofx.*
import tornadofx.Stylesheet.Companion.comboBox
import tornadofx.getValue
import tornadofx.setValue

/**
 * The histogram chart pop-up window.
 *
 * @author Krzysztof Bielikowicz 963560
 * This is my own work.
 */
class HistogramView : View("Histogram") {
    private val controller: HistogramController by inject()

    val histogramData: HistogramData by param() // data passed to the view

    // currently selected histogram mode
    private val selectedHistogramProperty = SimpleStringProperty("Brightness")
    private var selectedHistogram by selectedHistogramProperty

    // currently displayed chart series
    private val seriesProperty = SimpleObjectProperty<XYChart.Series<String, Number>>(XYChart.Series())
    private var series by seriesProperty

    // displayed chart object
    private val chartProperty = SimpleObjectProperty<BarChart<String, Number>>()
    private var chart by chartProperty

    override val root = borderpane {
        top = hbox {
            combobox(
                selectedHistogramProperty,
                FXCollections.observableArrayList("Brightness", "Green", "Red", "Blue")
            ) {
                onAction = EventHandler {
                    runAsync {
                        when (selectedHistogram) {
                            "Brightness" -> controller.createGreySeries(histogramData)
                            "Green" -> controller.createGreenSeries(histogramData)
                            "Red" -> controller.createRedSeries(histogramData)
                            else -> controller.createBlueSeries(histogramData)
                        }
                    } ui { newSeries ->
                        series = newSeries

                        chart.data.clear()
                        chart.data.add(series)

                        // change the color of bars to a new one depending on the chosen histogram mode
                        val bars = chart.lookupAll(".chart-bar")
                        val newBarColor = when (selectedHistogram) {
                            "Brightness" -> "-fx-bar-fill: black;"
                            "Green" -> "-fx-bar-fill: green;"
                            "Red" -> "-fx-bar-fill: red;"
                            else -> "-fx-bar-fill: blue;"
                        }
                        for (bar in bars) {
                            bar.style = newBarColor
                        }
                    }
                }
            }
            alignment = Pos.CENTER
            chart = barchart(
                "Histogram",
                CategoryAxis(),
                NumberAxis()
            ) {
                animated = false
                isLegendVisible = false
            }
            center = chart
        }
    }

    init {
        with(root) {
            prefWidth = 1024.0
            prefHeight = 768.0
        }

        // whenever the histogram open is opened
        subscribe<HistogramOpenEvent> {
            chart.data.clear()
            series = controller.createGreySeries(histogramData)
            chart.data.add(series)

            chart.yAxis.isAutoRanging = false
            (chart.yAxis as NumberAxis).tickUnit = 5000.0
            (chart.yAxis as NumberAxis).lowerBound = 0.0

            // set the maximum possible y value on the chart to the maximum value on the histogram
            val allHistogramValues = histogramData.green + histogramData.red + histogramData.blue
            runAsync {
                allHistogramValues.maxBy { it }
            } ui { maxAxisValue ->
                (chart.yAxis as NumberAxis).upperBound = maxAxisValue?.toDouble() ?: 100000.0
            }

            selectedHistogram = "Brightness"

            // initially change the bar color to black
            val bars = chart.lookupAll(".chart-bar")
            for (bar in bars) {
                bar.style = "-fx-bar-fill: black;"
            }
        }
    }
}
