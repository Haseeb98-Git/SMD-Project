package com.haseebali.savelife

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Make both the layout and image clickable
        val rootLayout = findViewById<View>(R.id.root_layout)
        val splashLogo = findViewById<ImageView>(R.id.splash_logo)

        val clickListener = View.OnClickListener {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }

        rootLayout?.setOnClickListener(clickListener)
        splashLogo.setOnClickListener(clickListener)
    }
}