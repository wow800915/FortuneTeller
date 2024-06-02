package com.example.fortuneteller

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.fortuneteller.ui.theme.FortuneTellerTheme

class MainActivity : ComponentActivity() {
    private val selectedImage = mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FortuneTellerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HomeScreen(selectedImage)
                }
            }
        }
    }

    // Add this function to handle activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                selectedImage.value = BitmapFactory.decodeStream(inputStream)
            }
        }
    }
}

// Add this constant for image pick code
const val IMAGE_PICK_CODE = 1000
