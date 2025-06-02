package com.example.clientkurswork

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

class NetworkHandler(private val context: Context) {
    private val serverAddress = "10.0.2.2"
    private val serverPort = 12345
    private lateinit var socket: Socket

    suspend fun connectToServer(username: String) : Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(serverAddress, serverPort), 5000)
                Log.d("Network", "Socket connected")

                if (socket.isConnected) {
                    val output = PrintWriter(socket.getOutputStream(), true)
                    output.println(username)
                    Log.d("Network", "Username sent")

                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val response = input.readLine()
                    Log.d("Network", "Answer got")

                    responseCheck(response)
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
            } finally {
                socket.close()
            }
        }
    }

    private fun responseCheck(response: String) : Boolean {
        val trueAnswer = context.getString(R.string.trueServerAnswer)
        Log.d("Network",
            "Response: '$response', Expected: '$trueAnswer'")
        return response == trueAnswer
    }
}