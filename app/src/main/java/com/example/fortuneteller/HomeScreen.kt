package com.example.fortuneteller

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val placeholderResult = stringResource(R.string.results_placeholder)
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prompt = stringResource(R.string.prompt)

    val selectedImage = remember { mutableStateOf<Bitmap?>(null) }

    // Image picker launcher for selecting image from gallery
    val imagePickerlauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            selectedImage.value = BitmapFactory.decodeStream(inputStream)
        }
    }

    // Uri and File for capturing image with camera
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val photoFile = remember { File(context.cacheDir, "photo.jpg") }

    // Camera launcher for taking a picture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri.value?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                selectedImage.value = bitmap
            }
        }
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            photoUri.value = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoUri.value)
        } else {
            // Permission denied
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        if (selectedImage.value == null) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_photo),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Image(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = stringResource(R.string.home_title),
                    modifier = Modifier
                        .requiredSize(50.dp)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary))
                        .clickable {
                            imagePickerlauncher.launch("image/*")
                        }
                        .padding(16.dp)
                )
            }

            // Button to take a photo using the camera
            Button(
                onClick = {
                    // Check if camera permission is granted
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            // Permission is granted, launch camera
                            photoUri.value = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            cameraLauncher.launch(photoUri.value)
                        }

                        else -> {
                            // Request camera permission
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.add_photo))
            }
        }

        selectedImage.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.home_title),
                modifier = Modifier
                    .padding(16.dp)
                    .size(160.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary))
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        imagePickerlauncher.launch("image/*")
                    }
            )
        }

        Row(
            modifier = Modifier.padding(all = 16.dp)
        ) {
            Button(
                onClick = {
                    if (isNetworkAvailable(context)) {
                        homeViewModel.sendPrompt(selectedImage.value!!, prompt)
                    } else {
                        Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedImage.value != null,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(text = stringResource(R.string.action_go))
            }
        }

        // Display loading indicator or result based on UI state
        if (uiState is UiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            var textColor = MaterialTheme.colorScheme.onSurface
            if (uiState is UiState.Error) {
                textColor = MaterialTheme.colorScheme.error
                result = (uiState as UiState.Error).errorMessage
            } else if (uiState is UiState.Success) {
                textColor = MaterialTheme.colorScheme.onSurface
                result = (uiState as UiState.Success).outputText
            }
            val scrollState = rememberScrollState()
            Text(
                text = result,
                textAlign = TextAlign.Start,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }
    }
}