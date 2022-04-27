package com.rajat.todo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.ServerError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rajat.todo.UtilsService.UtilService
import org.json.JSONException
import org.json.JSONObject

class Login : AppCompatActivity() {

    lateinit var etEmail : TextInputEditText
    lateinit var etPassword : TextInputEditText
    lateinit var btnLogin : MaterialButton

    var email = ""
    var password = ""

    lateinit var loginToReg : TextView
    lateinit var sharedPreferenceClass : SharedPreferenceClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginToReg = findViewById(R.id.loginToReg)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        val utilService = UtilService()

        sharedPreferenceClass = SharedPreferenceClass(this@Login)

        btnLogin.setOnClickListener {

            utilService.hideKeyboard(it,this@Login)
            email = etEmail.text.toString()
            password = etPassword.text.toString()

            if(TextUtils.isEmpty(email)){
                utilService.showSnackBar(it,"Please enter Email")
            }
            else if(TextUtils.isEmpty(password)){
                utilService.showSnackBar(it,"Please enter Password")
            }
            else{
                login(it)
            }
        }

        loginToReg.setOnClickListener{
            val intent = Intent(this@Login,Register::class.java)
            startActivity(intent)
        }
    }

    private fun login(view : View){

        val params = JSONObject()
        params.put("email",email)
        params.put("password",password)

        val api = "https://todoapp-raj.herokuapp.com/api/todo/auth/login"

        val jsonObjectRequest  = object : JsonObjectRequest(Request.Method.POST,api,params,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        val token = it.getString("token")

                        sharedPreferenceClass.setValue_string("token",token)

                        Toast.makeText(this@Login,token, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@Login,MainActivity::class.java))
                        finish()
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
                        Toast.makeText(this@Login,obj.getString("msg"), Toast.LENGTH_SHORT).show()
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
        val queue = Volley.newRequestQueue(this@Login)
        queue.add(jsonObjectRequest)
    }

    override fun onStart() {
        super.onStart()

        val todo_pref = getSharedPreferences("user_todo", MODE_PRIVATE)
        if(todo_pref.contains("token"))
        {
            startActivity(Intent(this@Login,MainActivity::class.java))
            finish()
        }
    }
}