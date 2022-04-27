package com.rajat.todo

import android.content.Intent
import android.content.res.Configuration
import android.icu.number.NumberFormatter.with
import android.icu.number.NumberRangeFormatter.with
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    lateinit var btnLogout : MaterialButton
    lateinit var toolbar : Toolbar
    lateinit var drawerLayout : DrawerLayout
    lateinit var drawerToggle : ActionBarDrawerToggle
    lateinit var navigationView: NavigationView
    lateinit var sharedPreferenceClass: SharedPreferenceClass
    lateinit var userName : TextView
    lateinit var userEmail : TextView
    lateinit var avatar : CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferenceClass = SharedPreferenceClass(this@MainActivity)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val hdView = navigationView.getHeaderView(0)
        userName = hdView.findViewById(R.id.username)
        userEmail = hdView.findViewById(R.id.email)
        avatar = hdView.findViewById(R.id.avatar)

        navigationView.setNavigationItemSelectedListener { item ->
            setDrawerClick(item.itemId)
            item.isChecked = true
            drawerLayout.closeDrawers()
            true
        }

        initDrawer()

        getUserProfile()

    }

    private fun getUserProfile() {
        val url = "https://todoapp-raj.herokuapp.com/api/todo/auth"

        val token = sharedPreferenceClass.getValue_string("token")

        val jsonObjectRequest  = object : JsonObjectRequest(
            Request.Method.GET,url,
            null,
            Response.Listener{
                try {
                    if (it.getBoolean("success")) {
                        val user = it.getJSONObject("user")
                        userName.text = user.getString("username")
                        userEmail.text = user.getString("email")
                        Picasso.get().load(user.getString("avatar"))
                            .placeholder(R.drawable.user_dummy)
                            .error(R.drawable.user_dummy)
                            .into(avatar)
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
            }, Response.ErrorListener {
                Toast.makeText(this@MainActivity,"Error Occurred", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
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
        val queue = Volley.newRequestQueue(this@MainActivity)
        queue.add(jsonObjectRequest)
    }

    private fun initDrawer() {
        val manager = supportFragmentManager;
        val ft = manager.beginTransaction()
        ft.replace(R.id.content, HomeFragment())
        ft.commit()

        navigationView.setCheckedItem(R.id.action_home)

        drawerToggle = object: ActionBarDrawerToggle(this@MainActivity,drawerLayout,toolbar,R.string.drawer_open,R.string.drawer_close){
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
            }
        }

        drawerToggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
        drawerLayout.addDrawerListener(drawerToggle)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    private fun setDrawerClick(itemId: Int) {
        when(itemId){
            R.id.action_finished_task -> supportFragmentManager.beginTransaction().replace(R.id.content,FinishedTaskFragment()).commit()
            R.id.action_home -> supportFragmentManager.beginTransaction().replace(R.id.content,HomeFragment()).commit()
            R.id.action_logout -> {
                sharedPreferenceClass.clearContent()
                startActivity(Intent(this@MainActivity,Login::class.java))
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.action_share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                val msg = "Hey Share this ToDo App"

                intent.putExtra(Intent.EXTRA_TEXT,msg)
                startActivity(Intent.createChooser(intent,"Share Via"))

                return true
            }

            R.id.refresh_menu -> {
                supportFragmentManager.beginTransaction().replace(R.id.content,HomeFragment()).commit()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}