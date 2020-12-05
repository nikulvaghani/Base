package com.base.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Nikul on 05-05-2020.
 */
abstract class BaseListAdapter<T>(private var layout: Int = 0, diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, BaseListAdapter<T>.ViewHolder>(diffCallback) {

    private var mOnLayoutSelector: BaseAdapter.OnLayoutSelector? = null

    private lateinit var mRecyclerView: RecyclerView
    private var emptyView: View? = null

    private var onClickView: ((View, Int, T) -> Unit)? = null
    private var onCreateViewHolderBlock: ((View) -> View)? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (mOnLayoutSelector != null) {
            if (mOnLayoutSelector?.selectLayout(viewType) != null) {
                layout = mOnLayoutSelector?.selectLayout(viewType)!!
            }
        }

        var v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        onCreateViewHolderBlock?.let { it -> v = it(v) }
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            onBind(holder.itemViewType, holder.getBindView(), position, getItem(position), payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBind(holder.itemViewType, holder.getBindView(), position, getItem(position))
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        init {
            setClickableView(view).forEach { clickView ->
                clickView?.setOnClickListener {
                    onClickView?.let { it1 -> it1(it, adapterPosition, getItem(adapterPosition)) }
                }
            }
        }

        fun getBindView(): View = view
    }

    fun setOnLayoutSelector(mOnLayoutSelector: BaseAdapter.OnLayoutSelector) {
        this.mOnLayoutSelector = mOnLayoutSelector
    }

    fun setItemClickListener(onClickView: (View, Int, T) -> Unit) {
        this.onClickView = onClickView
    }

    fun setOnCreateViewHolderBlock(block: ((View) -> View)) {
        this.onCreateViewHolderBlock = block
    }

    fun setEmptyView(emptyView: View?) {
        this.emptyView = emptyView
    }

    open fun addItem(item: T) {
        currentList.add(item)
        notifyItemInserted(currentList.size)
    }

    open fun addAll(dataList: List<T>) {
        submitList(dataList)
    }

    open fun appendAll(dataList: Collection<T>) {
        val newList = ArrayList<T>()
        newList.addAll(currentList)
        newList.addAll(dataList)
        submitList(newList)
    }

    open fun clearAll() {
        submitList(emptyList())
    }

    open fun removeItemAt(position: Int) {
        if (position in currentList.indices) {
            currentList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    abstract fun setClickableView(itemView: View): List<View?>

    abstract fun onBind(viewType: Int, view: View, position: Int, item: T, payloads: MutableList<Any>? = null)

    open fun selectMyLayout(itemViewType: Int): Int? = null

    interface OnLayoutSelector {
        fun selectLayout(itemViewType: Int): Int?
    }

    private fun RecyclerView.checkIfEmpty(emptyView: View?) {
        if (emptyView != null && adapter != null) {
            val emptyViewVisible = adapter?.itemCount == 0
            emptyView.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
            visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
        }
    }
}