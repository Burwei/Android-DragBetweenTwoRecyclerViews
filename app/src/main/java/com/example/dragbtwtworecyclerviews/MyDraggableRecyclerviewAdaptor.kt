package com.example.dragbtwtworecyclerviews

import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlin.math.roundToInt

/**
 *  Make the item of RecyclerView draggable.
 *  You have to make your RecyclerViewAdaptor implement this interface
 *  and also create a instance of MyDragListener to let your RecyclerView's item
 *  draggable.
 *  @author Burwei
 */
interface MyDraggableRecyclerviewAdaptor {
    /**
     *  Set data of the RecyclerView.
     *  The data could be any type you want, but the list must be mutable.
     *  @param data the underlying data that RecyclerView use it to make
     *  a list of items.
     */
    fun setData(data: MutableList<Any>)

    /**
     *  Get data of the RecyclerView.
     *  @return the underlying data that RecyclerView use it to make
     *  a list of items.
     */
    fun getData(): MutableList<Any>

    /**
     *  Set the drag listener to handle drag events.
     *  This method MUST called BEFORE setData(); otherwise you'll get
     *  a NullPointerException while calling this method.
     *  @param listener the drag listener that handle all drag events.
     */
    fun setDragListener(listener: MyDragListener)


    /**
     *  RecyclerView.Adaptor's methods that MyDragListener will use.
     */
    fun notifyItemMoved(sourcePosition: Int, targetPosition: Int)
    fun notifyDataSetChanged()

    /**
     *  Set each item of the RecyclerView to be draggable.
     *  You have to call this method in onBindViewHolder() to set the
     *  drag setting on each item's root layout.
     *  All items use the same drag listener.
     *  The "All items" here means not only all items in this RecyclerView,
     *  but also items in all other RecyclerViews which you want its item to be
     *  draggable.
     *  And you have to call this method to make the RecyclerView draggable,
     *  so that your item won't fly back to its origin position when receive
     *  the ACTION_DROP drag event. (Since we make the item invisible,
     *  the drop event won't be receive when you drop item on the invisible item,
     *  so we need RecyclerView to receive that; otherwise the item will fly back
     *  to its original position due to the drag event return false.)
     *  @param v the View that is going to be set draggable.
     *  @param position the position of this View in the parent RecyclerView.
     *  @param dragListener the drag listener that handle all drag events.
     */
    fun setDrag(v: View, position: Int, dragListener: MyDragListener) {
        var touchedX = 0f  // closure variable
        var touchedY = 0f  // closure variable
        v.visibility = View.VISIBLE
        v.setOnDragListener(dragListener)
        v.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchedX = event.x
                    touchedY = event.y
                }
            }
            return@setOnTouchListener false  // leave the touch event to other listeners
        }
        v.setOnLongClickListener {
            it.visibility = View.INVISIBLE
            val myShadow = MyDragShadowBuilder(it, touchedX.roundToInt(), touchedY.roundToInt())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                it.startDragAndDrop(
                        null,
                        myShadow,
                        it,
                        0
                )
            } else {
                it.startDrag(
                        null,
                        myShadow,
                        it,
                        0
                )
            }
        }
    }
}

/**
 *  A example RecyclerViewAdaptor class to implement the
 *  MyDraggableRecyclerviewAdaptor interface.
 *  This class implements all methods in MyDraggableRecyclerviewAdaptor,
 *  and also contains a simple OnClickLister to print the item you click.
 *  @author Burwei
 */
class MyRecyclerviewAdaptor :
        RecyclerView.Adapter<MyRecyclerviewAdaptor.MyViewHolder>(),
        MyDraggableRecyclerviewAdaptor {

    private var clickListener: OnClickListener? = null
    private var myDataset = mutableListOf<Any>()
    private var dragListener: MyDragListener? = null

    interface OnClickListener {
        fun recyclerviewClick(name: String)
    }

    fun setClickListener(parentFragment: OnClickListener) {
        clickListener = parentFragment
    }

    override fun setDragListener(listener: MyDragListener) {
        dragListener = listener
    }

    override fun setData(data: MutableList<Any>) {
        myDataset = data
        notifyDataSetChanged()
    }

    override fun getData(): MutableList<Any> {
        return myDataset
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutAnimal: ConstraintLayout = itemView.findViewById(R.id.layoutAnimal)
        val txtAnimalName: TextView = itemView.findViewById(R.id.txtAnimalName)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): MyRecyclerviewAdaptor.MyViewHolder {
        // create a new view
        val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_animal, parent, false) as View
        // set the view's size, margins, paddings and layout parameters
        return MyViewHolder(item)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val name = myDataset[position]
        holder.txtAnimalName.text = name as String
        holder.layoutAnimal.setOnClickListener {
            clickListener?.recyclerviewClick(name)
        }
        setDrag(holder.layoutAnimal, position, dragListener!!)
    }

    /**
     *  This is important, set a drag listener to the RecyclerView to
     *  receive the ACTION_DROP drag event.
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.setOnDragListener(dragListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        clickListener = null
        clear()
    }


    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size

    fun clear() {
        myDataset = mutableListOf<Any>()
    }
}


/**
 *  MyDragShadowBuilder is a simple DragShadowBuilder that create a
 *  shadow with a background color when dragging.
 *  @author Burwei
 */
class MyDragShadowBuilder(v: View, private val touchedX: Int, private val touchedY: Int) : View.DragShadowBuilder(v) {

    /**
     *  Set the shadow's size and position.
     *  The shadow's size is as big as the dragged view, and it's initial position
     *  is same as the dragged view.
     *  This method takes touchedX and touchedY from the class' attribute, and set
     *  it as the point where user touch this shadow, to let shadows initial position
     *  be the same as the dragged view.
     *  @param size the same as the original param, check official doc for more info.
     *  @param touch the same as the original param, check official doc for more info.
     */
    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        super.onProvideShadowMetrics(size, touch)
        touch.set(touchedX, touchedY)
    }

    /**
     *  Set the shadow's background color.
     *  You can customize your shadow here.
     *  @param canvas the same as the original param, check official doc for more info.
     */
    override fun onDrawShadow(canvas: Canvas) {
        super.onDrawShadow(canvas)
        canvas.drawColor(0x22000000)
    }
}

/**
 *  MyDragListener is the listener that handle all drag events.
 *  It should be used with MyDraggableRecyclerviewAdaptor.
 *  All items that is draggable should set the same drag listener instance
 *  as their drag listener, in order to use the same variables in the instance.
 */
class MyDragListener : View.OnDragListener {

    private var isStarted = false
    private var isOriginalParent = true
    private var initPositionInOriParent = 0
    private var initPositionInOtherParent = 0
    private var finalPosition = 0
    private var finalPositionInOriParent = 0
    private var finalParent: RecyclerView? = null


    override fun onDrag(v: View?, event: DragEvent?): Boolean {
        if (v == null || v.parent == null) {
            return true
        }
        when (event?.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                /*
                 *  Every view which is set as draggable will enter this state
                 *  when the dragging is begin, and its event.localState are all the same.
                 *  So we only need to process the first one.
                 */
                val sourceView = event.localState as View
                if (!isStarted && sourceView.parent != null) {
                    val sourcePosition = (sourceView.parent as RecyclerView).getChildAdapterPosition(sourceView)
                    initPositionInOriParent = sourcePosition
                    finalPosition = sourcePosition
                    isStarted = true
                }
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                /*
                 *  The current touched view is the target view.
                 *  There's four cases:
                 *  1. target view is RecyclerView, which is the source view's parent.
                 *  2. target view is RecyclerView, which is not the source view's parent.
                 *  3. target view is item, which is in source view's original parent.
                 *  4. target view is item, which is not in source view's original parent.
                 */
                val sourceView = event.localState as View
                /*
                 *  Case 1 and 2 (v is RecyclerView):
                 *      Add source view to the RecyclerView's end only when the
                 *      target RecyclerView is empty
                 */
                if (v is RecyclerView) {
                    if (v.adapter!!.itemCount == 0) {
                        val sourceValue = ((sourceView.parent as RecyclerView).adapter as MyDraggableRecyclerviewAdaptor).getData()[initPositionInOriParent]
                        try {
                            (v.adapter!! as MyDraggableRecyclerviewAdaptor).getData().add(sourceValue)
                            (v.adapter!! as MyDraggableRecyclerviewAdaptor).notifyDataSetChanged()
                            initPositionInOtherParent = 0
                        } catch (e: Exception) {
                            println("ignore index out of bound")
                        }
                        finalParent = v
                        isOriginalParent = false
                        finalPosition = 0
                    }
                    return true
                }
                /*
                 *  source view must has parent if it's not case 1 and 2.
                 */
                if (sourceView.parent == null) {
                    return true
                }
                val targetAdaptor = (v.parent as RecyclerView).adapter!! as MyDraggableRecyclerviewAdaptor
                val targetPosition = (v.parent as RecyclerView).getChildAdapterPosition(v)
                /*
                 *  Case 3 (v.parent == sourceView.parent):
                 *      There're 2 sub cases here,
                 *      (1) Item hasn't dragged out of source view's original parent yet.
                 *      (2) Item is dragged back to source view's original parent
                 */
                if (v.parent == sourceView.parent) {
                    if (isOriginalParent) {
                        try {
                            targetAdaptor.notifyItemMoved(finalPosition, targetPosition)
                        } catch (e: Exception) {
                            println("ignore index out of bound")
                        }
                    } else {
                        try {
                            targetAdaptor.notifyItemMoved(finalPositionInOriParent, targetPosition)
                            (finalParent?.adapter as MyDraggableRecyclerviewAdaptor?)?.getData()!!.removeAt(initPositionInOtherParent)
                            finalParent?.adapter?.notifyDataSetChanged()
                        } catch (e: Exception) {
                            println("ignore index out of bound")
                        }
                        isOriginalParent = true
                    }
                    finalPosition = targetPosition
                    finalPositionInOriParent = targetPosition
                }
                /*
                 *  Case 4 (v.parent != sourceView.parent):
                 *      There're 2 sub cases here,
                 *      (1) Item is dragged out of source view's original parent the first time.
                 *      (2) Item is already in other parent.
                 */
                else {
                    if (isOriginalParent) {
                        val sourceValue = ((sourceView.parent as RecyclerView).adapter as MyDraggableRecyclerviewAdaptor).getData()[initPositionInOriParent]
                        try {
                            targetAdaptor.getData().add(targetPosition, sourceValue)
                            targetAdaptor.notifyDataSetChanged()
                            (v.parent as RecyclerView)[targetPosition].visibility = View.INVISIBLE  // not working, don't know why
                            initPositionInOtherParent = targetPosition
                        } catch (e: Exception) {
                            println("ignore index out of bound")
                        }
                        isOriginalParent = false
                        finalPosition = targetPosition
                    } else {
                        if (finalPosition != targetPosition) {
                            try {
                                targetAdaptor.notifyItemMoved(finalPosition, targetPosition)
                            } catch (e: Exception) {
                                println("ignore index out of bound")
                            }
                            finalPosition = targetPosition
                        }
                    }
                }
                finalParent = v.parent as RecyclerView
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                /*
                 *  Every view which is set as draggable will enter this state
                 *  when the dragging is ended, and its event.localState are all the same.
                 *  We'll reset all RecyclerView's data in this state.
                 */
                println("ACTION_DRAG_ENDED")
                val sourceView = event.localState as View
                /*
                 *  v is RecyclerView, refresh all RecyclerViews.
                 */
                if(v is RecyclerView){
                    (v.adapter as MyDraggableRecyclerviewAdaptor).notifyDataSetChanged()
                }
                /*
                 *  v is Item, update the data.
                 */
                if (finalParent == null || sourceView.parent == null) {
                    return true
                }
                val sourceParent = sourceView.parent as RecyclerView
                val sourceValue = ((sourceView.parent as RecyclerView).adapter as MyDraggableRecyclerviewAdaptor).getData()[initPositionInOriParent]
                if (finalParent == sourceView.parent) {
                    (finalParent!!.adapter as MyDraggableRecyclerviewAdaptor).getData().removeAt(initPositionInOriParent)
                    (finalParent!!.adapter as MyDraggableRecyclerviewAdaptor).getData().add(finalPosition, sourceValue)
                } else {
                    (sourceParent.adapter as MyDraggableRecyclerviewAdaptor).getData().removeAt(initPositionInOriParent)
                    (finalParent!!.adapter as MyDraggableRecyclerviewAdaptor).getData().removeAt(initPositionInOtherParent)
                    (finalParent!!.adapter as MyDraggableRecyclerviewAdaptor).getData().add(finalPosition, sourceValue)
                }
                (finalParent!!.adapter as MyDraggableRecyclerviewAdaptor).notifyDataSetChanged()
                (sourceView.parent as RecyclerView?)?.adapter?.notifyDataSetChanged()
                isStarted = false
                finalParent = null
                isOriginalParent = true
            }
        }
        return true
    }
}