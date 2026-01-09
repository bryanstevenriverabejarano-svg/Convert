package salve.live2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Vista de ejemplo para renderizar un modelo Live2D si el SDK está presente.
 *
 * Este componente no depende directamente del SDK para mantener el proyecto compilable.
 * La integración real debe inyectar un [Live2DController] que invoque al runtime.
 */
class Live2DCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
    }

    var controller: Live2DController? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderPlaceholder()
        controller?.onSurfaceCreated(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        controller?.onSurfaceDestroyed()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        controller?.onSurfaceChanged(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            controller?.onTouch(event.x, event.y)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun renderPlaceholder() {
        val canvas: Canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.BLACK)
        canvas.drawText("Live2D listo para cargar", 40f, 80f, paint)
        holder.unlockCanvasAndPost(canvas)
    }
}

/**
 * API mínima para conectar el SDK Live2D sin agregar dependencias directas.
 */
interface Live2DController {
    fun onSurfaceCreated(holder: SurfaceHolder)
    fun onSurfaceChanged(width: Int, height: Int)
    fun onSurfaceDestroyed()
    fun onTouch(x: Float, y: Float)
}
