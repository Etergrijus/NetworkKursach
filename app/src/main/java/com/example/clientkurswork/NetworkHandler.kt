package com.example.clientkurswork

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

class NetworkHandler(private val context: Context) {
    private val serverAddress = "109.62.178.87"

    private val serverPort = 12345

    private lateinit var socket: Socket

    private lateinit var input: BufferedReader

    private lateinit var output: PrintWriter

    @Volatile
    private var isServerListeningRunning = false

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun connectToServer(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(serverAddress, serverPort), 5000)
                Log.d("Network", "Socket connected")

                if (isConnected()) {
                    output = PrintWriter(socket.getOutputStream(), true)
                    output.println("${context.getString(R.string.connectStr)} $username\n")
                    Log.d("Network", "Username sent")

                    input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val response = input.readLine()
                    Log.d("Network", "Answer got")

                    response == context.getString(R.string.trueServerAnswer)
                } else {
                    Log.d("Network", "Don't can connect the socket")
                    false
                }
            } catch (e: SocketException) {
                Log.d("Network", "Error on socket connection: ${e.message}")
                false
            } catch (e: Exception) {
                Log.e("Network", "Unexpected error: ${e.message}", e)
                false
            }
        }
    }

    suspend fun getRoomInfo(): String {
        return withContext(Dispatchers.IO) {
            if (isConnected()) {
                Log.d("Network", "I must got the room_info...")
                val response = input.readLine()
                Log.d("Network", "Received: $response")

                if (response.startsWith(context.getString(R.string.correctRoomInfo))) {
                    isServerListeningRunning = true
                    response
                } else {
                    Log.d("Network", "Error of getting room info")
                    throw Exception("Incorrect room message")
                }
            } else {
                Log.d("Network", "Socket is bad")
                throw Exception("Socket closed unexpected")
            }
        }
    }

    suspend fun disconnectFromServer(roomId: Int, username: String) {
        withContext(Dispatchers.IO) {
            isServerListeningRunning = false
            try {
                output.println("${context.getString(R.string.disconnectStr)} $roomId $username\n")
                //Обработка ответа от сервера вынесена слушателю
            } catch (e: SocketException) {
                Log.d("Network", "Error on socket disconnection: ${e.message}")
                throw SocketException()
            } catch (e: Exception) {
                Log.d("Network", "Bad sending of disconnection data: ${e.message}")
                throw Exception(e.message)
            }
        }
    }

    fun hearServer(onUpdate: (eventType: String, username: String) -> Unit) {
        scope.launch {
            try {
                while (isServerListeningRunning) {
                    val message = input.readLine()
                    if (message != null) {
                        Log.d("Client", "Received: $message")
                        parseMessage(message, onUpdate)
                    } else {
                        Log.d("Client", "Server disconnected")
                        break
                    }
                }
                Log.d("Client", "Potential-endless loop closed successfully")
            } catch (e: Exception) {
                Log.d("Client", "Error in read loop: ${e.message}")
            }
        }
    }

    suspend fun changeRoom(roomId: Int, username: String) {
        withContext(Dispatchers.IO) {
            isServerListeningRunning = false
            output.println("${context.getString(R.string.denialStr)} $roomId $username\n")
            //Далее снова работаем с сетью линейно, слушатель закрыт
        }
    }

    suspend fun initialVoting(roomId: Int, username: String) {
        withContext(Dispatchers.IO) {
            output.println("${context.getString(R.string.startVotingStr)} $roomId $username\n")
            sendVoteAnswer(roomId, username, context.getString(R.string.acceptStr))
            //Ждём ответов от будущих соперников
        }
    }

    suspend fun sendVoteAnswer(roomId: Int, username: String, message: String) {
        withContext(Dispatchers.IO) {
            Log.d("Network", "My answer - $message")
            output.println("$message $roomId $username")
            output.flush()
        }
    }

    private fun parseMessage(message: String, onUpdate: (String, String) -> Unit) {
        val parts = message.split(" ", "\n")
        when (val prefix = parts[0]) {
            context.getString(R.string.disconnectOK) -> {
                try {
                    Log.d("Network", "Ack got, socket closed")
                    socket.close()
                } catch (e: Exception) {
                    Log.d("Network", "Bad sending of disconnection data: ${e.message}")
                    throw Exception(e.message)
                }
            }

            context.getString(R.string.joinMessage),
            context.getString(R.string.leaveMessage),
            context.getString(R.string.needVoting),
            context.getString(R.string.accepted),
            context.getString(R.string.declined),
            context.getString(R.string.notStartGame) -> {
                onUpdate(parts[0], parts[1])
            }

            context.getString(R.string.newRoomFinding) -> {
                Log.d("Client", "Ready to change room")
            }

            context.getString(R.string.startGame) -> {
                isServerListeningRunning = false
                onUpdate(parts[0], parts[1])
            }

            else -> {
                Log.d("Client", "Unexpected message")
            }
        }
    }

    fun hearGame(onUpdate: (eventType: String, username: String, result: Int) -> Unit) {
        isServerListeningRunning = true
        scope.launch {
            try {
                while (isServerListeningRunning) {
                    val message = input.readLine()
                    if (message != null) {
                        Log.d("Client", "Received: $message")
                        processServerMessage(message, onUpdate)
                    } else {
                        Log.d("Client", "Server disconnected")
                        break
                    }
                }
                Log.d("Client", "Potential-endless loop closed successfully")
            } catch (e: Exception) {
                Log.d("Client", "Error in read loop: ${e.message}")
            }
        }
    }

    suspend fun sendMoveMessage(roomId: Int) {
        withContext(Dispatchers.IO) {
            Log.d("Game", "${context.getString(R.string.moveStr)} $roomId")
            output.println("${context.getString(R.string.moveStr)} $roomId")
            output.flush()
        }
    }

    private fun processServerMessage(
        message: String,
        onUpdate: (eventType: String, username: String, result: Int) -> Unit
    ) {
        val message_ = message.trimEnd()
        val parts = message_.split(" ")
        if (parts.size == 3 && parts[0] == context.getString(R.string.rolled)) {
            val nick = parts[1]
            val diceResult = parts[2].toInt()
            onUpdate(context.getString(R.string.rolled), nick, diceResult) // Вызываем колбэк
        } else {
            Log.d("Game", "Unexpected message: $message")
        }
    }

    /*suspend fun getDiceResult() : Pair<String, Int> {
        return withContext(Dispatchers.IO) {
            var response = input.readLine()
            response = response.trimEnd()
            val parts = response.split(" ")
            if (parts.size != 3) {
                Log.d("Game", "Unexpected roll dice: $response")
                throw Exception("Unexpected roll dice")
            }

            if (parts[0] == context.getString(R.string.rolled)) {
                val nick = parts[1]
                val diceResult = parts[2].toInt()

                Pair(nick, diceResult)
            } else {
                Log.d("Game", "Unexpected roll dice: $response")
                throw Exception("Unexpected roll dice")
            }
        }
    }*/

    fun isConnected(): Boolean {
        return socket.isConnected && !socket.isClosed
    }
}