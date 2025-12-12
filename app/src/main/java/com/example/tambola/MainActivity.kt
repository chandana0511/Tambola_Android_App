package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupProfileIcon()

        findViewById<View>(R.id.btnHost).setOnClickListener {
            val intent = Intent(this, RoomCreationActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnJoin).setOnClickListener {
            // Updated to navigate to RoomJoinActivity
            val intent = Intent(this, RoomJoinActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupProfileIcon() {
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        ivProfile.setOnClickListener { view ->
            showProfileMenu(view)
        }
    }

    private fun showProfileMenu(view: View) {
        val popup = PopupMenu(this, view)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: "User"

        // Add user email as a disabled item (header)
        popup.menu.add(0, 0, 0, email).apply {
            isEnabled = false
        }

        // Add Logout option
        popup.menu.add(0, 1, 1, "Logout")

        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                logout()
                true
            } else {
                false
            }
        }
        popup.show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
