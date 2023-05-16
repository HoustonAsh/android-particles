package com.houstonash.particle

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import kotlin.math.min
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = R.attr.ParticleViewStyle
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val particles = mutableListOf<Particle>()
    private var surfaceViewThread: SurfaceViewThread? = null
    private var hasSurface: Boolean = false
    private var hasSetup = false
    private var tag = "[PARTICLES]"

    private var _particleCount = 20

    @Dimension
    private var _particleMinRadius = 10

    @Dimension
    private var _particleMaxRadius = 25

    @Dimension
    private var _particleVelocity = 1.5f

    @Dimension
    private var _particleLineWidth = 15f

    private var _particleLineMinAlpha = 0

    private var _particleLineMaxAlpha = 255

    private var _dtLineAlpha = -1

    private var _particleLineHalfLength = -1

    @Dimension
    private var _particleLineMaxLength = 220

    @ColorInt
    private var _particlesBackgroundColor = Color.BLACK

    @ColorInt
    private var _particleColor = Color.WHITE

    @ColorInt
    private var _particleLineColor = Color.WHITE

    private var _particleLinesEnabled = true

    private var _particleRotationEnabled = false

    // Core Attributes
    var particleCount: Int
        get() = _particleCount
        set(value) {
            _particleCount = when {
                value > 50 -> 50
                value < 0 -> 0
                else -> value
            }

            Log.d(tag, "particleCount set: $_particleCount")
        }

    private var particleMinRadius: Int
        @Dimension get() = _particleMinRadius
        set(@Dimension value) {
            _particleMinRadius = when {
                value <= 0 -> 1
                value >= particleMaxRadius -> 1
                else -> value
            }

            Log.d(tag, "particleMinRadius set: $_particleMinRadius")
        }

    private var particleMaxRadius: Int
        @Dimension get() = _particleMaxRadius
        set(@Dimension value) {
            _particleMaxRadius = when {
                value <= particleMinRadius -> particleMinRadius + 1
                else -> value
            }

            Log.d(tag, "particleMaxRadius set: $_particleMaxRadius")
        }

    private var particleVelocity: Float
        @Dimension get() = _particleVelocity
        set(@Dimension value) {
            _particleVelocity = when {
                value <= 0 -> 1f
                else -> value
            }

            Log.d(tag, "particleVelocity set: $_particleVelocity")
        }
    private var particleLineWidth: Float
        @Dimension get() = _particleLineWidth
        set(@Dimension value) {
            _particleLineWidth = when {
                value < 0 -> 0f
                else -> value
            }
            Log.d(tag, "particleLineWidth set: $_particleLineWidth")
        }

    private var particleLineMinAlpha: Int
        get() = _particleLineMinAlpha
        set(value) {
            _particleLineMinAlpha = when {
                value < 0 -> 0
                value >= _particleLineMaxAlpha -> 1
                value > 255 -> 255
                else -> value
            }

            Log.d(tag, "particleLineMinAlpha set: $_particleLineMinAlpha")
        }

    private var particleLineMaxAlpha: Int
        get() = _particleLineMaxAlpha
        set(value) {
            _particleLineMaxAlpha = when {
                value < particleLineMinAlpha -> particleLineMinAlpha
                value > 255 -> 255
                else -> value
            }

            Log.d(tag, "particleLineMaxAlpha set: $_particleLineMaxAlpha")
        }

    private var particleLineMaxLength: Int
        @Dimension get() = _particleLineMaxLength
        set(@Dimension value) {
            _particleLineMaxLength = when {
                value <= 0 -> 0
                else -> value
            }
            Log.d(tag, "particleLineMaxLength set: $_particleLineMaxLength")
        }

    private var particlesBackgroundColor: Int
        @ColorInt get() = _particlesBackgroundColor
        set(@ColorInt value) {
            _particlesBackgroundColor = value

            Log.d(tag, "particlesBackgroundColor set: $particlesBackgroundColor")
        }

    private var particleColor: Int
        @ColorInt get() = _particleColor
        set(@ColorInt value) {
            _particleColor = value
            paintParticles.color = value

            Log.d(tag, "particleColor set: $_particleColor")
        }

    private var particleLineColor: Int
        @ColorInt get() = _particleLineColor
        set(@ColorInt value) {
            _particleLineColor = value
            paintLines.color = value
            Log.d(tag, "particleLineColor set: $_particleLineColor")
        }

    var particleLinesEnabled: Boolean
        get() = _particleLinesEnabled
        set(value) {
            _particleLinesEnabled = value
            Log.d(tag, "particleLinesEnabled set: $_particleLinesEnabled")
        }

    var particleRotationEnabled: Boolean
        get() = _particleRotationEnabled
        set(value) {
            _particleRotationEnabled = value
            Log.d(tag, "particleRotationEnabled set: $_particleRotationEnabled")
        }
    // Paints
    private val paintParticles: Paint = Paint().apply {
        style = Paint.Style.FILL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            blendMode = BlendMode.SRC_OVER
        else
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    private val paintParticlesErase: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = _particlesBackgroundColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            blendMode = BlendMode.SRC
        else
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

    private val paintLines: Paint = Paint().apply {
        style = Paint.Style.STROKE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            blendMode = BlendMode.SRC_OVER
        else
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    init {
        obtainStyledAttributes(attrs, defStyleAttr)
        if (holder != null) holder.addCallback(this)
        hasSurface = false
    }

    private fun obtainStyledAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.ParticleView,
            defStyleAttr,
            0
        )

        try {
            particleCount = typedArray.getInt(
                R.styleable.ParticleView_particleCount,
                particleCount
            )

            particleMinRadius = typedArray.getInt(
                R.styleable.ParticleView_particleMinRadius,
                particleMinRadius
            )

            particleMaxRadius = typedArray.getInt(
                R.styleable.ParticleView_particleMaxRadius,
                particleMaxRadius
            )

            particleVelocity = typedArray.getFloat(
                R.styleable.ParticleView_particleVelocity,
                particleVelocity
            )

            particleLineWidth = typedArray.getFloat(
                R.styleable.ParticleView_particleLineWidth,
                particleLineWidth
            )

            particleLineMinAlpha = typedArray.getInt(
                R.styleable.ParticleView_particleLineMinAlpha,
                particleLineMinAlpha
            )

            particleLineMaxAlpha = typedArray.getInt(
                R.styleable.ParticleView_particleLineMaxAlpha,
                particleLineMaxAlpha
            )

            particleLineMaxLength = typedArray.getInt(
                R.styleable.ParticleView_particleLineMaxLength,
                particleLineMaxLength
            )

            particlesBackgroundColor = typedArray.getColor(
                R.styleable.ParticleView_particlesBackgroundColor,
                particlesBackgroundColor
            )

            particleColor = typedArray.getColor(
                R.styleable.ParticleView_particleColor,
                particleColor
            )

            particleLineColor = typedArray.getColor(
                R.styleable.ParticleView_particleLineColor,
                particleLineColor
            )

            particleLinesEnabled = typedArray.getBoolean(
                R.styleable.ParticleView_particleLinesEnabled,
                particleLinesEnabled
            )

            particleRotationEnabled = typedArray.getBoolean(
                R.styleable.ParticleView_particleRotationEnabled,
                particleRotationEnabled
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        hasSurface = true

        if (surfaceViewThread == null) {
            surfaceViewThread = SurfaceViewThread()
        }

        surfaceViewThread?.start()
    }

    fun resume() {
        if (surfaceViewThread == null) {
            surfaceViewThread = SurfaceViewThread()

            if (hasSurface) {
                surfaceViewThread?.start()
            }
        }
    }

    fun pause() {
        surfaceViewThread?.requestExitAndWait()
        surfaceViewThread = null
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false
        surfaceViewThread?.requestExitAndWait()
        surfaceViewThread = null
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // ignored
    }

    private fun setupParticles() {
        if (!hasSetup) {
            particleLineMaxLength *= particleLineMaxLength
            _particleLineHalfLength = _particleLineMaxLength / 16
            hasSetup = true
            particles.clear()
            for (i in 0 until particleCount) {
                particles.add(
                    Particle(
                        Random.nextInt(particleMinRadius, particleMaxRadius).toFloat(),
                        Random.nextInt(0, width).toFloat(),
                        Random.nextInt(0, height).toFloat(),
                        ((Random.nextFloat() - 0.5) * particleVelocity).toFloat(),
                        ((Random.nextFloat() - 0.5) * particleVelocity).toFloat(),
                        Random.nextInt(180, 255)
                    )
                )
            }
        }
    }

    private inner class SurfaceViewThread : Thread() {

        private var running = true
        private var canvas: Canvas? = null

        override fun run() {
            setupParticles()
            _dtLineAlpha = particleLineMaxAlpha - particleLineMinAlpha
            var prevTime = System.nanoTime()
            val canvasWidth = width.toFloat()
            val canvasHeight = height.toFloat()

            while (running) {
                val dtTime = (System.nanoTime() - prevTime)/10_000_000f
                prevTime = System.nanoTime()
                try {
                    canvas = holder.lockCanvas()

                    synchronized (holder) {
                        // Clear screen every frame
                        canvas?.drawColor(particlesBackgroundColor, PorterDuff.Mode.SRC)

                        for (i in 0 until particleCount) {
                            particles[i].x += particles[i].vx * dtTime
                            particles[i].y += particles[i].vy * dtTime

                            if (particles[i].x < - particles[i].radius)
                                particles[i].x = canvasWidth
                            else if (particles[i].x > canvasWidth + particles[i].radius)
                                particles[i].x = 0F


                            if (particles[i].y < - particles[i].radius)
                                particles[i].y = canvasHeight
                            else if (particles[i].y > canvasHeight + particles[i].radius)
                                particles[i].y = 0F


                            canvas?.let {
                                if (particleLinesEnabled)
                                    for (j in i + 1 until particleCount)
                                        linkParticles(it, particles[i], particles[j])
                            }

                            paintParticles.alpha = particles[i].alpha
                            canvas?.drawCircle(particles[i].x, particles[i].y, particles[i].radius, paintParticlesErase)
                            canvas?.drawCircle(particles[i].x, particles[i].y, particles[i].radius, paintParticles)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (canvas != null)
                        holder.unlockCanvasAndPost(canvas)
                }
                yield()
            }
        }

        fun requestExitAndWait() {
            running = false

            try {
                join()
            } catch (e: InterruptedException) {
                // ignored
            }
        }
    }

    private var dx: Float = 0f
    private var dy: Float = 0f
    private var dist: Float = 0f
    private var distRatio: Float = 0f

    private fun linkParticles(canvas: Canvas, p1: Particle, p2: Particle): Float {
        dx = p1.x - p2.x
        dy = p1.y - p2.y
        dist = dx * dx + dy * dy

        if (dist < particleLineMaxLength) {
            distRatio = (particleLineMaxLength - dist) / particleLineMaxLength

            paintLines.alpha = (particleLineMinAlpha + _dtLineAlpha * distRatio).toInt().coerceAtMost(min(p1.alpha, p2.alpha))
            paintLines.strokeWidth = min(particleLineWidth, min(p1.radius, p2.radius)) * (if (distRatio < 0.65)  distRatio + 0.2f else 1f)
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintLines)

            if (particleRotationEnabled && dist < _particleLineHalfLength) {
                // cos(1 degree) = 0.9998476
                // sin(1 degree) = 0.0174524
                var newX = p1.vx * 0.9998476f - p1.vy * 0.0174524f
                var newY = p1.vx * 0.0174524f - p1.vy * 0.9998476f
                p1.vx = newX
                p1.vy = newY

                newX = p2.vx * 0.9998476f - p2.vy * 0.0174524f
                newY = p2.vx * 0.0174524f - p2.vy * 0.9998476f
                p2.vx = newX
                p2.vy = newY
            }
        }

        return dist
    }
}