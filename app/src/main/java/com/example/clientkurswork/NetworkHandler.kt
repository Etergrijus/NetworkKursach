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
import java.net.SocketTimeoutException

enum class ScenarioName {
    CONNECT,
    DISCONNECT
}

class NetworkHandler(private val context: Context) {
    private val serverAddress = "10.0.2.2"

    private val serverPort = 12345

    private lateinit var socket: Socket

    private lateinit var input: BufferedReader

    private lateinit var output: PrintWriter

    @Volatile private var isRunning = true

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun connectToServer(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(serverAddress, serverPort), 5000)
                Log.d("Network", "Socket connected")

                if (isConnected()) {
                    output = PrintWriter(socket.getOutputStream(), true)
                    output.println("CONNECT $username\n")
                    Log.d("Network", "Username sent")

                    input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val response = input.readLine()
                    Log.d("Network", "Answer got")

                    responseCheck(response, ScenarioName.CONNECT)
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
            isRunning = false
            try {
                val s = StringBuilder("DISCONNECT $roomId $username\n")
                output.println(s)
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
                while (isRunning) {
                        val message = input.readLine()
                        if (message != null) {
                            Log.d("Client", "Received: $message, $isRunning")
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

    private fun parseMessage(message: String, onUpdate: (String, String) -> Unit) {
        val parts = message.split(" ", "\n")
        when (val prefix = parts[0]) {
           context.getString(R.string.disconnectOK) -> {
               try {
                   if (responseCheck(prefix, ScenarioName.DISCONNECT)) {
                       Log.d("Network", "Ack got, socket closed")
                       socket.close()
                   } else {
                       Log.d(
                           "Network", "Did not receive DISCONNECT_OK," +
                                   "or received an error"
                       )
                   }
               } catch (e: Exception) {
                   Log.d("Network", "Bad sending of disconnection data: ${e.message}")
                   throw Exception(e.message)
               }
           }

            context.getString(R.string.joinMessage),
            context.getString(R.string.leaveMessage) -> {
                onUpdate(parts[0], parts[1])
            }

            else -> {
                Log.d("Client", "Unexpected message")
            }
       }
    }

    private fun responseCheck(response: String, scenario: ScenarioName): Boolean {
        val trueAnswer = when (scenario) {
            ScenarioName.CONNECT -> context.getString(R.string.trueServerAnswer)
            ScenarioName.DISCONNECT -> context.getString(R.string.disconnectOK)
        }

        Log.d(
            "Network",
            "Response: '$response', Expected: '$trueAnswer'"
        )
        return response == trueAnswer
    }

    fun isConnected(): Boolean {
        return socket != null && socket.isConnected && !socket.isClosed
    }
}