package com.base.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * Created by Keval on 14/08/19.
 */
class DividerDecorator(
    private val orientation: Int,
    private val firstDivider: Int,
    private val middleDivider: Int = firstDivider,
    private val lastDivider: Int = firstDivider
) : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemCount = state.itemCount

        val itemPosition = parent.getChildAdapterPosition(view)

        // no position, leave it alone
        if (itemPosition == RecyclerView.NO_POSITION) {
            return
        }

        // first item
        if (itemPosition == 0) {
            if (orientation == RecyclerView.HORIZONTAL) {
                outRect.set(firstDivider, view.paddingTop, middleDivider, view.paddingBottom)
            } else {
                outRect.set(view.paddingLeft, firstDivider, view.paddingRight, middleDivider)
            }
        }

        // last item
        else if (itemCount > 0 && itemPosition == itemCount - 1) {
            if (orientation == RecyclerView.HORIZONTAL) {
                outRect.set(view.paddingLeft, view.paddingTop, lastDivider, view.paddingBottom)
            } else {
                outRect.set(view.paddingLeft, view.paddingTop, view.paddingRight, lastDivider)
            }
        }

        // every middle item
        else {
            if (orientation == RecyclerView.HORIZONTAL) {
                outRect.set(view.paddingLeft, view.paddingTop, middleDivider, view.paddingBottom)
            } else {
                outRect.set(view.paddingLeft, view.paddingTop, view.paddingRight, middleDivider)
            }
        }
    }
}