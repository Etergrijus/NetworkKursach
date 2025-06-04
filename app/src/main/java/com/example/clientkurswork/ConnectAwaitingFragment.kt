package com.example.clientkurswork

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.clientkurswork.databinding.FragmentAwaitingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectAwaitingFragment(private val username: String) : Fragment() {
    private lateinit var binding: FragmentAwaitingBinding

    private var signalman: FragmentActivitySignalman? = null

    private lateinit var netHandler: NetworkHandler

    private var roomId = -1

    private val players = mutableListOf<TextView>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            signalman = context as FragmentActivitySignalman
        } catch (e: ClassCastException) {
            Log.d("DataPass", "Error")
            throw ClassCastException("$context must implement FragmentActivitySignalman")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAwaitingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ResourceAsColor")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.statusText.setText(R.string.awaitingNetwork)

        netHandler = NetworkHandler(requireContext())

        binding.returnButton.setOnClickListener {
            signalman?.onReturnOnStart()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = netHandler.connectToServer(username)
            withContext(Dispatchers.Main) {
                Log.d("Network", "Responce is $success")
                if (success) {
                    binding.statusText.setText(R.string.awaitingPlayers)
                } else {
                    throw Exception("Error of work with server, stopped")
                }
            }

            val response = netHandler.getRoomInfo()
            withContext(Dispatchers.Main) {
                val parts = response.split(" ")
                if (parts.size >= 3) {
                    try {
                        roomId = parts[1].toInt()
                        Log.d("Network", "ID: $roomId")

                        //players.addAll(parts.subList(2, parts.size - 1))
                        val playersNicknames = parts.subList(2, parts.size - 1)
                        playersNicknames.forEach {playerName ->
                            showNetworkData(playerName)
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("Client", "Invalid room info format: ${e.message}")
                        throw NumberFormatException()
                    }

                    //showNetworkData(parts)
                } else {
                    Log.e("Client", "Invalid room info format")
                }
            }

            netHandler.hearServer {eventType, username_ ->
                lifecycleScope.launch(Dispatchers.Main) {
                    when(eventType) {
                        "JOIN" -> {
                            Log.d("Client", "Player joined: $username_")
                            for (i in 0 until players.size)
                                if (players[i].text.isNullOrEmpty()) {
                                    updatePlayers(i, username_)
                                    break
                                }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("Connect Fragment", "onDestroy called")

        if (netHandler.isConnected())
            Log.d("Connect Fragment", "Disconnect is awaiting")
            GlobalScope.launch(Dispatchers.IO) {
                Log.d("Connect Fragment", "thread?")
                netHandler.disconnectFromServer(roomId, username)
            }
    }

    override fun onDetach() {
        super.onDetach()
        signalman = null
    }

    @SuppressLint("SetTextI18n")
    private fun showNetworkData(playerName: String) {
        val textView = TextView(requireContext()).apply {
            text = "• $playerName"
            textSize = 24f
            gravity = Gravity.START
            setTextColor(ContextCompat.getColor(requireContext(), R.color.rose))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(40, 0, 0, 20)
            }
            layoutParams = params
        }
        binding.players.addView(textView)
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlayers(index: Int, playerName: String) {
        if (index < players.size)
            players[index].text = "• $playerName"
    }
}