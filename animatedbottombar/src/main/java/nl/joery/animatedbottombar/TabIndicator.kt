package nl.joery.animatedbottombar

import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.FloatProperty
import android.util.Property
import androidx.recyclerview.widget.RecyclerView
import nl.joery.animatedbottombar.utils.fixDurationScale

internal class TabIndicator(
    private val bottomBar: AnimatedBottomBar,
    private val parent: RecyclerView,
    private val adapter: TabAdapter
) : RecyclerView.ItemDecoration() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val animator = ObjectAnimator().apply {
        target = this@TabIndicator
        setProperty(CURRENT_LEFT_PROPERTY)
        fixDurationScale()
    }

    private var lastSelectedIndex: Int = RecyclerView.NO_POSITION
    private var currentLeft: Float = 0f

    private val indicatorRect = RectF()

    private val shouldRender: Boolean
        get() = bottomBar.indicatorStyle.indicatorAppearance != AnimatedBottomBar.IndicatorAppearance.INVISIBLE

    init {
        applyStyle()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (adapter.selectedIndex == RecyclerView.NO_POSITION || !shouldRender) {
            return
        }

        val isAnimating = animator.isRunning
        val animatedFraction = animator.animatedFraction
        val lastView = parent.getChildAt(lastSelectedIndex)
        val newView = parent.getChildAt(adapter.selectedIndex) ?: return

        val newViewWidth = if (bottomBar.isVerticalBar)
            newView.height.toFloat()
        else
            newView.width.toFloat()
        val newViewLeft =  if (bottomBar.isVerticalBar)
            newView.top.toFloat()
        else
            newView.left.toFloat()

        var currentWidth = newViewWidth

        when(bottomBar.indicatorAnimation) {
            AnimatedBottomBar.IndicatorAnimation.SLIDE -> {
                if (isAnimating && lastView != null) {
                    val lastViewWidth = if (bottomBar.isVerticalBar)
                        lastView.height.toFloat()
                    else
                        lastView.width.toFloat()
                    currentWidth =
                        lastViewWidth + (newViewWidth - lastViewWidth) * animatedFraction
                } else {
                    currentLeft = newViewLeft
                }

                drawIndicator(c, currentLeft, currentWidth)
            }
            AnimatedBottomBar.IndicatorAnimation.FADE -> {
                if (isAnimating && lastView != null) {
                    val newAlpha = 255f * animatedFraction
                    val lastAlpha = 255f - newAlpha

                    if (bottomBar.isVerticalBar) {
                        drawIndicator(
                            c,
                            lastView.top.toFloat(),
                            lastView.height.toFloat(),
                            lastAlpha.toInt()
                        )
                    } else {
                        drawIndicator(
                            c,
                            lastView.left.toFloat(),
                            lastView.width.toFloat(),
                            lastAlpha.toInt()
                        )
                    }
                    drawIndicator(
                        c,
                        newViewLeft,
                        newViewWidth,
                        newAlpha.toInt()
                    )
                } else {
                    drawIndicator(c, newViewLeft, newViewWidth)
                }
            }
            else -> {
                drawIndicator(c, newViewLeft, newViewWidth)
            }
        }
    }

    private fun drawIndicator(c: Canvas, viewLeft: Float, viewWidth: Float, alpha: Int = 255) {
        val indicatorMargin = bottomBar.indicatorStyle.indicatorMargin
        paint.alpha = alpha

        val indicatorHeight =  bottomBar.indicatorStyle.indicatorHeight.toFloat()

        when(bottomBar.indicatorStyle.indicatorAppearance) {
            AnimatedBottomBar.IndicatorAppearance.SQUARE -> {
                val left: Float
                val top: Float
                val right: Float
                val bottom: Float

                if (bottomBar.isVerticalBar && bottomBar.longEdgeIndicator) {
                    top = viewLeft + indicatorMargin
                    bottom = top + viewWidth - indicatorMargin
                    when (bottomBar.indicatorStyle.indicatorLocation) {
                        AnimatedBottomBar.IndicatorLocation.TOP -> {
                            left = 0f
                            right = indicatorHeight
                        }

                        AnimatedBottomBar.IndicatorLocation.BOTTOM -> {
                            right = parent.width.toFloat()
                            left = right - indicatorHeight
                        }
                    }
                } else if (!bottomBar.isVerticalBar && !bottomBar.longEdgeIndicator) {
                    top = 0f + indicatorMargin
                    bottom = parent.height.toFloat() - indicatorMargin
                    when (bottomBar.indicatorStyle.indicatorLocation) {
                        AnimatedBottomBar.IndicatorLocation.TOP -> {
                            left = viewLeft
                            right = indicatorHeight
                        }

                        AnimatedBottomBar.IndicatorLocation.BOTTOM -> {
                            right = viewLeft + viewWidth
                            left = right - indicatorHeight
                        }
                    }
                } else {
                    if (bottomBar.isVerticalBar) {
                        left = 0f + indicatorMargin
                        right = parent.width.toFloat() - indicatorMargin
                    } else {
                        left = viewLeft + indicatorMargin
                        right = viewLeft + viewWidth - indicatorMargin
                    }
                    when (bottomBar.indicatorStyle.indicatorLocation) {
                        AnimatedBottomBar.IndicatorLocation.TOP -> {
                            top = if (bottomBar.isVerticalBar) {
                                viewLeft
                            } else {
                                0f
                            }
                            bottom = top + indicatorHeight
                        }

                        AnimatedBottomBar.IndicatorLocation.BOTTOM -> {
                            bottom = if (bottomBar.isVerticalBar) {
                                viewLeft + viewWidth
                            } else {
                                parent.height.toFloat()
                            }
                            top = bottom - indicatorHeight
                        }
                    }
                }

                c.drawRect(left, top, right, bottom, paint)
            }
            AnimatedBottomBar.IndicatorAppearance.ROUND -> {
                // Canvas.drawRoundRect draws rectangle with all round corners.
                // To make bottom corners round, we can draw rectangle still with all round corners,
                // but hide top round corners by translating the rectangle to top for radius
                // (rx, ry arguments in Canvas.drawRoundRect).
                // In the same way, we can make top corners round, but we have to translate to bottom

                val left: Float
                val top: Float
                val right: Float
                val bottom: Float

                if (bottomBar.isVerticalBar && bottomBar.longEdgeIndicator) {
                    top = viewLeft
                    bottom = viewLeft + viewWidth
                    when(bottomBar.indicatorStyle.indicatorLocation) {
                        AnimatedBottomBar.IndicatorLocation.TOP -> {
                            left = -indicatorHeight
                            right = indicatorHeight
                        }

                        AnimatedBottomBar.IndicatorLocation.BOTTOM -> {
                            val width = viewLeft + viewWidth
                            left = width - indicatorHeight
                            right = width + indicatorHeight
                        }
                    }
                } else if (!bottomBar.isVerticalBar && !bottomBar.longEdgeIndicator) {
                    top = 0f
                    bottom = parent.height.toFloat()
                    when(bottomBar.indicatorStyle.indicatorLocation) {
                        AnimatedBottomBar.IndicatorLocation.TOP -> {
                            left = -indicatorHeight
                            right = indicatorHeight
                        }

                        AnimatedBottomBar.IndicatorLocation.BOTTOM -> {
                            val width = viewLeft + viewWidth
                            left = width - indicatorHeight
                            right = width + indicatorHeight
                        }
                    }
                } else {
                    if (bottomBar.isVerticalBar) {
                        left = 0f
                        right = parent.width.toFloat()
                    } else {
                        left = viewLeft + indicatorMargin
                        right = viewLeft + viewWidth - indicatorMargin
                    }
                    when (bottomBar.indicatorStyle.indicatorLocation) {
                        AnimatedBottomBar.IndicatorLocation.TOP -> {
                            top = -indicatorHeight
                            bottom = indicatorHeight
                        }

                        AnimatedBottomBar.IndicatorLocation.BOTTOM -> {
                            val parentHeight = if (bottomBar.isVerticalBar)
                                viewLeft + viewWidth
                            else
                                parent.height.toFloat()
                            top = parentHeight - indicatorHeight
                            bottom = parentHeight + indicatorHeight
                        }
                    }
                }

                // The reason of using RectF is that Canvas.drawRoundRect(RectF, float, float) is available
                // only since API 21
                indicatorRect.set(left, top, right, bottom)

                c.drawRoundRect(indicatorRect, indicatorHeight, indicatorHeight, paint)
            }
            else -> { }
        }
    }

    fun setSelectedIndex(lastIndex: Int, newIndex: Int, animate: Boolean) {
        if (animator.isRunning) {
            animator.cancel()
        }

        if (!shouldRender) {
            return
        }

        val newView = parent.getChildAt(newIndex)
        if (!animate || lastIndex == -1 || newView == null) {
            parent.invalidate()
            return
        }

        lastSelectedIndex = lastIndex

        animator.run {
            if (bottomBar.isVerticalBar)
                setFloatValues(currentLeft, newView.top.toFloat())
            else
                setFloatValues(currentLeft, newView.left.toFloat())
            duration = bottomBar.tabStyle.animationDuration.toLong()
            interpolator = bottomBar.tabStyle.animationInterpolator

            start()
        }
    }

    fun applyStyle() {
        paint.color = bottomBar.indicatorStyle.indicatorColor

        if (shouldRender) {
            parent.invalidate()
        }
    }

    companion object {
        private val CURRENT_LEFT_PROPERTY = if(Build.VERSION.SDK_INT >= 24) {
            object: FloatProperty<TabIndicator>("currentLeft") {
                override fun get(o: TabIndicator): Float = o.currentLeft
                override fun setValue(o: TabIndicator, value: Float) {
                    o.currentLeft = value
                    o.parent.invalidate()
                }
            }
        } else {
            object: Property<TabIndicator, Float>(Float::class.java, "currentLeft") {
                override fun get(o: TabIndicator): Float = o.currentLeft
                override fun set(o: TabIndicator, value: Float) {
                    o.currentLeft = value
                    o.parent.invalidate()
                }
            }
        }
    }
}