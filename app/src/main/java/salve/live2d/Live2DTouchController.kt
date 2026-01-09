package salve.live2d

import android.view.SurfaceHolder
import kotlin.math.max
import kotlin.math.min

/**
 * Controlador que traduce gestos táctiles en parámetros del modelo.
 *
 * Ejemplo de mapeo:
 * - ParamAngleX / ParamAngleY para inclinación de cabeza.
 * - ParamBodyAngleX para giro suave del torso.
 */
class Live2DTouchController(
    private val parameterSink: Live2DParameterSink
) : Live2DController {

    override fun onSurfaceCreated(holder: SurfaceHolder) {
        parameterSink.onSurfaceReady(holder)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        parameterSink.onResize(width, height)
    }

    override fun onSurfaceDestroyed() {
        parameterSink.onDispose()
    }

    override fun onTouch(x: Float, y: Float) {
        val normalizedX = normalize(x, 0f, parameterSink.viewportWidth.toFloat())
        val normalizedY = normalize(y, 0f, parameterSink.viewportHeight.toFloat())
        parameterSink.setParameter("ParamAngleX", normalizedX * 30f)
        parameterSink.setParameter("ParamAngleY", -normalizedY * 30f)
        parameterSink.setParameter("ParamBodyAngleX", normalizedX * 10f)
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max <= min) return 0f
        val clamped = max(min, min(value, max))
        return ((clamped - min) / (max - min)) * 2f - 1f
    }
}

/**
 * Puente que debe implementar la integración con el SDK Live2D Cubism.
 */
interface Live2DParameterSink {
    val viewportWidth: Int
    val viewportHeight: Int

    fun onSurfaceReady(holder: SurfaceHolder)
    fun onResize(width: Int, height: Int)
    fun onDispose()
    fun setParameter(parameterId: String, value: Float)
}
