package com.rajat.todo

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.ServerError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.rajat.todo.Adapter.FinishedAdapter
import com.rajat.todo.Interfaces.RecyclerViewClickListener
import com.rajat.todo.model.TodoModel
import org.json.JSONException
import org.json.JSONObject

class FinishedTaskFragment : Fragment(),RecyclerViewClickListener {

    lateinit var sharedPreferenceClass: SharedPreferenceClass
    var token = ""

    lateinit var recyclerView : RecyclerView
    lateinit var progressBar: ProgressBar
    lateinit var emptyText : TextView
    lateinit var todoAdapter: FinishedAdapter

    var arrayList = ArrayList<TodoModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_finished_task, container, false)

        sharedPreferenceClass = SharedPreferenceClass(context)
        token = sharedPreferenceClass.getValue_string("token")


        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.empty)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)

        getTasks()

        return view
    }

    private fun getTasks() {

        progressBar.visibility = View.VISIBLE
        val url = "https://todoapp-raj.herokuapp.com/api/todo/finished"
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
                            todoAdapter = FinishedAdapter(context as Activity,arrayList,this)
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

    override fun onItemClick(position: Int) {
    }

    override fun onLongItemClick(position: Int) {
    }

    override fun onEditButtonClick(position: Int) {
    }

    override fun onDeleteButtonClick(position: Int) {
        showDeleteDialog(arrayList[position].id,position)
    }

    private fun showDeleteDialog(id: String, position: Int) {

        val dialog : AlertDialog = AlertDialog.Builder(activity as Context)
            .setTitle("|| Delete Task Confirmation ||")
            .setPositiveButton("Yes",null)
            .setNegativeButton("No",null)
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
    }
}