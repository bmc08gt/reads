package com.bmcreations.bookinfo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bmcreations.bookinfo.extensions.getViewModel
import com.bmcreations.bookinfo.extensions.isSignedIn
import com.bmcreations.bookinfo.ui.camera.CameraViewfinder
import com.bmcreations.bookinfo.ui.impl.ActivityNavigation
import com.bmcreations.bookinfo.ui.login.LoginViewModel
import com.bmcreations.bookinfo.ui.login.LoginViewModel.State.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


class MainActivity : AppCompatActivity(), ActivityNavigation, AnkoLogger {

    private val loginVm by lazy {
        getViewModel { LoginViewModel.create() }
    }

    var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)

        navView.setupWithNavController(navController)

        loginVm.startActivityForResultEvent.setEventReceiver(this, this)

        if (!isSignedIn) {
            observe()
            loginVm.authenticate()
        } else {
            info { "Current user found" }
        }

        look_up_book.setOnClickListener {
            Intent(this@MainActivity, CameraViewfinder::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun observe() {
        loginVm.authState.observe(this, Observer {
            when (it) {
                is Authenticated -> info { "Authenticated successfully" }
                is Unauthenticated -> info { "Not authenticated" }
                is InvalidAuthentication -> info { "Invalid authentication -- ${it.cause?.localizedMessage}" }
            }
        })
    }


    override fun onSupportNavigateUp() = navController?.navigateUp() ?: false
}
