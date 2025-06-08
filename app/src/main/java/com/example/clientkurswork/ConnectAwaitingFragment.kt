package com.example.clientkurswork

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.clientkurswork.databinding.FragmentAwaitingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.swap

class ConnectAwaitingFragment(private val username: String) : Fragment() {
    private lateinit var binding: FragmentAwaitingBinding

    private var signalman: FragmentActivitySignalman? = null

    private lateinit var netHandler: NetworkHandler

    private var roomId = -1

    private var players = mutableListOf<PlayerData>()

    private val app: MyApp by lazy {
        requireActivity().application as MyApp
    }

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

        netHandler = app.netHandler

        binding.statusText.setText(R.string.awaitingNetwork)
        binding.returnButton.setOnClickListener {
            signalman?.onReturnOnStart()
            if (netHandler.isConnected())
                Log.d("Connect Fragment", "Disconnect is awaiting")
            lifecycleScope.launch(Dispatchers.IO) {
                Log.d("Connect Fragment", "thread?")
                netHandler.disconnectFromServer(roomId, username)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = netHandler.connectToServer(username)
            withContext(Dispatchers.Main) {
                Log.d("Network", "Response is $success")
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
            binding.denialButton.isEnabled = false
            binding.denialButton.isClickable = false
            lifecycleScope.launch(Dispatchers.IO) {
                netHandler.changeRoom(roomId, username)
                val response = netHandler.getRoomInfo()
                withContext(Dispatchers.Main) {
                    binding.players.removeAllViews()
                    players.clear()
                    handleNetworkData(response)

                    binding.denialButton.isEnabled = true
                    binding.denialButton.isClickable = true
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

                players = MutableList(parts.size - 2) { PlayerData("") }
                var player = 0
                for (i in 2 until parts.size - 1) {
                    players[player++].username = parts[i]
                }
                updatePlayersViews()
            } catch (e: NumberFormatException) {
                Log.e("Client", "Invalid room info format: ${e.message}")
                throw NumberFormatException()
            }
        } else {
            Log.e("Client", "Invalid room info format")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePlayersViews() {
        if (binding.players.isNotEmpty())
            binding.players.removeAllViews()

        for (playerData in players) {
            val textView = TextView(requireContext()).apply {
                text = if (playerData.username != "")
                    "• ${playerData.username}"
                else
                    "•"
                textSize = 24f
                gravity = Gravity.START
                val (textColor, textStyle) = when (playerData.voteResult) {
                    VoteMessage.NOTHING ->
                        Pair(
                            ContextCompat.getColor(requireContext(), R.color.rose),
                            Typeface.NORMAL
                        )

                    VoteMessage.ACCEPTED ->
                        Pair(
                            ContextCompat.getColor(requireContext(), R.color.malachite),
                            Typeface.BOLD
                        )

                    VoteMessage.DECLINED ->
                        Pair(
                            ContextCompat.getColor(requireContext(), R.color.red),
                            Typeface.BOLD
                        )
                }

                setTextColor(textColor)
                setTypeface(null, textStyle)

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
    }

    private fun createServerListener() {
        netHandler.hearServer { eventType, playerName ->
            lifecycleScope.launch(Dispatchers.Main) {
                when (eventType) {
                    context?.getString(R.string.joinMessage) -> {
                        Log.d("Client", "Player joined: $playerName")
                        for (i in 0 until players.size)
                            if (players[i].username == "") {
                                addPlayer(i, playerName)
                                break
                            }
                        updatePlayersViews()
                    }

                    context?.getString(R.string.leaveMessage) -> {
                        Log.d("Client", "Player leaved: $playerName")
                        for (i in 0 until players.size)
                            if (players[i].username == playerName) {
                                removePlayer(i)
                                break
                            }
                        updatePlayersViews()
                    }

                    context?.getString(R.string.needVoting) -> {
                        showVoteDialog(playerName)
                    }

                    context?.getString(R.string.accepted) -> {
                        val index = players.indexOfFirst { it.username == playerName }
                        if (index != -1) {
                            players[index].voteResult = VoteMessage.ACCEPTED
                            updatePlayersViews()
                        }
                    }
                    context?.getString(R.string.declined) -> {
                        val index = players.indexOfFirst { it.username == playerName }
                        if (index != -1) {
                            players[index].voteResult = VoteMessage.DECLINED
                            updatePlayersViews()
                        }
                    }
                    context?.getString(R.string.notStartGame) -> {
                        delay(1000)
                        players.forEach {
                            it.voteResult = VoteMessage.NOTHING
                        }
                        updatePlayersViews()
                    }

                    context?.getString(R.string.startGame) -> {
                        delay(1000)
                        Log.d("Client", "We are starting the game!!")
                        val intent = Intent(binding.root.context, GameActivity::class.java)
                        intent.putExtra("Username", username)
                        intent.putExtra("RoomID", roomId)

                        val usernames = ArrayList<String>()
                        players.forEach {
                            usernames.add(it.username)
                        }

                        intent.putStringArrayListExtra("Usernames", usernames)

                        binding.root.context.startActivity(intent)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addPlayer(index: Int, playerName: String) {
        if (index < players.size)
            players[index].username = playerName
    }

    private fun removePlayer(index: Int) {
        players[index].username = ""
        for (i in index until players.size - 1)
            if (players[i + 1].username != "")
                swap(players, i, i + 1)
    }

    private fun showVoteDialog(initializerPlayerName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.voting))
            .setMessage("$initializerPlayerName ${getString(R.string.votingMessage)}")
            .setPositiveButton(getString(R.string.voteAccept)) { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    netHandler.sendVoteAnswer(roomId, username, getString(R.string.acceptStr))
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.voteDecline)) { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    netHandler.sendVoteAnswer(roomId, username, getString(R.string.declineStr))
                }
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}