package com.rajat.todo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rajat.todo.UtilsService.UtilService
import org.json.JSONException
import org.json.JSONObject

class Register : AppCompatActivity() {

    lateinit var etName : TextInputEditText
    lateinit var etEmail : TextInputEditText
    lateinit var etPassword : TextInputEditText
    lateinit var btnSignup : MaterialButton

    var name = ""
    var email = ""
    var password = ""

    lateinit var sharedPreferenceClass : SharedPreferenceClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignup = findViewById(R.id.btnSignup)

        val utilService = UtilService()

        sharedPreferenceClass = SharedPreferenceClass(this@Register)

        btnSignup.setOnClickListener {

            utilService.hideKeyboard(it,this@Register)
            name = etName.text.toString()
            email = etEmail.text.toString()
            password = etPassword.text.toString()

            if(TextUtils.isEmpty(name)){
                utilService.showSnackBar(it,"Please enter Name")
            }
            else if(TextUtils.isEmpty(email)){
                utilService.showSnackBar(it,"Please enter Email")
            }
            else if(TextUtils.isEmpty(password)){
                utilService.showSnackBar(it,"Please enter Password")
            }
            else{
                register(it)
            }
        }
    }

    private fun register(view : View){
//        val hashMap : HashMap<String,String> = HashMap()
//
//        hashMap.put("username",name)
//        hashMap.put("email",email)
//        hashMap.put("password",password)

        val params = JSONObject()

        params.put("username",name)
        params.put("email",email)
        params.put("password",password)

        val api = "https://todoapp-raj.herokuapp.com/api/todo/auth/register"

        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.POST,api,params,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        val token = it.getString("token")

                        sharedPreferenceClass.setValue_string("token",token)

                        Toast.makeText(this@Register,token,Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@Register,MainActivity::class.java))
                    }
                }catch (e:JSONException){
                    e.printStackTrace()
                }
            },Response.ErrorListener {
                val response = it.networkResponse
                if(it is ServerError && response!=null){
                    try {
                        val res = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers,"utf-8")))

                        val obj = JSONObject(res)
                        Toast.makeText(this@Register,obj.getString("msg"),Toast.LENGTH_SHORT).show()
                    }catch (e:JSONException){
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
        val policy = DefaultRetryPolicy(socketTime,DefaultRetryPolicy.DEFAULT_MAX_RETRIES,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonObjectRequest.retryPolicy = policy

        //sending queue to the server
        val queue = Volley.newRequestQueue(this@Register)
        queue.add(jsonObjectRequest)
    }

    override fun onStart() {
        super.onStart()

        val todo_pref = getSharedPreferences("user_todo", MODE_PRIVATE)
        if(todo_pref.contains("token"))
        {
            startActivity(Intent(this@Register,MainActivity::class.java))
            finish()
        }
    }
}