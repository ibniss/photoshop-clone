import javafx.scene.chart.XYChart
import tornadofx.Controller

/**
 * A controller class managing histogram-creation related operations.
 *
 * @author Krzysztof Bielikowicz 963560
 * This is my own work.
 */
class HistogramController : Controller() {
    /**
     * Creates a red color series for the given [data] set.
     */
    fun createRedSeries(data: HistogramData): XYChart.Series<String, Number> {
        val series = XYChart.Series<String, Number>()
        for (index in data.red.indices) {
            series.data.add(XYChart.Data<String, Number>(index.toString(), data.red[index]))
        }
        return series
    }

    /**
     * Creates a green color series for the given [data] set.
     */
    fun createGreenSeries(data: HistogramData): XYChart.Series<String, Number> {
        val series = XYChart.Series<String, Number>()
        for (index in data.green.indices) {
            series.data.add(XYChart.Data<String, Number>(index.toString(), data.green[index]))
        }
        return series
    }

    /**
     * Creates a blue color series for the given [data] set.
     */
    fun createBlueSeries(data: HistogramData): XYChart.Series<String, Number> {
        val series = XYChart.Series<String, Number>()
        for (index in data.blue.indices) {
            series.data.add(XYChart.Data<String, Number>(index.toString(), data.blue[index]))
        }
        return series
    }

    /**
     * Creates a RGB series for the given [data] set.
     */
    fun createGreySeries(data: HistogramData): XYChart.Series<String, Number> {
        val series = XYChart.Series<String, Number>()
        for (index in data.grey.indices) {
            series.data.add(XYChart.Data<String, Number>(index.toString(), data.grey[index]))
        }
        return series
    }
}