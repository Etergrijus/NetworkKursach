package com.example.clientkurswork

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.clientkurswork.databinding.FragmentStartWindowBinding


class StartWindowFragment : Fragment() {
    private lateinit var binding: FragmentStartWindowBinding

    private var signalman: FragmentActivitySignalman? = null

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
        binding = FragmentStartWindowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.onlineGameButton.setOnClickListener {
            if (binding.usernameEdit.text.isEmpty())
                binding.usernameEdit.setHint(R.string.emptyUsernameHint)
            else {
                signalman?.onDataPass(binding.usernameEdit.text.toString())
                signalman?.onShowNextFragment()
            }
        }

        binding.singleGameButton.setOnClickListener {
            if (binding.usernameEdit.text.isEmpty())
                binding.usernameEdit.setHint(R.string.emptyUsernameHint)
            else {
                val intent = Intent(binding.root.context, SingleGameActivity::class.java)
                intent.putExtra("Username", binding.usernameEdit.text.toString())
                binding.root.context.startActivity(intent)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        signalman = null
    }
}