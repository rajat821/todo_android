package com.rajat.todo

import android.app.Activity
import android.app.AlertDialog
import android.content.AbstractThreadedSyncAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.ServerError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rajat.todo.Adapter.TodoListAdapter
import com.rajat.todo.Interfaces.RecyclerViewClickListener
import com.rajat.todo.model.TodoModel
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

class HomeFragment : Fragment(), RecyclerViewClickListener {

    lateinit var floatingActionButton : FloatingActionButton
    lateinit var sharedPreferenceClass: SharedPreferenceClass
    var token = ""

    lateinit var recyclerView : RecyclerView
    lateinit var emptyText : TextView
    lateinit var progressBar: ProgressBar
    lateinit var todoAdapter: TodoListAdapter

    var arrayList = ArrayList<TodoModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        floatingActionButton = view.findViewById(R.id.add_task_btn)
        sharedPreferenceClass = SharedPreferenceClass(context)
        token = sharedPreferenceClass.getValue_string("token")

        floatingActionButton.setOnClickListener {
            showAlertDialog()
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyText = view.findViewById(R.id.empty)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)

        getTasks()

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        return view
    }

    val simpleCallback = object  : ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT){
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            showDeleteDialog(arrayList[position].id,position)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun getTasks() {

        progressBar.visibility = View.VISIBLE
        val url = "https://todoapp-raj.herokuapp.com/api/todo"
        arrayList.clear()
        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.GET,url,null,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        val jsonArray = it.getJSONArray("todos")

                        if (jsonArray.length()==0)
                        {
                            emptyText.visibility = View.VISIBLE
                        }
                        else{
                            emptyText.visibility = View.GONE
                            for(i in 0 until jsonArray.length()){
                                val jsonObject = jsonArray.getJSONObject(i)
                                val todoList = TodoModel(
                                    jsonObject.getString("_id"),
                                    jsonObject.getString("title"),
                                    jsonObject.getString("description")
                                )
                                arrayList.add(todoList)
                            }
                            todoAdapter = TodoListAdapter(context as Activity,arrayList,this)
                            recyclerView.adapter = todoAdapter
                        }
                    }
                    progressBar.visibility = View.GONE
                }catch (e: JSONException){
                    e.printStackTrace()
                    progressBar.visibility = View.GONE
                }
            }, Response.ErrorListener {

                val response = it.networkResponse
                if(it==null || response!=null){

                }
                else {
                    var body =""
                    // val statusCode = it.networkResponse.statusCode.toString()
                    try {
                        body = String(
                            it.networkResponse.data,
                            charset("UTF-8")
                        )
                        val errorObject = JSONObject(body)

                        if(errorObject.getString("msg").equals("Token not Valid")){
                            sharedPreferenceClass.clearContent()
                            startActivity(Intent(context,Login::class.java))
                            Toast.makeText(context,"Session Expired", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        progressBar.visibility = View.GONE
                    }
                }
                progressBar.visibility = View.GONE
            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-type"] = "application/json"
                headers["Authorization"] = token
                return headers
            }
        }


        //set retry policy
        val socketTime = 3000
        val policy = DefaultRetryPolicy(socketTime,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonObjectRequest.retryPolicy = policy

        //sending queue to the server
        val queue = Volley.newRequestQueue(context)
        queue.add(jsonObjectRequest)
    }

    private fun showAlertDialog() {

        val alertLayout = layoutInflater.inflate(R.layout.custom_dialog_layout,null)

        val title_field : EditText = alertLayout.findViewById(R.id.title)
        val description_field : EditText = alertLayout.findViewById(R.id.description)

        val dialog : AlertDialog = AlertDialog.Builder(activity as Context)
            .setView(alertLayout)
            .setTitle("Add Task")
            .setPositiveButton("Add",null)
            .setNegativeButton("Cancel",null)
            .create()

        dialog.setOnShowListener {

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positive.setOnClickListener {
                val title = title_field.text.toString()
                val description = description_field.text.toString()

                if(TextUtils.isEmpty(title)){
                    Toast.makeText(activity,"Enter Title",Toast.LENGTH_SHORT).show()
                }else if(TextUtils.isEmpty(description)){
                    Toast.makeText(activity,"Enter Description",Toast.LENGTH_SHORT).show()
                }else{
                    addTask(title,description)
                    dialog.dismiss()
                }

                Toast.makeText(activity,"Task Added",Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showUpdateDialog(id: String, title: String, description: String) {
        val alertLayout = layoutInflater.inflate(R.layout.custom_dialog_layout,null)

        val title_field : EditText = alertLayout.findViewById(R.id.title)
        val description_field : EditText = alertLayout.findViewById(R.id.description)

        title_field.setText(title)
        description_field.setText(description)

        val dialog : AlertDialog = AlertDialog.Builder(activity as Context)
            .setView(alertLayout)
            .setTitle("Update Task")
            .setPositiveButton("Update",null)
            .setNegativeButton("Cancel",null)
            .create()

        dialog.setOnShowListener {

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positive.setOnClickListener {
                val title_1 = title_field.text.toString()
                val description_1 = description_field.text.toString()

                if(TextUtils.isEmpty(title_1)){
                    Toast.makeText(activity,"Enter Title",Toast.LENGTH_SHORT).show()
                }else if(TextUtils.isEmpty(description_1)){
                    Toast.makeText(activity,"Enter Description",Toast.LENGTH_SHORT).show()
                }else{
                    updateTask(id,title_1,description_1)
                    dialog.dismiss()
                }

                Toast.makeText(activity,"Task Updated",Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateTask(id:String,title1: String, description1: String) {
        val url = "https://todoapp-raj.herokuapp.com/api/todo/$id"

        val params = JSONObject()
        params.put("title",title1)
        params.put("description",description1)

        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.PUT,url,
            params,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        Toast.makeText(activity,"Updated Successfully",Toast.LENGTH_SHORT).show()
                        getTasks()
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
            }, Response.ErrorListener {
                val response = it.networkResponse
                if(it is ServerError && response!=null){
                    try {
                        val res = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers,"utf-8")))

                        val obj = JSONObject(res)
                        Toast.makeText(activity,obj.getString("msg"), Toast.LENGTH_SHORT).show()
                    }catch (e: JSONException){
                        e.printStackTrace()
                    }
                }
            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-type"] = "application/json"
                return headers
            }
        }


        //set retry policy
        val socketTime = 3000
        val policy = DefaultRetryPolicy(socketTime,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonObjectRequest.retryPolicy = policy

        //sending queue to the server
        val queue = Volley.newRequestQueue(context)
        queue.add(jsonObjectRequest)
    }

    private fun addTask(title: String, description: String) {
        val url = "https://todoapp-raj.herokuapp.com/api/todo"

        val params = JSONObject()
        params.put("title",title)
        params.put("description",description)

        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.POST,url,
            params,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        Toast.makeText(activity,"Added Successfully",Toast.LENGTH_SHORT).show()
                        getTasks()
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
            }, Response.ErrorListener {
                val response = it.networkResponse
                if(it is ServerError && response!=null){
                    try {
                        val res = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers,"utf-8")))

                        val obj = JSONObject(res)
                        Toast.makeText(activity,obj.getString("msg"), Toast.LENGTH_SHORT).show()
                    }catch (e: JSONException){
                        e.printStackTrace()
                    }
                }
            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-type"] = "application/json"
                headers["Authorization"] = token
                return headers
            }
        }


        //set retry policy
        val socketTime = 3000
        val policy = DefaultRetryPolicy(socketTime,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonObjectRequest.retryPolicy = policy

        //sending queue to the server
        val queue = Volley.newRequestQueue(context)
        queue.add(jsonObjectRequest)
    }

    override fun onItemClick(position: Int) {
    }

    override fun onLongItemClick(position: Int) {
        showUpdateDialog(arrayList[position].id, arrayList[position].title, arrayList[position].description)
    }

    override fun onEditButtonClick(position: Int) {
        showUpdateDialog(arrayList[position].id, arrayList[position].title, arrayList[position].description)
    }

    override fun onDeleteButtonClick(position: Int) {
        showDeleteDialog(arrayList[position].id,position)
    }

    private fun showDeleteDialog(id: String, position: Int) {

        val dialog : AlertDialog = AlertDialog.Builder(activity as Context)
            .setTitle("|| Delete Task Confirmation ||")
            .setPositiveButton("Yes",null)
            .setNegativeButton("No",DialogInterface.OnClickListener { dialogInterface, i ->
                getTasks()
            })
            .create()

        dialog.setOnShowListener {

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positive.setOnClickListener {
                deleteTodo(id,position)
                dialog.dismiss()
            }
        }

        dialog.show()

    }

    private fun deleteTodo(id: String, position: Int) {
        val url = "https://todoapp-raj.herokuapp.com/api/todo/$id"

        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.DELETE,url,
            null,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        Toast.makeText(activity,it.getString("msg"),Toast.LENGTH_SHORT).show()
                        arrayList.removeAt(position)
                        todoAdapter.notifyItemRemoved(position)
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
            }, Response.ErrorListener {
                val response = it.networkResponse
                if(it is ServerError && response!=null){
                    try {
                        val res = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers,"utf-8")))

                        val obj = JSONObject(res)
                        Toast.makeText(activity,obj.getString("msg"), Toast.LENGTH_SHORT).show()
                    }catch (e: JSONException){
                        e.printStackTrace()
                    }
                }
            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-type"] = "application/json"
                headers["Authorization"] = token
                return headers
            }
        }


        //set retry policy
        val socketTime = 3000
        val policy = DefaultRetryPolicy(socketTime,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonObjectRequest.retryPolicy = policy

        //sending queue to the server
        val queue = Volley.newRequestQueue(context)
        queue.add(jsonObjectRequest)
    }

    override fun onDoneButtonClick(position: Int) {
        showFinishedDialog(arrayList[position].id,position)
     }

    private fun showFinishedDialog(id: String, position: Int) {
        val dialog : AlertDialog = AlertDialog.Builder(activity as Context)
            .setTitle("|| Is Finished ? ||")
            .setPositiveButton("Yes",null)
            .setNegativeButton("No",null)
            .create()

        dialog.setOnShowListener {

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positive.setOnClickListener {
                finishTodo(id,position)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun finishTodo(id: String, position: Int) {
        val url = "https://todoapp-raj.herokuapp.com/api/todo/$id"

        val params = JSONObject()
        params.put("finished","true")

        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.PUT,url,
            params,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        arrayList.removeAt(position)
                        todoAdapter.notifyItemRemoved(position)
                        Toast.makeText(activity,it.getString("msg"),Toast.LENGTH_SHORT).show()
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
            }, Response.ErrorListener {
                val response = it.networkResponse
                if(it is ServerError && response!=null){
                    try {
                        val res = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers,"utf-8")))

                        val obj = JSONObject(res)
                        Toast.makeText(activity,obj.getString("msg"), Toast.LENGTH_SHORT).show()
                    }catch (e: JSONException){
                        e.printStackTrace()
                    }
                }
            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-type"] = "application/json"
                return headers
            }
        }


        //set retry policy
        val socketTime = 3000
        val policy = DefaultRetryPolicy(socketTime,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonObjectRequest.retryPolicy = policy

        //sending queue to the server
        val queue = Volley.newRequestQueue(context)
        queue.add(jsonObjectRequest)
    }
}