package com.example.clientkurswork

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.clientkurswork.databinding.FragmentAwaitingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectAwaitingFragment(private val username: String) : Fragment() {
    private lateinit var binding: FragmentAwaitingBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAwaitingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.text.setText(R.string.awaitingNetwork)

        val netHandler = NetworkHandler(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            val success = netHandler.connectToServer(username)

            withContext(Dispatchers.Main) {
                Log.d("Network", "Responce is $success")
                if (success) {
                    binding.text.setText(R.string.awaitingPlayers)
                } else {
                    throw Exception("Error of work with server, stopped")
                }
            }
        }
    }
}