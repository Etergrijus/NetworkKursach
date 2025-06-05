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
import android.graphics.Typeface
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.clientkurswork.databinding.FragmentAwaitingBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.swap

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
        binding.returnButton.setOnClickListener {
            signalman?.onReturnOnStart()
        }

        netHandler = NetworkHandler(requireContext())
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
                handleNetworkData(response)
            }

            createServerListener()
        }

        binding.denialButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                netHandler.changeRoom(roomId, username)
                val response = netHandler.getRoomInfo()
                withContext(Dispatchers.Main) {
                    binding.players.removeAllViews()
                    players.clear()
                    handleNetworkData(response)
                }

                createServerListener()
            }
        }

        binding.votingButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                netHandler.initialVoting(roomId, username)
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

    private fun handleNetworkData(response: String) {
        val parts = response.split(" ")
        if (parts.size >= 3) {
            try {
                roomId = parts[1].toInt()
                Log.d("Network", "ID: $roomId")

                val playersNicknames = parts.subList(2, parts.size - 1)
                playersNicknames.forEach { playerName ->
                    createPlayersViews(playerName)
                }
                players.forEach {
                    binding.players.addView(it)
                }
            } catch (e: NumberFormatException) {
                Log.e("Client", "Invalid room info format: ${e.message}")
                throw NumberFormatException()
            }
        } else {
            Log.e("Client", "Invalid room info format")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createPlayersViews(playerName: String) {
        val textView = TextView(requireContext()).apply {
            text = if (playerName != "")
                "• $playerName"
            else
                "•"
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
        players.add(textView)
    }

    private fun createServerListener() {
        netHandler.hearServer { eventType, playerName ->
            lifecycleScope.launch(Dispatchers.Main) {
                when (eventType) {
                    context?.getString(R.string.joinMessage) -> {
                        Log.d("Client", "Player joined: $playerName")
                        for (i in 0 until players.size)
                            if (players[i].text == "•") {
                                addPlayer(i, playerName)
                                break
                            }
                    }

                    context?.getString(R.string.leaveMessage) -> {
                        Log.d("Client", "Player leaved: $playerName")
                        for (i in 0 until players.size)
                            if (players[i].text == "• $playerName") {
                                removePlayer(i)
                                break
                            }
                        binding.players.removeAllViews()
                        players.forEach {
                            binding.players.addView(it)
                        }
                    }

                    context?.getString(R.string.needVoting) -> {
                        showVoteDialog(playerName)
                    }

                    context?.getString(R.string.accepted),
                    context?.getString(R.string.declined) -> {
                        val votedPlayerTextView = players.find { it.text == playerName }
                        votedPlayerTextView?.apply {
                            if (eventType == context?.getString(R.string.accepted))
                                setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.malachite
                                    )
                                )
                            else
                                setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.red
                                    )
                                )
                            setTypeface(typeface, Typeface.BOLD)
                        }
                    }

                    context?.getString(R.string.notStartGame) -> {
                        players.forEach {
                            it.apply {
                                setTextColor(ContextCompat.getColor(
                                    requireContext(),
                                    R.color.rose
                                ))
                                setTypeface(typeface, Typeface.NORMAL)
                            }
                        }
                        binding.players.removeAllViews()
                        players.forEach {
                            binding.players.addView(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addPlayer(index: Int, playerName: String) {
        if (index < players.size)
            players[index].text = "• $playerName"
    }

    private fun removePlayer(index: Int) {
        players[index].text = "•"
        for (i in index until players.size - 1)
            if (players[i + 1].text != "•")
                swap(players, i, i + 1)
    }

    private fun showVoteDialog(initializerPlayerName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.voting))
            .setMessage("$initializerPlayerName ${getString(R.string.votingMessage)}")
            .setPositiveButton(getString(R.string.voteAccept)) { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    netHandler.sendVoteAnswer(roomId, username, getString(R.string.acceptStr))
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.voteDecline)) { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    netHandler.sendVoteAnswer(roomId, username, getString(R.string.declineStr))
                }
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}