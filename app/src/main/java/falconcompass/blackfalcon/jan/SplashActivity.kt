package falconcompass.blackfalcon.jan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(goNext, 2200L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
