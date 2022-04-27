package com.rajat.todo.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.rajat.todo.Interfaces.RecyclerViewClickListener
import com.rajat.todo.R
import com.rajat.todo.model.TodoModel
import java.util.*

class FinishedAdapter (val context : Context, val arrayList : ArrayList<TodoModel>, val clickListener: RecyclerViewClickListener) : RecyclerView.Adapter<FinishedAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val accordianTitle : CardView = view.findViewById(R.id.accordian_title)
        val accordianBody: RelativeLayout = view.findViewById(R.id.accordian_body)
        val title: TextView = view.findViewById(R.id.task_title)
        val description: TextView = view.findViewById(R.id.task_description)
        val arrow: ImageView = view.findViewById(R.id.arrow)
        val delete: ImageView = view.findViewById(R.id.delete)

        init{
            view.setOnClickListener {
                clickListener.onItemClick(adapterPosition)
            }

            view.setOnLongClickListener {
                clickListener.onLongItemClick(adapterPosition)
                true
            }

            delete.setOnClickListener {
                clickListener.onDeleteButtonClick(adapterPosition)
            }
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.todo_finished_item,
            parent,
            false
        )

        val todoViewHolder = TodoViewHolder(view)

        val androidColors = view.resources.getIntArray(R.array.androidcolors)
        val randomColors = androidColors[Random().nextInt(androidColors.size)]
        todoViewHolder.accordianTitle.setBackgroundColor(randomColors)

        todoViewHolder.arrow.setOnClickListener {
            if(todoViewHolder.accordianBody.visibility == View.VISIBLE){
                todoViewHolder.accordianBody.visibility = View.GONE
            }else{
                todoViewHolder.accordianBody.visibility = View.VISIBLE
            }
        }

        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val list = arrayList[position]

        holder.title.text = list.title
        if(!list.description.equals("")) {
            holder.description.text = list.description
        }

    }

    override fun getItemCount(): Int {
        return arrayList.size
    }


}