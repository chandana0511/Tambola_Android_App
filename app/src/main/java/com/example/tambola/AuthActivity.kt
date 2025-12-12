package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnAction: Button
    private lateinit var tvToggleMode: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var tvSubtitle: TextView

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (auth.currentUser != null) {
            goToMainActivity()
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnAction = findViewById(R.id.btnAction)
        tvToggleMode = findViewById(R.id.tvToggleMode)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvSubtitle = findViewById(R.id.tvSubtitle)

        btnAction.setOnClickListener {
            handleAuthAction()
        }

        tvToggleMode.setOnClickListener {
            toggleMode()
        }

        tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        if (isLoginMode) {
            tvWelcome.text = "Welcome Back"
            tvSubtitle.text = "Sign in to continue"
            btnAction.text = "Login"
            tvToggleMode.text = "New here? Create Account"
            tvForgotPassword.visibility = View.VISIBLE
        } else {
            tvWelcome.text = "Create Account"
            tvSubtitle.text = "Join the fun!"
            btnAction.text = "Sign Up"
            tvToggleMode.text = "Already have an account? Login"
            tvForgotPassword.visibility = View.GONE
        }
    }

    private fun handleForgotPassword() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val userEmail = view.findViewById<TextInputEditText>(R.id.etResetEmail)

        builder.setView(view)
        builder.setPositiveButton("Reset", null) // Set null here to override later
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        // Override the positive button onClick to prevent the dialog from closing automatically
        // if the validation fails (empty email).
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = userEmail.text.toString().trim()
            if (email.isEmpty()) {
                userEmail.error = "Email is required"
                userEmail.requestFocus()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun handleAuthAction() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!email.endsWith("@blauplug.com", ignoreCase = true)) {
            Toast.makeText(this, "Only Blauplug employees are allowed", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnAction.isEnabled = false

        if (isLoginMode) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    if (task.isSuccessful) {
                        goToMainActivity()
                    } else {
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        toggleMode()
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear back stack so user can't go back to auth screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}