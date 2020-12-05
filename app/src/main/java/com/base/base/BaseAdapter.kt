package com.base.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Nikul on 14-08-2020.
 */
abstract class BaseAdapter<T>(private var layout: Int = 0) : RecyclerView.Adapter<BaseAdapter<T>.ViewHolder>() {

    val list = ArrayList<T>()

    private var mOnLayoutSelector: OnLayoutSelector? = null

    private lateinit var mRecyclerView: RecyclerView
    private var emptyView: View? = null

    private var onClickView: ((View, Int, T) -> Unit)? = null
    private var onLongClickView: ((View, Int, T) -> Unit)? = null
    private var onSeekBarProgress: ((Int, T, SeekBar?, Int, Boolean) -> Unit)? = null
    private var onStartTrackingTouch: ((Int, T, SeekBar?) -> Unit)? = null
    private var onStopTrackingTouch: ((Int, T, SeekBar?) -> Unit)? = null
    private var onCreateViewHolderBlock: ((View) -> View)? = null

    fun setOnLayoutSelector(mOnLayoutSelector: OnLayoutSelector) {
        this.mOnLayoutSelector = mOnLayoutSelector
    }

    fun setItemClickListener(onClickView: (View, Int, T) -> Unit) {
        this.onClickView = onClickView
    }

    fun setItemLongClickListener(onLongClickView: (View, Int, T) -> Unit) {
        this.onLongClickView = onLongClickView
    }

    fun setItemSeekBarChangeListener(
        onSeekBarProgress: (Int, T, SeekBar?, Int, Boolean) -> Unit,
        onStartTrackingTouch: (Int, T, SeekBar?) -> Unit,
        onStopTrackingTouch: (Int, T, SeekBar?) -> Unit
    ) {
        this.onSeekBarProgress = onSeekBarProgress
        this.onStartTrackingTouch = onStartTrackingTouch
        this.onStopTrackingTouch = onStopTrackingTouch
    }

    fun setOnCreateViewHolderBlock(block: ((View) -> View)) {
        this.onCreateViewHolderBlock = block
    }

    interface OnLayoutSelector {
        fun selectLayout(itemViewType: Int): Int?
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (mOnLayoutSelector != null) {
            if (mOnLayoutSelector?.selectLayout(viewType) != null) {
                layout = mOnLayoutSelector?.selectLayout(viewType)!!
            }
        }

        var v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        onCreateViewHolderBlock?.let { it -> v = it(v) }
        return ViewHolder(v, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            onBind(holder.itemViewType, holder.getBindView(), position, list[position], payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBind(holder.itemViewType, holder.getBindView(), position, list[position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(val view: View, val context: Context) : RecyclerView.ViewHolder(view) {

        init {
            setClickableView(view).forEach { clickView ->
                clickView?.setOnClickListener {
                    onClickView?.let { it1 -> it1(it, adapterPosition, list[adapterPosition]) }
                }
            }
            setLongClickableView(view).forEach { longClickView ->
                longClickView?.setOnLongClickListener {
                    onLongClickView?.let { it1 -> it1(it, adapterPosition, list[adapterPosition]) }
                    return@setOnLongClickListener true
                }
            }
            setSeekBarListener(view).forEach { seekBarView ->
                seekBarView?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        onSeekBarProgress?.let { onProgress ->
                            onProgress(adapterPosition, list[adapterPosition], seekBar, progress, fromUser)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        onStartTrackingTouch?.let { onStartTouch ->
                            onStartTouch(adapterPosition, list[adapterPosition], seekBar)
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        onStopTrackingTouch?.let { onStopTouch ->
                            onStopTouch(adapterPosition, list[adapterPosition], seekBar)
                        }
                    }

                })
            }
        }

        fun getBindView(): View = view
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    open fun addItemAt(index: Int, item: T) {
        list.add(index, item)
        notifyItemInserted(index)
    }

    open fun addItem(item: T) {
        list.add(item)
        notifyItemInserted(list.size)
    }

    open fun addAll(dataList: Collection<T>) {
        list.clear()
        list.addAll(dataList)
        notifyDataSetChanged()
        if (::mRecyclerView.isInitialized) mRecyclerView.checkIfEmpty(emptyView)
    }

    open fun appendAll(dataList: Collection<T>) {
        val oldSize = list.size
        list.addAll(dataList)
        notifyItemRangeInserted(oldSize, dataList.size)
    }

    open fun clearAll() {
        list.clear()
        notifyDataSetChanged()
    }

    open fun removeItemAt(position: Int) {
        if (position in list.indices) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setEmptyView(emptyView: View?) {
        this.emptyView = emptyView
    }

    abstract fun setClickableView(itemView: View): List<View?>

    abstract fun setLongClickableView(itemView: View): List<View?>

    open fun setSeekBarListener(itemView: View): List<SeekBar?> {
        return listOf()
    }

    abstract fun onBind(viewType: Int, view: View, position: Int, item: T, payloads: MutableList<Any>? = null)

    open fun selectMyLayout(itemViewType: Int): Int? = null

    private fun RecyclerView.checkIfEmpty(emptyView: View?) {
        if (emptyView != null && adapter != null) {
            val emptyViewVisible = adapter?.itemCount == 0
            emptyView.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
            visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
        }
    }
}