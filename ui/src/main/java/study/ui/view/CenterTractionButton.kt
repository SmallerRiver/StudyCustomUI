package study.ui.view

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatRadioButton
import study.ui.R
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 仿旧版本的QQTab按钮 没有加文字 需要加文字只需将y减去字体高度
 * 重点是onTouchEvent逻辑 图片看assets文件夹中的20171125115158077.jpg
 * 参考(不要脸的说了 其实就是仿着这个写的)了https://blog.csdn.net/qq_22770457/article/details/78630695
 */
class CenterTractionButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {
    private var mAttracted: Boolean = false

    /**
     * 外部背景资源ID
     */
    private var mNormalExternalBackgroundRes: Int = 0

    /**
     * 内部图片资源ID
     */
    private var mNormalInsideDrawableRes: Int = 0

    /**
     * 选中状态下的外部背景资源ID
     */
    private var mSelectedExternalBackgroundRes: Int = 0

    /**
     * 选中状态下的内部图片资源ID
     */
    private var mSelectedInsideDrawableRes: Int = 0

    /**
     * 外部资源的背景drawable图片
     */
    private var mExternalBackgroundDrawable: BitmapDrawable? = null

    /**
     * 内部图片的drawable图片
     */
    private var mInsideBackgroundDrawable: BitmapDrawable? = null

    /**
     * 画笔
     */
    private var mBmPaint: Paint? = null

    /**
     * 组件宽
     */
    private var mWidth: Float = 0f

    /**
     * 组件高
     */
    private var mHeight: Float = 0f

    /**
     * 中心点坐标X,相较于屏幕
     */
    private var mScreenCenterX: Float = 0f

    /**
     * 中心点坐标Y,相较于屏幕
     */
    private var mScreenCenterY: Float = 0f

    /**
     * 中心点坐标X,相比较于组件
     */
    private var mComponentCenterX: Float = 0f

    /**
     * 中心点坐标Y,相比较于组件
     */
    private var mComponentCenterY: Float = 0f

    /**
     * 图形偏移距离
     */
    private var mOffsetDistanceLimit: Float = 0.toFloat()

    /**
     * inside图片能动的轨迹圆 半径
     */
    private var mRadius: Float = 0f

    /**
     * 背景图图形的半径
     */
    private var mBackgroundRadius: Float = 0f

    //
    private var mExternalSrcRect: Rect? = null
    private var mExternalDestRect: Rect? = null
    private var mInsideSrcRect: Rect? = null
    private var mInsideDestRect: Rect? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CenterTractionButton)
        mNormalExternalBackgroundRes =
            ta.getResourceId(R.styleable.CenterTractionButton_normalExternalBackgroundRes, 0)
        mNormalInsideDrawableRes =
            ta.getResourceId(R.styleable.CenterTractionButton_normalInsideDrawableRes, 0)
        mSelectedExternalBackgroundRes =
            ta.getResourceId(R.styleable.CenterTractionButton_selectedExternalBackgroundRes, 0)
        mSelectedInsideDrawableRes =
            ta.getResourceId(R.styleable.CenterTractionButton_selectedInsideDrawableRes, 0)
        ta.recycle()

        isClickable=true
        init()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mWidth = measuredWidth.toFloat()
        mHeight = measuredHeight.toFloat()

        mOffsetDistanceLimit = mWidth / 6

        mScreenCenterX = ((getRight() + getLeft()) / 2).toFloat()
        mScreenCenterY = ((getBottom() + getTop()) / 2).toFloat()
        mComponentCenterX = mWidth / 2
        mComponentCenterY = mHeight / 2
        init()
    }

    private fun init() {
        initPaint()

        //得到组件宽高中的较小值,再/2得到ob的距离
        mRadius = if (mHeight > mWidth) mHeight / 2 else mWidth / 2
        mBackgroundRadius = mRadius / 2


        // 背景图绘制区域
        mExternalDestRect =
            Rect(
                (mComponentCenterX - mBackgroundRadius).toInt(),
                (mComponentCenterY - mBackgroundRadius).toInt(),
                (mComponentCenterY + mBackgroundRadius).toInt(),
                (mComponentCenterY + mBackgroundRadius).toInt()
            )

        // 中心图绘制区域
        mInsideDestRect =
            Rect(
                (mComponentCenterX - mBackgroundRadius).toInt(),
                (mComponentCenterY - mBackgroundRadius).toInt(),
                (mComponentCenterY + mBackgroundRadius).toInt(),
                (mComponentCenterY + mBackgroundRadius).toInt()
            )

        mExternalBackgroundDrawable = resources.getDrawable(
            mNormalExternalBackgroundRes,
            context.theme
        ) as BitmapDrawable

        mExternalSrcRect = Rect(
            0,
            0,
            mExternalBackgroundDrawable?.intrinsicWidth ?: 0,
            mExternalBackgroundDrawable?.intrinsicHeight ?: 0
        )

        mInsideBackgroundDrawable = resources.getDrawable(
            mNormalInsideDrawableRes,
            context.theme
        ) as BitmapDrawable

        mInsideSrcRect = Rect(
            0,
            0,
            mInsideBackgroundDrawable?.intrinsicWidth ?: 0,
            mInsideBackgroundDrawable?.intrinsicHeight ?: 0
        )


        setOnCheckedChangeListener { _, bChecked ->
            if (bChecked) {
                mExternalBackgroundDrawable = resources.getDrawable(
                    mSelectedExternalBackgroundRes,
                    context.theme
                ) as BitmapDrawable
                mInsideBackgroundDrawable = resources.getDrawable(
                    mSelectedInsideDrawableRes,
                    context.theme
                ) as BitmapDrawable

                val pvhX = PropertyValuesHolder.ofFloat(
                    "scaleX", 0.1f,
                    1f
                )
                val pvhY = PropertyValuesHolder.ofFloat(
                    "scaleY", 0.1f,
                    1f
                )

                val objectAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhX, pvhY)
                objectAnimator.duration = 500
                val overshootInterpolator = OvershootInterpolator(1.2f)
                objectAnimator.interpolator = overshootInterpolator
                objectAnimator.start()

                postInvalidate()
            } else {
                mExternalBackgroundDrawable = resources.getDrawable(
                    mNormalExternalBackgroundRes,
                    context.theme
                ) as BitmapDrawable
                mInsideBackgroundDrawable = resources.getDrawable(
                    mNormalInsideDrawableRes,
                    context.theme
                ) as BitmapDrawable

                postInvalidate()
            }
        }
    }

    /**
     * 初始化画笔
     */
    private fun initPaint() {
        if (mBmPaint == null) {
            //绘制图形的画笔 打开抗锯齿
            mBmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            ////设置填充样式   Style.FILL  Style.FILL_AND_STROKE  Style.STROKE
            mBmPaint!!.style = Paint.Style.FILL
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //绘制默认状态下背景图
        val externalBM = mExternalBackgroundDrawable!!.bitmap
        mExternalDestRect?.let { canvas.drawBitmap(externalBM, mExternalSrcRect, it, mBmPaint) }


        //绘制默认状态下中心图
        val insideBM = mInsideBackgroundDrawable!!.bitmap
        mInsideDestRect?.let { canvas.drawBitmap(insideBM, mInsideSrcRect, it, mBmPaint) }
    }

    private fun startAttract() {
        mAttracted = true
    }

    private fun releaseAttract() {
        mAttracted = false
    }

    //重点
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (mAttracted)
            parent.requestDisallowInterceptTouchEvent(true)

        //相较于视图的XY
        var touchPointExternalX = event.x
        var touchPointExternalY = event.y

        var touchPointInsideX = event.x
        var touchPointInsideY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //按下后禁止父类调用
                startAttract()
                postInvalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                //判断点击位置距离中心的距离
                val distance2Center =
                    getDistanceTwoPoint(
                        touchPointExternalX,
                        touchPointExternalY,
                        mComponentCenterX,
                        mComponentCenterY
                    )

                //外圈能动的距离
                val mExternalOffsetLimit = mBackgroundRadius / 4
                //内圈能动的距离
                val mInsideOffsetLimit = mBackgroundRadius / 2

                //如果区域在轨迹圆内则移动
                if (distance2Center > mExternalOffsetLimit) {
                    //如果点击位置在组件外，则获取点击位置和中心点连线上的一点(该点满足矩形在组件内)为中心作图
                        // oc/oa = od/ob
                    var od = touchPointExternalX - mComponentCenterY
                    var ob = getDistanceTwoPoint(
                        mComponentCenterX,
                        mComponentCenterY,
                        touchPointExternalX,
                        touchPointExternalY
                    )
                    var oc = od / ob * mExternalOffsetLimit
                    // ca/oa = db/ob
                    var db = mComponentCenterY - touchPointExternalY
                    var ac = db / ob * mExternalOffsetLimit
                    //得到ac和oc判断得出a点的位置
                    touchPointExternalX = mComponentCenterX + oc
                    touchPointExternalY = mComponentCenterY - ac

                    od = touchPointInsideX - mComponentCenterX
                    ob = getDistanceTwoPoint(
                        mComponentCenterX,
                        mComponentCenterY,
                        touchPointInsideX,
                        touchPointInsideY
                    )
                    oc = od / ob * mInsideOffsetLimit
                    // ca/oa = db/ob
                    db = mComponentCenterY - touchPointInsideY
                    ac = db / ob * mInsideOffsetLimit
                    //得到ac和oc判断得出a点的位置
                    touchPointInsideX = mComponentCenterY + oc
                    touchPointInsideY = mComponentCenterY - ac
                } else {
                    //获得与中点的距离，*2,如图3
                    val ab = touchPointExternalY - mComponentCenterY
                    val bo = touchPointExternalX - mComponentCenterX
                    touchPointInsideX = mComponentCenterX + 2f * bo
                    touchPointInsideY = mComponentCenterY + 2f * ab
                    if (distance2Center > mExternalOffsetLimit) {
                        return super.onTouchEvent(event)
                    }
                }

                var left: Int = (touchPointExternalX - mBackgroundRadius).toInt()
                var right: Int = (touchPointExternalX + mBackgroundRadius).toInt()
                var top: Int = (touchPointExternalY - mBackgroundRadius).toInt()
                var bottom: Int = (touchPointExternalY + mBackgroundRadius).toInt()
                //更新背景图绘制区域
                mExternalDestRect = Rect(left, top, right, bottom)

                left = (touchPointInsideX - mBackgroundRadius).toInt()
                right = (touchPointInsideX + mBackgroundRadius).toInt()
                top = (touchPointInsideY - mBackgroundRadius).toInt()
                bottom = (touchPointInsideY + mBackgroundRadius).toInt()
                //更新中心图绘制区域
                mInsideDestRect = Rect(left, top, right, bottom)
                postInvalidate()
            }
            MotionEvent.ACTION_UP -> {
                //复原背景图绘制区域
                mExternalDestRect = Rect(
                    (mComponentCenterX - mBackgroundRadius).toInt(),
                    (mComponentCenterY - mBackgroundRadius).toInt(),
                    (mComponentCenterX + mBackgroundRadius).toInt(),
                    (mComponentCenterY + mBackgroundRadius).toInt()
                )
                //复原中心图绘制区域
                mInsideDestRect = Rect(
                    (mComponentCenterX - mBackgroundRadius).toInt(),
                    (mComponentCenterY - mBackgroundRadius).toInt(),
                    (mComponentCenterX + mBackgroundRadius).toInt(),
                    (mComponentCenterY + mBackgroundRadius).toInt()
                )
                postInvalidate()
                releaseAttract()
            }
        }

        return super.onTouchEvent(event)
    }

    //计算两点之间的距离
    private fun getDistanceTwoPoint(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt(
            ((x1 - x2).toDouble().pow(2.toDouble()) + (y1 - y2).toDouble().pow(2.toDouble()))
        ).toFloat()
    }
}