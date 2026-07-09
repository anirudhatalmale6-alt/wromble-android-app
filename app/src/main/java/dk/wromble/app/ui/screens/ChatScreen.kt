package dk.wromble.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.wromble.app.data.BASE_URL
import dk.wromble.app.data.ChatMessage
import dk.wromble.app.data.Session
import dk.wromble.app.ui.ChatViewModel
import dk.wromble.app.ui.NetworkImage
import dk.wromble.app.ui.theme.WrombleRed
import java.io.File
import java.net.URLEncoder

private fun chatFileUrl(path: String?): String? =
    if (path.isNullOrBlank()) null else if (path.startsWith("http")) path else "$BASE_URL$path"

private fun copyUriToCache(ctx: Context, uri: Uri): File? = try {
    val input = ctx.contentResolver.openInputStream(uri)
    if (input == null) null else {
        val f = File(ctx.cacheDir, "chat_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { out -> input.use { it.copyTo(out) } }
        f
    }
} catch (_: Exception) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val ctx = LocalContext.current
    val vm: ChatViewModel = viewModel()
    var showCall by remember { mutableStateOf(false) }

    // Genoptag/stop polling naar man skifter fane
    DisposableEffect(Unit) {
        vm.resume()
        onDispose { vm.stop() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kundeservice", fontWeight = FontWeight.Bold) },
                actions = {
                    if (vm.isStarted && vm.status == "open") {
                        IconButton(onClick = { showCall = true }) {
                            Icon(Icons.Filled.Videocam, "Videoopkald", tint = WrombleRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!vm.isStarted) {
                ChatStartForm(vm)
            } else {
                ChatMessages(vm, Modifier.weight(1f))
                if (vm.status == "open") ChatInputBar(vm)
                else ClosedBanner()
            }
        }
    }

    if (showCall) {
        val name = URLEncoder.encode(
            vm.senderName.ifBlank { Session.user?.name ?: "Kunde" }, "UTF-8"
        )
        CallDialog(
            url = "$BASE_URL/app-call.php?cid=${vm.conversationId}&name=$name&video=1",
            onClose = { showCall = false }
        )
    }
}

@Composable
private fun ChatStartForm(vm: ChatViewModel) {
    var name by remember { mutableStateOf(Session.user?.name ?: "") }
    var email by remember { mutableStateOf(Session.user?.email ?: "") }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(42.dp)).background(WrombleRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.ChatBubble, null, tint = WrombleRed, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(16.dp))
        Text("Kontakt Kundeservice", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("Vi svarer hurtigst muligt", fontSize = 14.sp, color = Color(0xFF8A8A90))
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Dit navn") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("E-mail (valgfrit)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.start(name, email) },
            enabled = name.isNotBlank() && !vm.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
        ) {
            if (vm.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Start chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChatMessages(vm: ChatViewModel, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }
    LazyColumn(
        modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(vm.messages) { msg -> ChatBubble(msg) }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isCustomer = msg.senderType == "customer"
    val fileUrl = chatFileUrl(msg.fileUrl)
    val isImage = msg.fileType == "image" && fileUrl != null
    val isPlaceholder = msg.message == "[Billede]" || msg.message.startsWith("[Fil:")
    val showText = msg.message.isNotBlank() && !(fileUrl != null && isPlaceholder)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isCustomer) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isCustomer && msg.senderName.isNotBlank()) {
                Text(msg.senderName, fontSize = 11.sp, color = Color(0xFF8A8A90),
                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp))
            }
            val bg = if (isCustomer) WrombleRed else Color(0xFFEDEDF0)
            val fg = if (isCustomer) Color.White else Color(0xFF1A1A1E)
            if (showText) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(bg)
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(msg.message, color = fg, fontSize = 15.sp)
                }
            }
            if (isImage) {
                Spacer(Modifier.height(if (showText) 4.dp else 0.dp))
                NetworkImage(
                    fileUrl,
                    Modifier.width(200.dp).heightIn(max = 220.dp).clip(RoundedCornerShape(14.dp))
                )
            } else if (fileUrl != null) {
                Spacer(Modifier.height(if (showText) 4.dp else 0.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(bg)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.InsertDriveFile, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(msg.fileName ?: "Fil", color = fg, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(vm: ChatViewModel) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var showAttach by remember { mutableStateOf(false) }
    val camFile = remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) camFile.value?.let { vm.upload(it, "image/jpeg") }
    }
    fun launchCamera() {
        val f = File(ctx.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
        camFile.value = f
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        cameraLauncher.launch(uri)
    }
    val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { copyUriToCache(ctx, it)?.let { f -> vm.upload(f, "image/jpeg") } }
    }

    fun onCamera() {
        showAttach = false
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            launchCamera()
        else camPermLauncher.launch(Manifest.permission.CAMERA)
    }

    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { showAttach = true }, enabled = !vm.isUploading) {
                    if (vm.isUploading)
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = WrombleRed)
                    else
                        Icon(Icons.Filled.AddCircle, "Vedhaeft", tint = WrombleRed)
                }
                DropdownMenu(expanded = showAttach, onDismissRequest = { showAttach = false }) {
                    DropdownMenuItem(
                        text = { Text("Tag billede") },
                        leadingIcon = { Icon(Icons.Filled.PhotoCamera, null) },
                        onClick = { onCamera() }
                    )
                    DropdownMenuItem(
                        text = { Text("Vaelg fra galleri") },
                        leadingIcon = { Icon(Icons.Filled.Image, null) },
                        onClick = { showAttach = false; galleryLauncher.launch("image/*") }
                    )
                }
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Skriv en besked...") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(22.dp)
            )
            Spacer(Modifier.width(4.dp))
            FilledIconButton(
                onClick = { if (text.isNotBlank()) { vm.send(text.trim()); text = "" } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = WrombleRed)
            ) { Icon(Icons.Filled.Send, "Send", tint = Color.White) }
        }
    }
}

@Composable
private fun ClosedBanner() {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF0F0F3)).padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("Denne samtale er afsluttet", color = Color(0xFF8A8A90), fontSize = 14.sp)
    }
}

@Composable
private fun CallDialog(url: String, onClose: () -> Unit) {
    val webRef = remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) { onDispose { webRef.value?.destroy() } }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                request.grant(request.resources)
                            }
                        }
                        loadUrl(url)
                        webRef.value = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .size(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.5f))
            ) { Icon(Icons.Filled.Close, "Afslut opkald", tint = Color.White) }
        }
    }
}
