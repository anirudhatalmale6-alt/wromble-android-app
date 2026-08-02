package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.*
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormScaffold(title: String, nav: NavController, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background).padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun Tf(value: String, onChange: (String) -> Unit, label: String,
              keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed)
    )
}

// Samler input-felterne i ét afrundet kort, så en formular med mange felter
// virker mere rolig og overskuelig. Rent visuelt – funktionen er uændret.
@Composable
private fun FieldCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) { Column(Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp), content = content) }
}

@Composable
private fun FormIntro(text: String) {
    Text(text, color = Color(0xFF8A8A90), fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
private fun SubmitButton(text: String, loading: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SuccessView(message: String, nav: NavController) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("Tak!", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(message, color = Color(0xFF6B6B72), modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))
        SubmitButton("Tilbage", false) { nav.popBackStack() }
    }
}

// ---- Edit profile ----
@Composable
fun EditProfileScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = Session.user
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var adress by remember { mutableStateOf("") }
    var zipcode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var loading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val id = user?.id ?: 0
        if (id > 0) try {
            val p = Api.service.userProfile(id).profile
            if (p != null) {
                firstname = p.firstname; lastname = p.lastname; adress = p.adress
                zipcode = p.zipcode; city = p.city; phone = p.phone
                if (p.email.isNotBlank()) email = p.email
            }
        } catch (_: Exception) {}
    }

    FormScaffold("Rediger profil", nav) {
        if (saved) { SuccessView("Din profil er opdateret", nav); return@FormScaffold }
        FormIntro("Dine oplysninger bruges til levering og kontakt.")
        FieldCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Tf(firstname, { firstname = it }, "Fornavn") }
                Box(Modifier.weight(1f)) { Tf(lastname, { lastname = it }, "Efternavn") }
            }
            Tf(adress, { adress = it }, "Adresse")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Tf(zipcode, { zipcode = it }, "Postnr.", KeyboardType.Number) }
                Box(Modifier.weight(2f)) { Tf(city, { city = it }, "By") }
            }
            Tf(phone, { phone = it }, "Telefon", KeyboardType.Phone)
            OutlinedTextField(email, {}, label = { Text("E-mail") }, enabled = false,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp))
        }
        if (msg.isNotBlank()) { Text(msg, color = WrombleRed); Spacer(Modifier.height(8.dp)) }
        SubmitButton("Gem", loading) {
            val id = user?.id ?: 0
            if (id == 0) { msg = "Log ind for at gemme"; return@SubmitButton }
            loading = true; msg = ""
            scope.launch {
                try {
                    val r = Api.service.updateUserProfile(mapOf(
                        "user_id" to id, "firstname" to firstname, "lastname" to lastname,
                        "adress" to adress, "zipcode" to zipcode, "city" to city, "phone" to phone))
                    loading = false
                    if (r.success) {
                        Session.user?.let { Session.save(ctx, it.copy(name = "$firstname $lastname".trim(), phone = phone)) }
                        saved = true
                    } else msg = r.error ?: "Kunne ikke gemme"
                } catch (_: Exception) { loading = false; msg = "Netværksfejl" }
            }
        }
    }
}

// ---- Contact ----
@Composable
fun ContactScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(Session.user?.name ?: "") }
    var email by remember { mutableStateOf(Session.user?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    FormScaffold("Kontakt os", nav) {
        if (done) { SuccessView("Vi vender tilbage hurtigst muligt", nav); return@FormScaffold }
        FormIntro("Skriv til os – vi vender tilbage hurtigst muligt.")
        FieldCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Tf(name, { name = it }, "Navn") }
                Box(Modifier.weight(1f)) { Tf(phone, { phone = it }, "Telefon", KeyboardType.Phone) }
            }
            Tf(email, { email = it }, "E-mail", KeyboardType.Email)
            Tf(subject, { subject = it }, "Emne")
            Tf(message, { message = it }, "Besked", lines = 4)
        }
        SubmitButton("Send", loading, enabled = name.isNotBlank() && message.isNotBlank()) {
            loading = true
            scope.launch {
                try {
                    Api.service.contact(mapOf("name" to name, "email" to email, "phone" to phone,
                        "subject" to subject, "message" to message))
                    loading = false; done = true
                } catch (_: Exception) { loading = false }
            }
        }
    }
}

// ---- Become partner ----
@Composable
fun PartnerScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var company by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    FormScaffold("Bliv partner", nav) {
        if (done) { SuccessView("Tak for din interesse – vi kontakter dig", nav); return@FormScaffold }
        FormIntro("Vil du sælge via Wromble? Udfyld, så kontakter vi dig.")
        FieldCard {
            Tf(company, { company = it }, "Virksomhed")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Tf(contact, { contact = it }, "Kontaktperson") }
                Box(Modifier.weight(1f)) { Tf(city, { city = it }, "By") }
            }
            Tf(email, { email = it }, "E-mail", KeyboardType.Email)
            Tf(phone, { phone = it }, "Telefon", KeyboardType.Phone)
            Tf(message, { message = it }, "Besked", lines = 3)
        }
        SubmitButton("Send", loading, enabled = company.isNotBlank() && email.isNotBlank()) {
            loading = true
            scope.launch {
                try {
                    Api.service.partner(mapOf("company" to company, "contact" to contact, "email" to email,
                        "phone" to phone, "city" to city, "message" to message))
                    loading = false; done = true
                } catch (_: Exception) { loading = false }
            }
        }
    }
}

// ---- Jobs ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(nav: NavController) {
    val jobs = remember { mutableStateListOf<JobPost>() }
    var loading by remember { mutableStateOf(true) }
    var applying by remember { mutableStateOf<JobPost?>(null) }

    LaunchedEffect(Unit) {
        try { jobs.addAll(Api.service.jobs().jobs) } catch (_: Exception) {}
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Job hos Wromble", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                jobs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Ingen ledige stillinger", color = Color(0xFF8A8A90)) }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(jobs) { j ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(j.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                (j.location ?: j.hours)?.let { Text(it, fontSize = 13.sp, color = Color(0xFF8A8A90)) }
                                (j.body ?: j.description)?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, fontSize = 14.sp, color = Color(0xFF6B6B72),
                                        modifier = Modifier.padding(top = 6.dp), maxLines = 3)
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { applying = j }, shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                                    Text("Søg nu", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    applying?.let { job -> JobApplyDialog(job, onDismiss = { applying = null }) }
}

@Composable
private fun JobApplyDialog(job: JobPost, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(Session.user?.name ?: "") }
    var email by remember { mutableStateOf(Session.user?.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (!done) TextButton(onClick = {
                loading = true
                scope.launch {
                    try {
                        Api.service.jobApply(mapOf("job_id" to job.id, "job_title" to job.title,
                            "name" to name, "email" to email, "phone" to phone, "message" to message))
                        loading = false; done = true
                    } catch (_: Exception) { loading = false }
                }
            }, enabled = name.isNotBlank() && email.isNotBlank() && !loading) { Text("Send", color = WrombleRed) }
            else TextButton(onClick = onDismiss) { Text("Luk", color = WrombleRed) }
        },
        dismissButton = { if (!done) TextButton(onClick = onDismiss) { Text("Annuller") } },
        title = { Text(if (done) "Ansøgning sendt" else "Søg: ${job.title}") },
        text = {
            if (done) Text("Tak for din ansøgning!")
            else Column {
                Tf(name, { name = it }, "Navn")
                Tf(email, { email = it }, "E-mail", KeyboardType.Email)
                Tf(phone, { phone = it }, "Telefon", KeyboardType.Phone)
                Tf(message, { message = it }, "Kort om dig", lines = 3)
                if (loading) CircularProgressIndicator(color = WrombleRed, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
    )
}

// ---- Delete account ----
@Composable
fun AccountDeletionScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = Session.user
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }

    FormScaffold("Slet konto", nav) {
        Text("Dette sletter din konto permanent. Handlingen kan ikke fortrydes.",
            color = Color(0xFF6B6B72), modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(password, { password = it },
            label = { Text("Bekræft med adgangskode") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed))
        if (msg.isNotBlank()) { Text(msg, color = WrombleRed); Spacer(Modifier.height(8.dp)) }
        Button(onClick = { confirm = true }, enabled = password.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
            if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Slet min konto", fontWeight = FontWeight.Bold)
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false; loading = true; msg = ""
                    scope.launch {
                        try {
                            val r = Api.service.deleteAccount(mapOf(
                                "user_id" to (user?.id ?: 0), "email" to (user?.email ?: ""), "password" to password))
                            loading = false
                            if (r.success) { Session.clear(ctx); nav.navigate("login") { popUpTo(0) } }
                            else msg = r.error ?: "Forkert adgangskode"
                        } catch (_: Exception) { loading = false; msg = "Netværksfejl" }
                    }
                }) { Text("Slet", color = WrombleRed) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuller") } },
            title = { Text("Er du sikker?") },
            text = { Text("Din konto og dine data slettes permanent.") }
        )
    }
}
