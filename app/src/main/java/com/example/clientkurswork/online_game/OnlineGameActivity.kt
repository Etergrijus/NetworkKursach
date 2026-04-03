package com.example.clientkurswork.online_game

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clientkurswork.MyApp
import com.example.clientkurswork.R
import com.example.clientkurswork.databinding.ActivityGameBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnlineGameActivity : AppCompatActivity() {
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
        setContentView(binding.root)

        //Получаем данные из интентов - имя пользователя, ID комнаты,
        //список игроков
        val username = this.intent.getStringExtra("Username").toString()
        Log.d("OnlineGameActivity", "Your name is $username")
        roomId = intent.getIntExtra("RoomID", -1)
        Log.d("OnlineGameActivity", "Your room number is $roomId")
        players = this.intent.getStringArrayListExtra("Usernames")!!
        activePlayer = players[0]

        val app = application as MyApp
        netHandler = app.netHandler

        listenGameMessages()

        binding.makeMoveButton.setOnClickListener {
            if (username != activePlayer)
                return@setOnClickListener

            lifecycleScope.launch(Dispatchers.IO) {
                netHandler.sendMoveMessage(roomId)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun listenGameMessages() {
        netHandler.hearGame { eventType, username, result ->
            lifecycleScope.launch(Dispatchers.Main) {
                when (eventType) {
                    baseContext.getString(R.string.rolled) -> {
                        Log.d("OnlineGameActivity", "Dice rolled by $username: $result")
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