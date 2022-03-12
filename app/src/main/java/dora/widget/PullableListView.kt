package dora.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

class PullableListView @JvmOverloads constructor(context: Context, attrs: AttributeSet?,
                                                 defStyleAttr: Int = 0)

    : ListView(context, attrs, defStyleAttr), Pullable {

    override fun canPullDown(): Boolean {
        return if (count == 0) {
            true
        } else firstVisiblePosition == 0
            && getChildAt(0).top >= 0
    }

    override fun canPullUp(): Boolean {
        if (count == 0) {
            return true
        } else if (lastVisiblePosition == count - 1) {
            if (getChildAt(lastVisiblePosition - firstVisiblePosition) != null
                && getChildAt(
                    lastVisiblePosition
                            - firstVisiblePosition
                ).bottom <= measuredHeight
            ) {
                return true
            }
        }
        return false
    }
}