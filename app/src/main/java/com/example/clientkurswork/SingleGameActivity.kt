package com.example.clientkurswork

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.PlayerModel
import com.example.clientkurswork.ui.theme.ClientKursWorkTheme
import com.example.clientkurswork.views.Main
import java.util.LinkedList
import java.util.Queue

class SingleGameActivity : AppCompatActivity() {
    private lateinit var username: String

    @SuppressLint("ConfigurationScreenWidthHeight")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        username = intent.getStringExtra("Username").toString()
        Log.d("SingleGameActivity", "Username is $username")

        var isPortrait = true
        if (isPortrait) {
            isPortrait = false
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }


        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val playersQueue = createPlayersQueue(username)
            val gameVMFactory = GameViewModelFactory(playersQueue)
            val viewModel: GameViewModel = viewModel(factory = gameVMFactory)
            Main(viewModel)
        }
    }

    private fun createPlayersQueue(humanPlayerUsername: String): Queue<PlayerModel> {
        val queue: Queue<PlayerModel> = LinkedList(listOf())
        queue.add(PlayerModel(humanPlayerUsername, Player.BLUE))

        val usedUsernamesOfAI: MutableList<Int> = mutableListOf()
        val onBoardPlayers: Set<Player> = setOf(Player.YELLOW, Player.RED, Player.GREEN)
        val iterOnBoardPlayers = onBoardPlayers.iterator()

        while (usedUsernamesOfAI.size < 3) {
            val applicantUsernameIndex = namesForAI.indices.random()
            if (applicantUsernameIndex !in usedUsernamesOfAI) {
                queue.add(
                    PlayerModel(
                        namesForAI[applicantUsernameIndex],
                        iterOnBoardPlayers.next()
                    )
                )
                usedUsernamesOfAI.add(applicantUsernameIndex)
            }
        }
        return queue
    }

    private val namesForAI = listOf(
        "Быстрый Гонсалес", "Мигель", "Алонсо", "Родригес",
        "Хуан", "Диего Армандо", "Кортес", "Писарро",
        "Габриэль", "Хесус", "Пабло", "Эктор", "Санчес",
        "Мия", "Эль Локо", "Эль Дьябло", "Игнасио", "Хосе",
        "Баск", "Хосеп"
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClientKursWorkTheme {
        Greeting("Android")
    }
}

