package com.bmcreations.bookinfo.widget.behavior

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import java.lang.ref.WeakReference
import java.util.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.NestedScrollingChild
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.isAttachedToWindow
import androidx.core.view.ViewCompat.postOnAnimation
import androidx.customview.widget.ViewDragHelper
import com.bmcreations.bookinfo.R
import com.bmcreations.bookinfo.widget.behavior.AnchorPointState.*


class AnchorPointBottomSheetBehavior<V: View> constructor(context: Context, attrs: AttributeSet?): CoordinatorLayout.Behavior<V>(context, attrs) {

    private var mMinimumVelocity: Float = 0f

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Params_behavior_peekHeight
     */
    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * The height of the collapsed bottom sheet in pixels.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Params_behavior_peekHeight
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var peekHeight: Int = 0
        set(peekHeight) {
            field = Math.max(0, peekHeight)
            mMaxOffset = mParentHeight - peekHeight
        }
    var peekHeightPercent: Float = -1f

    private var mMinOffset: Int = 0
    private var mMaxOffset: Int = 0
    var anchorPoint: Int = 0
    var anchorPointPercent: Float = 1.0f
        set(value) {
            field = Math.min(1.0f, value)
        }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return `true` if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Params_behavior_hideable
     */
    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * `true` to make this bottom sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Params_behavior_hideable
     */
    var hideable: Boolean = false

    /**
     * Gets whether some states should be skipped.
     *
     * @return `true` if this bottom sheet can hide.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Params_behavior_hideable
     */
    /**
     * Sets whether some states should be skipped.
     *
     * `true` to make this bottom sheet hideable.
     * @attr ref android.support.design.R.styleable#BottomSheetBehavior_Params_behavior_hideable
     */
    var collapsible: Boolean = true

    var expandSwipeTo: AnchorPointState = Collapsed

    var locked: Boolean = false

    private var mState : AnchorPointState = Anchor
    private var mLastStableState : AnchorPointState = Anchor

    private var mViewDragHelper: ViewDragHelper? = null

    private var mIgnoreEvents: Boolean = false

    private var mNestedScrolled: Boolean = false

    private var mParentHeight: Int = 0

    private var mViewRef: WeakReference<V>? = null

    private var mNestedScrollingChildRef: WeakReference<View>? = null

    private var mCallback: Vector<BottomSheetCallback>? = null

    private var mActivePointerId: Int = 0

    private var mInitialX: Int = 0
    private var mInitialY: Int = 0

    private var mTouchingScrollingChild: Boolean = false

    private val mScrollVelocityTracker = ScrollVelocityTracker()

    /**
     * Gets the current state of the bottom sheet.
     *
     * @return One of [.STATE_EXPANDED], [.STATE_ANCHOR_POINT], [.STATE_COLLAPSED],
     * [.STATE_DRAGGING], and [.STATE_SETTLING].
     */
    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * state One of [.STATE_COLLAPSED], [.STATE_ANCHOR_POINT],
     * [.STATE_EXPANDED] or [.STATE_HIDDEN].
     */
    /**
     * New behavior (added: state == STATE_ANCHOR_POINT ||)
     */
    var state: AnchorPointState
        get() = mState
        set(state) {
            if (state == mState) {
                return
            }

            val child = mViewRef?.get() ?: return
            val parent = child.parent

            if (parent?.isLayoutRequested == true && isAttachedToWindow(child)) {
                child.post { startSettlingAnimation(child, state)}
            } else {
                startSettlingAnimation(child, state)
            }
        }

    private val mDragCallback = object : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (mState is Dragging) {
                return false
            }
            if (mTouchingScrollingChild) {
                return false
            }
            if (mState is Expanded && mActivePointerId == pointerId) {
                val scroll = mNestedScrollingChildRef?.get()
                scroll?.let {
                    if (it.canScrollVertically(-1)) {
                        return false
                    }
                }
            }
            return mViewRef?.get() === child
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            dispatchOnSlide(top, dy)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(Dragging)
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            var top: Int
            var targetState: AnchorPointState = if (yvel < 0) { // Moving up
                val currentTop = releasedChild.top
                if (anchorPoint in (mMinOffset + 1)..(currentTop - 1)) {
                    top = anchorPoint
                    Anchor
                } else {
                    top = mMinOffset
                    Expanded
                }
            } else if (hideable && shouldHide(releasedChild, yvel)) {
                top = mParentHeight
                Hidden
            } else if (yvel == 0f) {
                val currentTop = releasedChild.top
                val distanceToExpanded = Math.abs(currentTop - mMinOffset)
                val distanceToCollapsed = Math.abs(currentTop - mMaxOffset)
                val distanceToAnchor = Math.abs(currentTop - anchorPoint)

                if (anchorPoint > mMinOffset
                    && distanceToAnchor < distanceToExpanded
                    && distanceToAnchor < distanceToCollapsed) {
                    top = anchorPoint
                    Anchor
                } else if (distanceToExpanded < distanceToCollapsed) {
                    top = mMinOffset
                    Expanded
                } else {
                    top = mMaxOffset
                    Collapsed
                }
            } else {
                val currentTop = releasedChild.top
                if (anchorPoint > mMinOffset && expandSwipeTo == Anchor && currentTop < anchorPoint) {
                    top = anchorPoint
                    Anchor
                } else {
                    top = mMaxOffset
                    Collapsed
                }
            }

            // Restrict Collapsed view (optional)
            if (!collapsible && targetState is Collapsed) {
                Log.d("AnchorPointBS", "restricting collapsed as requested to anchor")
                top = anchorPoint
                targetState = Anchor
            }

            if (mViewDragHelper?.settleCapturedViewAt(releasedChild.left, top) == true) {
                setStateInternal(Settling)
                postOnAnimation(releasedChild,
                    SettleRunnable(releasedChild, targetState))
            } else {
                setStateInternal(targetState)
            }
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return constrain(top, mMinOffset, if (hideable) mParentHeight else mMaxOffset)
        }

        fun constrain(amount: Int, low: Int, high: Int): Int {
            return if (amount < low) low else if (amount > high) high else amount
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return if (hideable) {
                mParentHeight - mMinOffset
            } else {
                mMaxOffset - mMinOffset
            }
        }
    }

    /**
     * Callback for monitoring events about bottom sheets.
     */
    abstract class BottomSheetCallback {

        /**
         * Called when the bottom sheet changes its state.
         *
         * @param bottomSheet The bottom sheet view.
         * @param newState    The new state. This will be one of [.STATE_DRAGGING],
         * [.STATE_SETTLING], [.STATE_ANCHOR_POINT],
         * [.STATE_EXPANDED],
         * [.STATE_COLLAPSED], or [.STATE_HIDDEN].
         */
        abstract fun onStateChanged(bottomSheet: View, newState: AnchorPointState)

        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param bottomSheet The bottom sheet view.
         * @param slideOffset The new offset of this bottom sheet within its range, from 0 to 1
         * @param slideDirection The direction of the slide relative to the parent
         * when it is moving upward, and from 0 to -1 when it moving downward.
         */
        abstract fun onSlide(bottomSheet: View, slideOffset: Float, slideDirection: SlideDirection)
    }

    init {
        /**
         * Getting the anchorPoint...
         */
        anchorPoint = DEFAULT_ANCHOR_POINT
        var a = context.obtainStyledAttributes(attrs, R.styleable.AnchorPointBottomSheetBehavior)
        if (attrs != null) {
            if (a.hasValue(R.styleable.AnchorPointBottomSheetBehavior_behavior_anchorPointPercent)) {
                val ap = a.getFraction(R.styleable.AnchorPointBottomSheetBehavior_behavior_anchorPointPercent, 1, 1, -1f)
                if (ap != -1f) {
                    val height = context.resources.displayMetrics.heightPixels.toDouble()
                    anchorPointPercent = ap
                    // anchor is from the top
                    anchorPoint = (height * (1 - anchorPointPercent)).toInt()
                }
            } else if (a.hasValue(R.styleable.AnchorPointBottomSheetBehavior_behavior_anchorPoint)) {
                anchorPoint = a.getDimension(R.styleable.AnchorPointBottomSheetBehavior_behavior_anchorPoint, 0f).toInt()
            }
        }
        val def = a.getInt(R.styleable.AnchorPointBottomSheetBehavior_behavior_defaultState, Collapsed.identifier)
        mState = def.toAnchorPointState()

        val edef = a.getInt(R.styleable.AnchorPointBottomSheetBehavior_behavior_expandSwipeTo, Collapsed.identifier)
        expandSwipeTo = edef.toAnchorPointState()

        if (a.hasValue(R.styleable.AnchorPointBottomSheetBehavior_behavior_peekHeightPercent)) {
            val pp = a.getFraction(R.styleable.AnchorPointBottomSheetBehavior_behavior_peekHeightPercent, 1, 1, -1f)
            if (pp != -1f) {
                val height = context.resources.displayMetrics.heightPixels.toDouble()
                peekHeightPercent = pp
                // peek is from the bottom
                peekHeight = (height * peekHeightPercent).toInt()
            }
        }

        if (a.hasValue(R.styleable.AnchorPointBottomSheetBehavior_behavior_locked)) {
            locked = a.getBoolean(R.styleable.AnchorPointBottomSheetBehavior_behavior_locked, false)
        }

        a.recycle()

        a = context.obtainStyledAttributes(attrs,
            com.google.android.material.R.styleable.BottomSheetBehavior_Layout)
        // Only respect attributed [behavior_peekHeight] if not already set based on %'s
        if (peekHeightPercent == -1.0f) {
            @SuppressLint("PrivateResource")
            peekHeight = a.getDimensionPixelSize(
                com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, 0)
        }
        @SuppressLint("PrivateResource")
        hideable = a.getBoolean(com.google.android.material.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false)

        a.recycle()

        val configuration = ViewConfiguration.get(context)
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity.toFloat()
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): Parcelable? {
        return SavedState(super.onSaveInstanceState(parent, child), mState.identifier)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        val ss = state as SavedState?
        ss?.let {
            super.onRestoreInstanceState(parent, child, it.superState)
            // Intermediate states are restored as collapsed state
            mState = if (it.state is Dragging || it.state is Settling) {
                Collapsed
            } else {
                it.state
            }

            mLastStableState = mState
        }
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        parent.let { p ->
            child.let { c ->
                // First let the parent lay it out
                if (mState !is Dragging && mState !is Settling) {
                    if (p.fitsSystemWindows && !c.fitsSystemWindows) {
                        c.fitsSystemWindows = true
                    }
                    p.onLayoutChild(c, layoutDirection)
                }
                // Offset the bottom sheet
                mParentHeight = p.height
                if (anchorPointPercent < 1.0f) {
                    anchorPoint = (mParentHeight * (1 - anchorPointPercent)).toInt()
                }
                mMinOffset = Math.max(0, mParentHeight - c.height)
                mMaxOffset = Math.max(mParentHeight - peekHeight, mMinOffset)

                /**
                 * New behavior
                 */
                if (mState is Anchor) {
                    ViewCompat.offsetTopAndBottom(c, anchorPoint)
                } else if (mState is Expanded) {
                    ViewCompat.offsetTopAndBottom(c, mMinOffset)
                } else if (hideable && mState is Hidden || mState is ForceHidden) {
                    ViewCompat.offsetTopAndBottom(c, mParentHeight)
                } else if (mState is Collapsed) {
                    ViewCompat.offsetTopAndBottom(c, mMaxOffset)
                }
                if (mViewDragHelper == null) {
                    mViewDragHelper = ViewDragHelper.create(p, mDragCallback)
                }
                mViewRef = WeakReference(c)
                mNestedScrollingChildRef = WeakReference<View>(findScrollingChild(c))
            }
        }
        return true
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (!child.isShown) {
            mIgnoreEvents = true
            return false
        }

        return event.actionMasked.let { action ->
            if (action == MotionEvent.ACTION_DOWN) {
                reset()
            }

            when (action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mTouchingScrollingChild = false
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID
                    // Reset the ignore flag
                    if (mIgnoreEvents) {
                        mIgnoreEvents = false
                        return false
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    mScrollVelocityTracker.clear()
                    mInitialX = event.x.toInt()
                    mInitialY = event.y.toInt()
                    mNestedScrollingChildRef?.get()?.let { scroll ->
                        if (parent.isPointInChildBounds(scroll, mInitialX, mInitialY)) {
                            mActivePointerId = event.getPointerId(event.actionIndex)
                            mTouchingScrollingChild = true
                        }
                    }
                    mIgnoreEvents = mActivePointerId == MotionEvent.INVALID_POINTER_ID &&
                            parent.isPointInChildBounds(child, mInitialX, mInitialY) == false
                }
                MotionEvent.ACTION_MOVE -> {

                }
            }

            if (!mIgnoreEvents && mViewDragHelper?.shouldInterceptTouchEvent(event) == true) {
                return true
            }
            // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
            // it is not the top most view of its parent. This is not necessary when the touch event is
            // happening over the scrolling content as nested scrolling logic handles that case.
            val scroll = mNestedScrollingChildRef?.get()
            action == MotionEvent.ACTION_MOVE && scroll != null &&
                    !mIgnoreEvents && mState !is Dragging &&
                    !parent.isPointInChildBounds(scroll, event.x.toInt(), event.y.toInt()) && Math.abs(mInitialY - event.y) > mViewDragHelper?.touchSlop ?: 0
        }
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (locked) return false
        if (!child.isShown) return false

        event.let { e ->
            e.actionMasked.let { action ->
                if (mState is Dragging && action == MotionEvent.ACTION_DOWN) {
                    return true
                }

                // Detect scroll direction for ignoring collapsible
                if (mLastStableState is Anchor && action == MotionEvent.ACTION_MOVE) {
                    if (event.y > mInitialY && !collapsible) {
                        reset()
                        return false
                    }
                }

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    mTouchingScrollingChild = false
                }

                parent.let {
                    mViewDragHelper?.processTouchEvent(event)
                }

                if (action == MotionEvent.ACTION_DOWN) {
                    reset()
                }

                (child as? View)?.let {
                    // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
                    // to capture the bottom sheet in case it is not captured and the touch slop is passed.
                    if (action == MotionEvent.ACTION_MOVE && !mIgnoreEvents) {
                        if (Math.abs(mInitialY - event.y) > mViewDragHelper?.touchSlop ?: 0) {
                            if (mTouchingScrollingChild) {
                                mViewDragHelper?.captureChildView(child, event.getPointerId(event.actionIndex))
                            }
                        }
                    }
                }
            }
        }

        return !mIgnoreEvents
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V,
                                     directTargetChild: View, target: View, nestedScrollAxes: Int,
                                     @ViewCompat.NestedScrollType type: Int): Boolean {
        if (locked) return false
        mNestedScrolled = false
        return nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    private inner class ScrollVelocityTracker {
        private var mPreviousScrollTime: Long = 0
        var scrollVelocity = 0f
            private set

        fun recordScroll(dy: Int) {
            val now = System.currentTimeMillis()

            if (mPreviousScrollTime != 0L) {
                val elapsed = now - mPreviousScrollTime
                scrollVelocity = dy.toFloat() / elapsed * 1000 // pixels per sec
            }

            mPreviousScrollTime = now
        }

        fun clear() {
            mPreviousScrollTime = 0
            scrollVelocity = 0f
        }
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View,
                                   dx: Int, dy: Int, consumed: IntArray,
                                   @ViewCompat.NestedScrollType type: Int) {
        val scrollingChild = mNestedScrollingChildRef?.get()
        if (target !== scrollingChild) {
            return
        }

        mScrollVelocityTracker.recordScroll(dy)

        val currentTop = child.top
        val newTop = currentTop - dy

//        // Force stop at the anchor - do not go from collapsed to expanded in one scroll
//        if (mLastStableState is Collapsed && newTop < anchorPoint || mLastStableState is Expanded && newTop > anchorPoint) {
//            consumed[1] = dy
//            ViewCompat.offsetTopAndBottom(child, anchorPoint - currentTop)
//            dispatchOnSlide(child.top, dy)
//            mNestedScrolled = true
//            return
//        }

        if (dy > 0) { // Upward
            if (newTop < mMinOffset) {
                consumed[1] = currentTop - mMinOffset
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setStateInternal(Expanded)
            } else {
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setStateInternal(Dragging)
            }
        } else if (dy < 0) { // Downward
            if (!target.canScrollVertically(-1)) {
                if (newTop <= mMaxOffset || hideable) {
                    // Restrict STATE_COLLAPSED if restrictedState is set
                    if (collapsible || !collapsible && anchorPoint - newTop >= 0) {
                        consumed[1] = dy
                        ViewCompat.offsetTopAndBottom(child, -dy)
                        setStateInternal(Dragging)
                    }
                } else {
                    consumed[1] = currentTop - mMaxOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    setStateInternal(Collapsed)
                }
            }
        }
        dispatchOnSlide(child.top, dy)
        mNestedScrolled = true
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View,
                                    @ViewCompat.NestedScrollType type: Int) {
        if (child.top == mMinOffset) {
            setStateInternal(Expanded)
            mLastStableState = Expanded
            return
        }
        if (target !== mNestedScrollingChildRef?.get() || !mNestedScrolled) {
            return
        }
        val top: Int
        val targetState: AnchorPointState

        // Are we flinging up?
        val scrollVelocity = mScrollVelocityTracker.scrollVelocity
        if (scrollVelocity > mMinimumVelocity) {
            when (mLastStableState) {
                is Collapsed -> {
                    // Fling from collapsed to anchor
                    top = anchorPoint
                    targetState = Anchor
                }
                is Anchor -> {
                    // Fling from anchor to expanded
                    top = mMinOffset
                    targetState = Expanded
                }
                else -> {
                    // We are already expanded
                    top = mMinOffset
                    targetState = Expanded
                }
            }
        } else
        // Are we flinging down?
            if (scrollVelocity < -mMinimumVelocity) {
                if (mLastStableState is Expanded) {
                    when (expandSwipeTo) {
                        Anchor -> {
                            // Fling to from expanded to anchor
                            top = anchorPoint
                            targetState = Anchor
                        }
                        else -> {
                            if (collapsible) {
                                top = mMaxOffset
                                targetState = Collapsed
                            } else {
                                // Fling to from expanded to anchor
                                top = anchorPoint
                                targetState = Anchor
                            }
                        }
                    }
                } else if (hideable && shouldHide(child, scrollVelocity)) {
                    top = mParentHeight
                    targetState = Hidden
                } else if (collapsible) {
                    if (mLastStableState is Anchor) {
                        // Fling from anchor to collapsed
                        top = mMaxOffset
                        targetState = Collapsed
                    } else {
                        val currentTop = child.top
                        if (anchorPoint > mMinOffset && expandSwipeTo == Anchor && currentTop < anchorPoint) {
                            top = anchorPoint
                            targetState = Anchor
                        } else {
                            top = mMaxOffset
                            targetState = Collapsed
                        }
                    }
                } else {
                    top = anchorPoint
                    targetState = Anchor
                }
            } else {
                // Collapse?
                if (hideable && shouldHide(child, scrollVelocity)) {
                    top = mParentHeight
                    targetState = Hidden
                } else {
                    val currentTop = child.top
                    if (currentTop > anchorPoint * 1.25 && collapsible) { // Multiply by 1.25 to account for parallax. The currentTop needs to be pulled down 50% of the anchor point before collapsing.
                        top = mMaxOffset
                        targetState = Collapsed
                    } else if (currentTop < anchorPoint * 0.5) {
                        top = mMinOffset
                        targetState = Expanded
                    } else {
                        top = anchorPoint
                        targetState = Anchor
                    }// Snap back to the anchor
                    // Expand?
                }
            }// Not flinging, just settle to the nearest state

        if (mViewDragHelper?.smoothSlideViewTo(child, child.left, top) == true) {
            setStateInternal(Settling)
            postOnAnimation(child, SettleRunnable(child, targetState))
        } else {
            setStateInternal(targetState)
        }
        mNestedScrolled = false
    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View,
                                  velocityX: Float, velocityY: Float): Boolean {
        if (locked) return false
        return target === mNestedScrollingChildRef?.get() &&
                (mState !is Expanded ||
                        super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY))
    }

    /**
     * Adds a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    fun addBottomSheetCallback(callback: BottomSheetCallback) {
        mCallback?.add(callback) ?: run {
            mCallback = Vector()
            mCallback?.add(callback)
        }
    }

    private fun setStateInternal(state: AnchorPointState) {
        if (mState == state) {
            return
        }
        mState = state
        mLastStableState = mState
        val bottomSheet = mViewRef?.get()
        if (bottomSheet != null && mCallback != null) {
            notifyStateChangedToListeners(bottomSheet, state)
        }
    }

    private fun notifyStateChangedToListeners(bottomSheet: View, newState: AnchorPointState) {
        mCallback?.let { c ->
            c.forEach { it.onStateChanged(bottomSheet, newState) }
        }
    }

    private fun notifyOnSlideToListeners(bottomSheet: View, slideOffset: Float, slideDirection: SlideDirection) {
        mCallback?.let { c ->
            c.forEach { it.onSlide(bottomSheet, slideOffset, slideDirection) }

        }
    }

    private fun reset() {
        mActivePointerId = ViewDragHelper.INVALID_POINTER
    }

    private fun shouldHide(child: View, yvel: Float): Boolean {
        if (child.top < mMaxOffset) {
            // It should not hide, but collapse.
            return false
        }
        val newTop = child.top + yvel * HIDE_FRICTION
        return Math.abs(newTop - mMaxOffset) / peekHeight.toFloat() > HIDE_THRESHOLD
    }

    private fun findScrollingChild(view: View): View? {
        if (view is NestedScrollingChild) {
            return view
        }
        if (view is ViewGroup) {
            var i = 0
            val count = view.childCount
            while (i < count) {
                val scrollingChild = findScrollingChild(view.getChildAt(i))
                if (scrollingChild != null) {
                    return scrollingChild
                }
                i++
            }
        }
        return null
    }

    private fun dispatchOnSlide(top: Int, dy: Int) {
        val bottomSheet = mViewRef?.get()
        bottomSheet?.let { bs ->
            mCallback?.let { cb ->
                val direction = if (dy < 0) SlideDirection.Upward else SlideDirection.Downward
                if (top > mMaxOffset) {
                    notifyOnSlideToListeners(bs, (mMaxOffset - top).toFloat() / (mParentHeight - mMaxOffset), direction)
                } else {
                    notifyOnSlideToListeners(bs, (mMaxOffset - top).toFloat() / (mMaxOffset - peekHeight), direction)
                }
            }
        }
    }

    private fun startSettlingAnimation(child: View, state: AnchorPointState) {
        val top = when (state) {
            is Anchor -> anchorPoint
            is Collapsed -> mMaxOffset
            is Expanded -> mMinOffset
            is Hidden -> {
                if (hideable) {
                    mParentHeight
                } else {
                    throw IllegalArgumentException("Illegal state argument: $state when not hideable")
                }
            }
            is ForceHidden -> mParentHeight
            else -> throw IllegalArgumentException("Illegal state argument: $state")
        }

        if (mViewDragHelper?.smoothSlideViewTo(child, child.left, top) == true) {
            setStateInternal(Settling)
            postOnAnimation(child, SettleRunnable(child, state))
        } else {
            setStateInternal(state)
        }
    }

    private inner class SettleRunnable internal constructor(private val view: View,
                                                            private val targetState: AnchorPointState) : Runnable {

        override fun run() {
            if (mViewDragHelper?.continueSettling(true) == true) {
                postOnAnimation(view, this)
            } else {
                setStateInternal(targetState)
            }
        }
    }

    private class SavedState : View.BaseSavedState {

        internal val state: AnchorPointState

        constructor(source: Parcel) : super(source) {
            state = source.readInt().toAnchorPointState()
        }

        constructor(superState: Parcelable?, state: Int) : super(superState) {
            this.state = state.toAnchorPointState()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(state.identifier)
        }

        companion object {

            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {

        private const val HIDE_THRESHOLD = 0.5f
        private const val HIDE_FRICTION = 0.1f

        private const val DEFAULT_ANCHOR_POINT = 700

        /**
         * A utility function to get the [AnchorPointBottomSheetBehavior] associated with the `view`.
         *
         * @param view The [View] with [AnchorPointBottomSheetBehavior].
         * @param <V> Instance of behavior
         * @return The [AnchorPointBottomSheetBehavior] associated with the `view`.
        </V> */
        fun <V : View> from(view: V): AnchorPointBottomSheetBehavior<V> {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams
                ?: throw IllegalArgumentException("The view is not a child of CoordinatorLayout")
            val behavior = params
                .behavior as? AnchorPointBottomSheetBehavior<*>
                ?: throw IllegalArgumentException(
                    "The view is not associated with AnchorPointBottomSheetBehavior")
            @Suppress("UNCHECKED_CAST")
            return behavior as AnchorPointBottomSheetBehavior<V>
        }
    }
}

fun Int.toAnchorPointState() : AnchorPointState {
    return when (this) {
        1 -> Dragging
        2 -> Settling
        3 -> Anchor
        4 -> Expanded
        5 -> Collapsed
        6 -> Hidden
        7 -> ForceHidden
        else -> Collapsed
    }
}

sealed class AnchorPointState(val identifier: Int = -1) {
    object Dragging: AnchorPointState(1)
    object Settling: AnchorPointState(2)
    object Anchor: AnchorPointState(3)
    object Expanded: AnchorPointState(4)
    object Collapsed: AnchorPointState(5)
    object Hidden: AnchorPointState(6)
    object ForceHidden: AnchorPointState(7)
}

sealed class SlideDirection {
    object Upward: SlideDirection()
    object Downward: SlideDirection()
}