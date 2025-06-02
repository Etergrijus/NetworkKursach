package com.example.clientkurswork

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.clientkurswork.databinding.ActivityMainBinding
import com.example.clientkurswork.ui.theme.ClientKursWorkTheme

class MainActivity : AppCompatActivity(), FragmentActivitySignalman {
    private lateinit var binding: ActivityMainBinding

    private lateinit var username : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragmentStartWindow = StartWindowFragment()
        supportFragmentManager.beginTransaction()
            .add(binding.main.id, fragmentStartWindow)
            .commit()

    }

    override fun onDataPass(data: String) {
        username = data
        Log.d("Data Pass", "The activity got a username $username")
    }

    override fun onShowNextFragment() {
        val fragmentConnectAwaiting = ConnectAwaitingFragment(username)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(binding.main.id, fragmentConnectAwaiting)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClientKursWorkTheme {
        Greeting("Android")
    }
}