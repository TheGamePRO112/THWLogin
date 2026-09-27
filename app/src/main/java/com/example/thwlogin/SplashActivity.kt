package com.example.thwlogin

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        // HIER könnten Sie initialen Code ausführen, der vor der Animation
        // abgeschlossen sein muss (z.B. eine Konfiguration laden).
        // In diesem einfachen Fall startet die Animation sofort.

        val progressBar: ProgressBar = findViewById(R.id.loading_spinner)

        // Animation für den Ladebalken erstellen
        val animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        animation.duration = 2500 // 2.5 Sekunden
        animation.interpolator = DecelerateInterpolator()

        // Einen Listener hinzufügen, der auf das Ende der Animation reagiert.
        // Das ersetzt den separaten Handler.
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                // Optional: Code, der beim Start der Animation ausgeführt wird.
            }

            override fun onAnimationEnd(animator: Animator) {
                // Diese Methode wird aufgerufen, sobald die Animation beendet ist.
                // Jetzt wechseln wir zur MainActivity.
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                // Beende den Splash Screen, damit man nicht mit "zurück" hierherkommt.
                finish()
            }

            override fun onAnimationCancel(animator: Animator) {
                // Optional: Code für den Fall, dass die Animation abgebrochen wird.
            }

            override fun onAnimationRepeat(animator: Animator) {
                // Optional: Code für sich wiederholende Animationen.
            }
        })

        // Starte die Animation
        animation.start()
    }
}