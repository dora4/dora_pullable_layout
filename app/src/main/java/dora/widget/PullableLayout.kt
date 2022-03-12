package dora.widget

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import java.util.*
import kotlin.math.abs
import kotlin.math.tan

class PullableLayout  @JvmOverloads constructor(context: Context, attrs: AttributeSet?,
                                                defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    private var state = STATE_INIT
    private var onRefreshListener: OnRefreshListener? = null
    private var downY = 0f
    private var lastY = 0f
    private var touchSlop = 0f
    private var deltaY = 0f
    private var pullDownY = 0f
    private var pullUpY = 0f
    private var refreshDist = 200f
    private var loadMoreDist = 200f
    private var layout = false
    private var touch = false
    private var ratio = 2f
    private lateinit var rotateAnimation: RotateAnimation
    private lateinit var refreshingAnimation: RotateAnimation
    private lateinit var refreshView: View
    private lateinit var refreshImageView: ImageView
    private lateinit var refreshTextView: TextView
    private lateinit var loadMoreView: View
    private lateinit var loadMoreImageView: ImageView
    private lateinit var loadMoreTextView: TextView
    private lateinit var pullableView: View
    private var pointerEvent = 0
    private var canPullDown = true
    private var canPullUp = true
    private var refreshHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            deltaY = (8 + 5 * tan(
                (Math.PI / 2
                        / measuredHeight) * (pullDownY + Math.abs(pullUpY))
            )).toFloat()
            if (!touch) {
                if (state == STATE_REFRESHING && pullDownY <= refreshDist) {
                    pullDownY = refreshDist
                    timer.cancel()
                } else if (state == STATE_LOADING && -pullUpY <= loadMoreDist) {
                    pullUpY = -loadMoreDist
                    timer.cancel()
                }
            }
            if (pullDownY > 0) {
                pullDownY -= deltaY
            } else if (pullUpY < 0) {
                pullUpY += deltaY
            }
            if (pullDownY < 0) {
                pullDownY = 0f
                if (state != STATE_REFRESHING && state != STATE_LOADING) {
                    changeState(STATE_INIT)
                }
                timer.cancel()
                requestLayout()
            }
            if (pullUpY > 0) {
                pullUpY = 0f
                if (state != STATE_REFRESHING && state != STATE_LOADING) {
                    changeState(STATE_INIT)
                }
                timer.cancel()
                requestLayout()
            }
            requestLayout()
            if (pullDownY + Math.abs(pullUpY) == 0f) {
                timer.cancel()
            }
        }
    }
    private var timer: RefreshTimer = RefreshTimer(refreshHandler)

    fun setOnRefreshListener(l: OnRefreshListener) {
        onRefreshListener = l
    }

    private fun initView(context: Context) {
        if (isInEditMode) {
            return
        }
        timer = RefreshTimer(refreshHandler)
        rotateAnimation = AnimationUtils.loadAnimation(
            context, R.anim.anim_reverse
        ) as RotateAnimation
        refreshingAnimation = AnimationUtils.loadAnimation(
            context, R.anim.anim_rotating
        ) as RotateAnimation
        val interpolator = LinearInterpolator()
        rotateAnimation.interpolator = interpolator
        refreshingAnimation.interpolator = interpolator
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    }

    private fun hide() {
        timer.schedule(5)
    }

    fun refreshFinish(refreshResult: Int) {
        refreshImageView.clearAnimation()
        when (refreshResult) {
            SUCCEED -> {
                refreshTextView.text = "刷新成功"
                refreshImageView.setImageResource(R.drawable.pullable_layout_refresh_succeed)
            }
            FAIL -> {
                refreshTextView.text = "刷新失败"
                refreshImageView.setImageResource(R.drawable.pullable_layout_refresh_failed)
            }
            else -> {
                refreshTextView.text = "刷新失败"
                refreshImageView.setImageResource(R.drawable.pullable_layout_refresh_failed)
            }
        }
        if (pullDownY > 0) {
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    changeState(STATE_DONE)
                    hide()
                }
            }.sendEmptyMessageDelayed(0, 1000)
        } else {
            changeState(STATE_DONE)
            hide()
        }
    }

    fun loadMoreFinish(refreshResult: Int) {
        loadMoreImageView.clearAnimation()
        when (refreshResult) {
            SUCCEED -> {
                loadMoreTextView.text = "加载成功"
                loadMoreImageView.setImageResource(R.drawable.pullable_layout_load_succeed)
            }
            FAIL -> {
                loadMoreTextView.text = "加载失败"
                loadMoreImageView.setImageResource(R.drawable.pullable_layout_load_failed)
            }
            else -> {
                loadMoreTextView.text = "加载失败"
                loadMoreImageView.setImageResource(R.drawable.pullable_layout_load_failed)
            }
        }
        if (pullUpY < 0) {
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    changeState(STATE_DONE)
                    hide()
                }
            }.sendEmptyMessageDelayed(0, 1000)
        } else {
            changeState(STATE_DONE)
            hide()
        }
    }

    private fun changeState(state: Int) {
        this.state = state
        when (state) {
            STATE_INIT -> {
                refreshImageView.setImageResource(R.drawable.pullable_layout_logo)
                refreshTextView.text = "下拉刷新"
                loadMoreImageView.setImageResource(R.drawable.pullable_layout_logo)
                loadMoreTextView.text = "上拉加载"
            }
            STATE_RELEASE_TO_REFRESH -> refreshTextView.text = "释放刷新"
            STATE_REFRESHING -> {
                refreshTextView.text = "刷新中"
                refreshImageView.setImageResource(R.drawable.pullable_layout_refreshing)
                refreshImageView.startAnimation(refreshingAnimation)
            }
            STATE_RELEASE_TO_LOAD -> loadMoreTextView.text = "释放加载"
            STATE_LOADING -> {
                loadMoreTextView.text = "加载中"
                loadMoreImageView.setImageResource(R.drawable.pullable_layout_loading)
                loadMoreImageView.startAnimation(refreshingAnimation)
            }
            STATE_DONE -> {
                refreshingAnimation.cancel()
            }
        }
    }

    private fun releasePull() {
        canPullDown = true
        canPullUp = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.y
                lastY = downY
                timer.cancel()
                pointerEvent = 0
                releasePull()
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> pointerEvent = -1
            MotionEvent.ACTION_MOVE -> {
                if (pointerEvent == 0) {
                    if (pullDownY > 0
                            || ((pullableView as Pullable).canPullDown()
                                    && canPullDown && state != STATE_LOADING)
                    ) {
                        pullDownY += (ev.y - lastY) / ratio
                        if (pullDownY < 0) {
                            pullDownY = 0f
                            canPullDown = false
                            canPullUp = true
                        }
                        if (pullDownY > measuredHeight) {
                            pullDownY = measuredHeight.toFloat()
                        }
                        if (state == STATE_REFRESHING) {
                            touch = true
                        }
                    } else if (pullUpY < 0
                            || (pullableView as Pullable).canPullUp() && canPullUp && state != STATE_REFRESHING
                    ) {
                        pullUpY += (ev.y - lastY) / ratio
                        pullUpY = pullUpY + ev.y - lastY
                        if (pullUpY > 0) {
                            pullUpY = 0f
                            canPullDown = true
                            canPullUp = false
                        }
                        if (pullUpY < -measuredHeight) {
                            pullUpY = -measuredHeight.toFloat()
                        }
                        if (state == STATE_LOADING) {
                            touch = true
                        }
                    } else {
                        releasePull()
                    }
                } else {
                    pointerEvent = 0
                }
                lastY = ev.y
                ratio = (2 + 2 * tan(
                        Math.PI / 2 / measuredHeight
                                * (pullDownY + Math.abs(pullUpY))
                )).toFloat()
                if (pullDownY > 0 || pullUpY < 0) {
                    requestLayout()
                }
                if (pullDownY > 0) {
                    if (pullDownY <= refreshDist
                            && (state == STATE_RELEASE_TO_REFRESH || state == STATE_DONE)
                    ) {
                        changeState(STATE_INIT)
                    }
                    if (pullDownY >= refreshDist && state == STATE_INIT) {
                        changeState(STATE_RELEASE_TO_REFRESH)
                    }
                } else if (pullUpY < 0) {
                    if (-pullUpY <= loadMoreDist
                            && (state == STATE_RELEASE_TO_LOAD || state == STATE_DONE)
                    ) {
                        changeState(STATE_INIT)
                    }
                    if (-pullUpY >= loadMoreDist && state == STATE_INIT) {
                        changeState(STATE_RELEASE_TO_LOAD)
                    }
                }
                if (pullDownY + abs(pullUpY) > 8) {
                    ev.action = MotionEvent.ACTION_CANCEL
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pullDownY > refreshDist || -pullUpY > loadMoreDist) {
                    touch = false
                }
                if (state == STATE_RELEASE_TO_REFRESH) {
                    changeState(STATE_REFRESHING)
                    onRefreshListener?.onRefresh(this)
                } else if (state == STATE_RELEASE_TO_LOAD) {
                    changeState(STATE_LOADING)
                    onRefreshListener?.onLoadMore(this)
                }
                hide()
            }
        }
        return true
    }

    private fun initView() {
        refreshTextView = refreshView.findViewById(R.id.tv_refresh)
        refreshImageView = refreshView.findViewById(R.id.iv_refresh)
        loadMoreTextView = loadMoreView.findViewById(R.id.tv_load_more)
        loadMoreImageView = loadMoreView.findViewById(R.id.iv_load_more)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (!layout) {
            refreshView = getChildAt(0)
            pullableView = getChildAt(1)
            loadMoreView = getChildAt(2)
            layout = true
            initView()
            refreshDist = (refreshView as ViewGroup).getChildAt(0)
                .measuredHeight.toFloat()
            loadMoreDist = (loadMoreView as ViewGroup).getChildAt(0)
                .measuredHeight.toFloat()
        }
        refreshView.layout(
            0,
            (pullDownY + pullUpY).toInt() - refreshView.measuredHeight,
            refreshView.measuredWidth, (pullDownY + pullUpY).toInt()
        )
        pullableView.layout(
            0, (pullDownY + pullUpY).toInt(),
            pullableView.measuredWidth, (pullDownY + pullUpY).toInt()
                    + pullableView.measuredHeight
        )
        loadMoreView.layout(
            0,
            (pullDownY + pullUpY).toInt() + pullableView.measuredHeight,
            loadMoreView.measuredWidth,
            (pullDownY + pullUpY).toInt() + pullableView.measuredHeight
                    + loadMoreView.measuredHeight
        )
    }

    internal inner class RefreshTimer(private val handler: Handler) {
        private val timer: Timer = Timer()
        private var task: RefreshTask? = null
        fun schedule(period: Long) {
            if (task != null) {
                task!!.cancel()
                task = null
            }
            task = RefreshTask(handler)
            timer.schedule(task, 0, period)
        }

        fun cancel() {
            if (task != null) {
                task!!.cancel()
                task = null
            }
        }

        private inner class RefreshTask(private val handler: Handler) : TimerTask() {
            override fun run() {
                handler.obtainMessage().sendToTarget()
            }
        }

    }

    interface OnRefreshListener {
        fun onRefresh(layout: PullableLayout)
        fun onLoadMore(layout: PullableLayout)
    }

    companion object {
        const val STATE_INIT = 0
        const val STATE_RELEASE_TO_REFRESH = 1
        const val STATE_REFRESHING = 2
        const val STATE_RELEASE_TO_LOAD = 3
        const val STATE_LOADING = 4
        const val STATE_DONE = 5
        const val SUCCEED = 0
        const val FAIL = 1
    }

    init {
        initView(context)
    }
}