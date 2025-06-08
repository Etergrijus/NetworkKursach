package com.example.clientkurswork

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clientkurswork.databinding.ActivityGameBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding

    private var players: ArrayList<String> = arrayListOf()

    private var roomId: Int = -1

    private lateinit var netHandler: NetworkHandler

    private lateinit var activePlayer: String

    private var diceRoll = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)

        val username = this.intent.getStringExtra("Username").toString()
        Log.d("GameActivity", username)

        roomId = intent.getIntExtra("RoomID", -1)

        setContentView(binding.root)

        players = this.intent.getStringArrayListExtra("Usernames")!!
        activePlayer = players[0]

        val app = application as MyApp
        netHandler = app.netHandler

        getMoves()

        binding.makeMoveButton.setOnClickListener {
            if (username != activePlayer)
                return@setOnClickListener

            lifecycleScope.launch(Dispatchers.IO) {
                netHandler.sendMoveMessage(roomId)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getMoves() {
        netHandler.hearGame { eventType, username, result ->
            lifecycleScope.launch(Dispatchers.Main) {
                when (eventType) {
                    baseContext.getString(R.string.rolled) -> {
                        Log.d("GameActivity", "Dice rolled by $username: $result")
                        activePlayer = username
                        diceRoll = result

                        binding.currentPlayerText.text =
                            baseContext.getString(R.string.currentPlayer) + activePlayer
                        binding.diceResultText.text =
                            baseContext.getString(R.string.diceResult) + diceRoll.toString()
                    }
                }
            }
        }
    }
}