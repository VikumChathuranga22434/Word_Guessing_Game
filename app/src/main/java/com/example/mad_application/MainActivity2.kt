package com.example.mad_application

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var wordToGuess: String = ""
    private var score: Int = 100
    private var attempts: Int = 0
    private var level: Int = 1
    private var timeTaken: Long = 0
    private lateinit var timer: CountDownTimer

    private lateinit var tvWordHint: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var etGuess: EditText
    private lateinit var etLetter: EditText
    private lateinit var btnGuess: Button
    private lateinit var btnNewWord: Button
    private lateinit var btnLetterCount: Button
    private lateinit var btnWordLength: Button
    private lateinit var btnTip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val playerName = sharedPreferences.getString("player_name", "")
        if (playerName.isNullOrEmpty()) {
            showNameInputDialog()
        }

        // Bind UI elements
        tvWordHint = findViewById(R.id.tvWordHint)
        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        etGuess = findViewById(R.id.etGuess)
        etLetter = findViewById(R.id.etLetter)
        btnGuess = findViewById(R.id.btnGuess)
        btnNewWord = findViewById(R.id.btnNewWord)
        btnLetterCount = findViewById(R.id.btnLetterCount)
        btnWordLength = findViewById(R.id.btnWordLength)
        btnTip = findViewById(R.id.btnTip)

        // Fetch initial word and start game
        fetchRandomWord()
        startTimer()

        // Guess button logic
        btnGuess.setOnClickListener {
            val guess = etGuess.text.toString().lowercase()
            if (guess.isEmpty()) {
                Toast.makeText(this, "Enter a guess!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            attempts++
            if (guess == wordToGuess) {
                timer.cancel()
                tvWordHint.text = "Correct! Word was '$wordToGuess'. Time: ${timeTaken}s"
                levelUp()
            } else {
                score -= 10
                updateScore()
                tvWordHint.text = "Wrong guess! Attempts: $attempts"
                checkGameOver()
            }
            etGuess.text.clear()
        }

        // New word button
        btnNewWord.setOnClickListener {
            resetGame()
        }

        // Letter count button
        btnLetterCount.setOnClickListener {
            val letter = etLetter.text.toString().lowercase()
            if (letter.length != 1 || !letter[0].isLetter()) {
                Toast.makeText(this, "Enter a single letter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            score -= 5
            updateScore()
            val count = wordToGuess.count { it == letter[0] }
            tvWordHint.text = "'$letter' appears $count times."
            etLetter.text.clear()
            checkGameOver()
        }

        // Word length button
        btnWordLength.setOnClickListener {
            score -= 5
            updateScore()
            tvWordHint.text = "Word has ${wordToGuess.length} letters."
            checkGameOver()
        }

        // Tip button (available after 5 attempts)
        btnTip.setOnClickListener {
            if (attempts < 5) {
                Toast.makeText(this, "Available after 5 attempts!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            score -= 5
            updateScore()
            tvWordHint.text = "Tip: Rhymes with '${getRhymingWord()}'"
            checkGameOver()
        }
    }

    private fun showNameInputDialog() {
        val editText = EditText(this)
        android.app.AlertDialog.Builder(this)
            .setTitle("Welcome!")
            .setMessage("Please enter your name:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().ifEmpty { "Player1" }
                sharedPreferences.edit().putString("player_name", name).apply()
                Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun fetchRandomWord() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://random-word-api.herokuapp.com/word?length=${4 + level}") // Increase length with level
                    .build()
                val response = client.newCall(request).execute()
                val jsonData = response.body?.string()
                val wordArray = JSONArray(jsonData)
                wordToGuess = wordArray.getString(0).lowercase()

                withContext(Dispatchers.Main) {
                    tvWordHint.text = "Guess the word! (Level $level)"
                    updateScore()
                    startTimer()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvWordHint.text = "Error fetching word. Try again."
                    Toast.makeText(this@MainActivity, "Check internet!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startTimer() {
        timeTaken = 0
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeTaken++
                tvTimer.text = "Time: ${timeTaken}s"
            }
            override fun onFinish() {}
        }.start()
    }

    private fun updateScore() {
        tvScore.text = "Score: $score (Attempts: $attempts)"
    }

    private fun checkGameOver() {
        if (score <= 0 || attempts >= 10) {
            timer.cancel()
            tvWordHint.text = "Game Over! Word was '$wordToGuess'. Starting new game..."
            resetGame()
        }
    }

    private fun levelUp() {
        level++
        Toast.makeText(this, "Level Up! New word incoming...", Toast.LENGTH_SHORT).show()
        resetGame()
    }

    private fun resetGame() {
        score = 100
        attempts = 0
        timer.cancel()
        fetchRandomWord()
    }

    private fun getRhymingWord(): String {
        // Simplified: In a real app, use an API like https://api-ninjas.com/api/rhyme
        return when (wordToGuess) {
            "cat" -> "hat"
            "dog" -> "fog"
            else -> "similar"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}