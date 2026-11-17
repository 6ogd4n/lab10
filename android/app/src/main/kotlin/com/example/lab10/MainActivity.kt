package com.example.lab10

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlin.math.roundToInt

private const val ROBOT_ICON_URL =
    "https://emojiisland.com/cdn/shop/products/Robot_Emoji_Icon_abe1111a-1293-4668-bdf9-9ceb05cff58e_large.png?v=1571606090"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation()
        }
      }
    }
  }
}

@Composable
fun AppNavigation() {
  val navController = rememberNavController()

  NavHost(
      navController = navController,
      startDestination = "input_screen"
  ) {
    // 1. Перший екран: введення тексту та вибір розміру
    composable("input_screen") { backStackEntry ->
      // Відстежуємо прапорець, чи був перехід на другий екран
      var navigatedToPreview by remember { mutableStateOf(false) }
      // Текст для діалогового вікна
      var dialogMessage by remember { mutableStateOf<String?>(null) }

      // Отримуємо результат з другого екрана через SavedStateHandle
      val resultState = backStackEntry.savedStateHandle
          .getStateFlow<String?>("preview_result", null)
          .collectAsState()

      LaunchedEffect(resultState.value, navigatedToPreview) {
        val result = resultState.value
        if (result != null) {
          // Користувач натиснув Ok або Cancel
          dialogMessage = result
          navigatedToPreview = false
          // Очищаємо стан, щоб діалог не з'являвся повторно при повороті екрана
          backStackEntry.savedStateHandle.remove<String>("preview_result")
        } else if (navigatedToPreview) {
          // Користувач повернувся без натискання кнопок (системна кнопка Back або стрілка)
          dialogMessage = "Don't know what to say"
          navigatedToPreview = false
        }
      }

      InputScreen(
          dialogMessage = dialogMessage,
          onDismissDialog = { dialogMessage = null },
          onNavigateToPreview = { text, fontSize ->
            navigatedToPreview = true
            // Кодуємо текст через Uri.encode на випадок пробілів чи спецсимволів
            val encodedText = Uri.encode(text.ifBlank { "Sample Text" })
            navController.navigate("preview_screen/$encodedText/$fontSize")
          }
      )
    }

    // 2. Другий екран: попередній перегляд тексту
    composable(
        route = "preview_screen/{text}/{fontSize}",
        arguments = listOf(
            navArgument("text") { type = NavType.StringType },
            navArgument("fontSize") { type = NavType.IntType }
        )
    ) { backStackEntry ->
      val rawText = backStackEntry.arguments?.getString("text") ?: ""
      val text = Uri.decode(rawText)
      val fontSize = backStackEntry.arguments?.getInt("fontSize") ?: 24

      PreviewScreen(
          text = text,
          fontSize = fontSize,
          onOkClick = {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("preview_result", "Cool!")
            navController.popBackStack()
          },
          onCancelClick = {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("preview_result", "Let’s try something else")
            navController.popBackStack()
          },
          onBackClick = {
            // Повернення без встановлення preview_result -> викличе "Don't know what to say"
            navController.popBackStack()
          }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    dialogMessage: String?,
    onDismissDialog: () -> Unit,
    onNavigateToPreview: (String, Int) -> Unit
) {
  var inputText by remember { mutableStateOf("") }
  var sliderValue by remember { mutableFloatStateOf(24f) }
  val fontSize = sliderValue.roundToInt()

  // Діалогове вікно після повернення з другого екрана
  if (dialogMessage != null) {
    AlertDialog(
        onDismissRequest = onDismissDialog,
        icon = {
          AsyncImage(
              model = ROBOT_ICON_URL,
              contentDescription = "Robot Icon",
              modifier = Modifier.size(56.dp)
          )
        },
        title = {
          Text(
              text = dialogMessage,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          TextButton(onClick = onDismissDialog) {
            Text("OK")
          }
        }
    )
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Text Editor") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
      }
  ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
      AsyncImage(
          model = ROBOT_ICON_URL,
          contentDescription = "Robot Icon",
          modifier = Modifier
              .size(80.dp)
              .padding(bottom = 16.dp)
      )

      OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          label = { Text("Enter your text") },
          placeholder = { Text("Type something...") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = false,
          maxLines = 4
      )

      Spacer(modifier = Modifier.height(24.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            text = "Text size:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "$fontSize sp",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
      }

      Slider(
          value = sliderValue,
          onValueChange = { sliderValue = it },
          valueRange = 12f..64f,
          modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(32.dp))

      Button(
          onClick = { onNavigateToPreview(inputText, fontSize) },
          modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
      ) {
        Text(
            text = "Preview",
            fontSize = 16.sp
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    text: String,
    fontSize: Int,
    onOkClick: () -> Unit,
    onCancelClick: () -> Unit,
    onBackClick: () -> Unit
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Preview") },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
      }
  ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Область попереднього перегляду тексту (з можливістю прокручування для великих розмірів)
      Box(
          modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
          contentAlignment = Alignment.Center
      ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.2).sp,
            textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Кнопки Ok та Cancel
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
        ) {
          Text("Cancel", fontSize = 16.sp)
        }

        Button(
            onClick = onOkClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
        ) {
          Text("Ok", fontSize = 16.sp)
        }
      }
    }
  }
}
