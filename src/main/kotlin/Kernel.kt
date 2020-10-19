/**
 * A simple enum which holds possible filter matrices.
 *
 * @author Krzysztof Bielikowicz 963560
 * This is my own work.
 */
enum class Kernel(val matrix: Array<IntArray>) {
    LAPLACIAN(
        arrayOf(
            intArrayOf(-4, -1, 0, -1, -4),
            intArrayOf(-1, 2, 3, 2, -1),
            intArrayOf(0, 3, 4, 3, 0),
            intArrayOf(-1, 2, 3, 2, -1),
            intArrayOf(-4, -1, 0, -1, -4)
        )
    ),
    HIGH_PASS(
        arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, 8, -1),
            intArrayOf(-1, -1, -1)
        )
    ),
    EMBOSS(
        arrayOf(
            intArrayOf(-1, -1, -1),
            intArrayOf(-1, 1, 1),
            intArrayOf(-1, 1, 1)
        )
    ),
    SOBEL_X(
        arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
    ),
    SOBEL_Y(
        arrayOf(
            intArrayOf(1, 2, 1),
            intArrayOf(0, 0, 0),
            intArrayOf(-1, -2, -1)
        )
    ),
    LOW_PASS(
        arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1)
        )
    )
}