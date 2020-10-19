import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.transform.Scale
import javafx.stage.FileChooser
import javafx.stage.Popup
import tornadofx.*
import tornadofx.getValue
import tornadofx.setValue

/**
 * Represents the main app window.
 *
 * @author Krzysztof Bielikowicz 963560
 * This is my own work.
 */
class PsView : View("Photoshop") {
    private val controller: PsController by inject()

    // currently opened image's name
    private val currentImageUrlProperty = SimpleStringProperty(null)
    private var currentImageUrl by currentImageUrlProperty

    // currently displayed image object
    private val mainImageProperty =
        SimpleObjectProperty<Image>(null)
    private var mainImage by mainImageProperty

    // gamma slider text
    private val gammaSliderValueTextProperty = SimpleStringProperty("0,10")
    private var gammaSliderValueText by gammaSliderValueTextProperty

    // gamma slider value
    private val sliderValueProperty = SimpleDoubleProperty(0.1)
    private var sliderValue by sliderValueProperty

    // zoom slider value
    private val imageSliderValueProperty = SimpleDoubleProperty(1.0)
    private var imageSliderValue by imageSliderValueProperty

    // displayed when dragging the zoom slider
    private var imageSliderPopup: Popup by singleAssign()

    // slider-dependant zoom transform
    private val imageViewTransform = Scale(1.0, 1.0, 0.0, 0.0)

    // contrast stretching chart series
    private val chartSeriesProperty =
        SimpleObjectProperty<XYChart.Series<Number, Number>>(XYChart.Series())
    private var chartSeries by chartSeriesProperty

    // user-created contrast stretching chart points
    private val userCreatedPointsProperty =
        SimpleListProperty<XYChart.Data<Number, Number>>(FXCollections.observableArrayList())
    private var userCreatedPoints by userCreatedPointsProperty

    // contrast stretching displayed coordinates
    private var chartCoordsTextField: TextField by singleAssign()

    // currently selected filter kernel
    private val selectedKernelProperty =
        SimpleStringProperty(Kernel.values().first().name)
    private var selectedKernel by selectedKernelProperty


    override val root = borderpane {
        top = toolbar {
            button("Open...") {
                action {
                    val chosenFiles = chooseFile(
                        "Choose an image to open",
                        arrayOf(FileChooser.ExtensionFilter("Image Files", "*.jpg")),
                        FileChooserMode.Single,
                        null
                    )
                    if (!chosenFiles.isNullOrEmpty()) {
                        currentImageUrl = chosenFiles.first().toURI().toURL().toString()
                        mainImage = Image(currentImageUrl)
                    }
                }
            }
        }
        center = scrollpane(true, true) {
            isPannable = true
            center = stackpane {
                group {
                    imageview {
                        transforms.add(imageViewTransform)
                        imageViewTransform.xProperty().bind(imageSliderValueProperty)
                        imageViewTransform.yProperty().bind(imageSliderValueProperty)
                        imageProperty().bind(mainImageProperty)
                    }
                }
            }
        }
        bottom = toolbar {
            imageview("zoom.png")
            slider {
                min = 0.0
                max = 4.0
                value = 1.0
                isShowTickMarks = false
                isShowTickMarks = false
                imageSliderValueProperty.bind(valueProperty())

                // create a popup displayed when slider is being dragged
                imageSliderPopup = Popup()
                imageSliderPopup.content.add(borderpane {
                    maxWidth = 75.0
                    style = "-fx-background-color: white; -fx-background-radius: 7; -fx-border-radius: 7;"
                    center = textfield {
                        isEditable = false
                        text = "100%"

                        // update the popup's text whenever slider is moved
                        imageSliderValueProperty.onChange {
                            text = Math.round(it * 100).toString() + "%"
                        }
                    }
                })

                // set up mouse listeners
                onMousePressed = EventHandler { me ->
                    imageSliderPopup.show(this@borderpane.scene.window, me.screenX, me.screenY)
                }

                onMouseDragged = EventHandler { me ->
                    imageSliderPopup.anchorX = me.screenX
                    imageSliderPopup.anchorY = me.screenY
                }

                onMouseReleased = EventHandler {
                    imageSliderPopup.hide()
                }
            }
        }

        right = vbox {
            minWidth = 256.0
            maxWidth = 400.0
            alignment = Pos.CENTER
            button("Reset") {
                maxWidth = Double.MAX_VALUE
                action {
                    currentImageUrl?.let {
                        mainImage = Image(it)
                    }
                }
            }
            button("Invert Colours") {
                maxWidth = Double.MAX_VALUE
                action {
                    mainImage?.let {
                        runAsync {
                            controller.invert(it)
                        } ui { correctedImage ->
                            mainImage = correctedImage
                        }
                    }
                }
            }
            squeezebox(multiselect = false) {
                fold("Gamma Correction") {
                    slider {
                        min = 0.1
                        max = 10.0
                        majorTickUnit = 0.5
                        minorTickCount = 5
                        valueProperty().onChange { gammaSliderValueText = String.format("%.2f", value) }
                        sliderValueProperty.bind(valueProperty())
                    }
                    hbox {
                        alignment = Pos.CENTER
                        textfield {
                            maxWidth = 50.0
                            textProperty().bind(gammaSliderValueTextProperty)
                            editableProperty().set(false)
                        }
                        button("Apply") {
                            action {
                                mainImage?.let {
                                    runAsync {
                                        controller.applyLookupTable(
                                            it,
                                            controller.createGammaLookupTable(sliderValue)
                                        )
                                    } ui { correctedImage ->
                                        mainImage = correctedImage
                                    }
                                }
                            }
                        }
                    }
                }
                fold("Contrast Stretching") {
                    linechart(
                        x = NumberAxis(0.0, 260.0, 5.0),
                        y = NumberAxis(0.0, 260.0, 5.0)
                    ) {
                        animated = false
                        isLegendVisible = false
                        data.add(chartSeries)

                        // create the starting and ending points of the chart, add them to the chart
                        val startPoint = XYChart.Data<Number, Number>(0.0, 0.0)
                        val endPoint = XYChart.Data<Number, Number>(255.0, 255.0)
                        chartSeries.data.add(startPoint)
                        chartSeries.data.add(endPoint)

                        // make the end nodes invisible
                        startPoint.node.isVisible = false
                        endPoint.node.isVisible = false

                        onMouseMoved = EventHandler { me ->

                            // translate mouse position to (x,y) chart values
                            val pointInScene = Point2D(me.sceneX, me.sceneY)
                            val xAxisLoc = xAxis.sceneToLocal(pointInScene).x
                            val yAxisLoc = yAxis.sceneToLocal(pointInScene).y
                            val xValue = xAxis.getValueForDisplay(xAxisLoc)
                            val yValue = yAxis.getValueForDisplay(yAxisLoc)

                            // hide the chart coordinates label if mouse goes outside the chart; show it otherwise
                            chartCoordsTextField.isVisible = xValue.toDouble() >= 0.0 && yValue.toDouble() >= 0.0

                            // display the coordinates
                            chartCoordsTextField.text = String.format(
                                "(%.2f ; %.2f)",
                                xValue,
                                yValue
                            )

                        }

                        onMouseClicked = EventHandler { me ->
                            // calculate the number of user-created points on the chart (not including the start/end)
                            val pointsOnChartBeforeClick = chartSeries.data.size - 2

                            // if there are less than 2 points on the chart
                            if (pointsOnChartBeforeClick < 2) {
                                // translate mouse position to (x,y) chart values
                                val pointInScene = Point2D(me.sceneX, me.sceneY)
                                val xAxisLoc = xAxis.sceneToLocal(pointInScene).x
                                val yAxisLoc = yAxis.sceneToLocal(pointInScene).y
                                val xValue = xAxis.getValueForDisplay(xAxisLoc)
                                val yValue = yAxis.getValueForDisplay(yAxisLoc)

                                // create a new point at the mouse location
                                val newData = XYChart.Data<Number, Number>(xValue, yValue)
                                val circle = Circle(5.0, Color.DEEPSKYBLUE)
                                newData.node = circle
                                circle.cursor = Cursor.HAND
                                chartSeries.data.add(newData)

                                // add that point to user created nodes
                                userCreatedPoints.add(newData)

                                circle.onMouseDragged = EventHandler { mouseDragEvent ->
                                    // translate mouse position to (x,y) chart values
                                    val pntInScene = Point2D(mouseDragEvent.sceneX, mouseDragEvent.sceneY)
                                    val xAxLoc = xAxis.sceneToLocal(pntInScene).x
                                    val yAxLoc = yAxis.sceneToLocal(pntInScene).y
                                    val xVal = xAxis.getValueForDisplay(xAxLoc)
                                    val yVal = yAxis.getValueForDisplay(yAxLoc)

                                    // if the user didn't drag the node out of the chart
                                    if (xVal.toDouble() in 0.0..255.0 && yVal.toDouble() in 0.0..255.0) {
                                        // update the dragged node position
                                        newData.xValue = xVal
                                        newData.yValue = yVal
                                    }
                                }
                            }
                        }

                    }
                    hbox {
                        alignment = Pos.CENTER
                        chartCoordsTextField = textfield {
                            alignment = Pos.CENTER
                            maxWidth = 130.0
                            editableProperty().set(false)
                        }
                        button("Apply") {
                            action {
                                mainImage?.let {
                                    var point1 =
                                        Pair(
                                            userCreatedPoints[0].xValue.toDouble(),
                                            userCreatedPoints[0].yValue.toDouble()
                                        )
                                    var point2 =
                                        Pair(
                                            userCreatedPoints[1].xValue.toDouble(),
                                            userCreatedPoints[1].yValue.toDouble()
                                        )

                                    // make sure the points are in order ; swap them if not in the correct order
                                    if (point1.first > point2.first) {
                                        point1 = point2.also { point2 = point1 }
                                    }

                                    runAsync {
                                        controller.applyLookupTable(
                                            it,
                                            controller.createContrastStretchingLookupTable(point1, point2)
                                        )
                                    } ui { correctedImage ->
                                        mainImage = correctedImage
                                    }
                                }
                            }
                        }
                    }
                }
                fold("Cross Correlation") {
                    hbox {
                        combobox(
                            selectedKernelProperty,
                            FXCollections.observableArrayList(enumValues<Kernel>().map(Kernel::name))
                        )
                        button("Apply") {
                            action {
                                mainImage?.let {
                                    runAsync {
                                        controller.applyFilterKernel(it, Kernel.valueOf(selectedKernel))
                                    } ui { correctedImage ->
                                        mainImage = correctedImage
                                    }
                                }
                            }
                        }
                    }
                }
            }
            button("Histogram") {
                maxWidth = Double.MAX_VALUE
                action {
                    mainImage?.let {
                        runAsync {
                            controller.calculateHistogramData(it)
                        } ui { histogramData ->
                            find<HistogramView>(mapOf("histogramData" to histogramData)).openWindow()
                            fire(HistogramOpenEvent())
                        }
                    }
                }
            }
            button("Convert to greyscale") {
                maxWidth = Double.MAX_VALUE
                action {
                    mainImage?.let {
                        runAsync {
                            controller.convertToGreyScale(it)
                        } ui { correctedImage ->
                            mainImage = correctedImage
                        }
                    }
                }
            }
            button("Equalize (use only on greyscale images)") {
                maxWidth = Double.MAX_VALUE
                action {
                    mainImage?.let {
                        runAsync {
                            // calculate histogram data (slightly inefficient since
                            // we don't need to calculate the data for each channel),
                            // calculate the lookup table and apply it
                            controller.applyLookupTable(
                                it,
                                controller.createGreyEqualizationLookupTable(
                                    controller.calculateHistogramData(it),
                                    mainImage.height * mainImage.width
                                )
                            )
                        } ui { correctedImage ->
                            mainImage = correctedImage
                        }
                    }
                }
            }
            button("Equalize RGB") {
                maxWidth = Double.MAX_VALUE
                action {
                    mainImage?.let {
                        runAsync {
                            controller.equalizeHistogram(it)
                        } ui { correctedImage ->
                            mainImage = correctedImage
                        }
                    }
                }
            }
        }
    }

    init {
        importStylesheet("/darktheme.css")
        currentStage?.icons?.add(Image("camera.png"))
        with(root) {
            prefWidth = 1280.0
            prefHeight = 720.0
            currentStage?.isMaximized = true
        }
    }
}