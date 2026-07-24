package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.brandGradient
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

private data class OnbPage(val icon: ImageVector, val title: String, val body: String)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = listOf(
        OnbPage(Icons.Filled.RestaurantMenu, "Bestil mad", "Find dine favoritter og bestil med få tryk – nemt og hurtigt."),
        OnbPage(Icons.Filled.Storefront, "Shop lokalt", "Støt butikker og spisesteder i dit lokalområde."),
        OnbPage(Icons.Filled.Notifications, "Følg din ordre", "Få besked med det samme, når din ordre er på vej."),
        OnbPage(Icons.Filled.Place, "Find i nærheden", "Se hvad der er åbent lige nu tæt på dig.")
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun finish() {
        Settings.setOnboardingDone(ctx, true)
        val dest = if (Session.user == null) "login" else "main"
        nav.navigate(dest) { popUpTo("onboarding") { inclusive = true } }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { finish() }, modifier = Modifier.padding(top = 40.dp, end = 8.dp)) {
                Text("Spring over", color = Color(0xFF8A8A90))
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { i ->
            val p = pages[i]
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(140.dp).clip(CircleShape).background(brandGradient),
                    contentAlignment = Alignment.Center
                ) { Icon(p.icon, null, tint = Color.White, modifier = Modifier.size(70.dp)) }
                Spacer(Modifier.height(36.dp))
                Text(p.title, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.body, fontSize = 16.sp, color = Color(0xFF6B6B72),
                    textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    Modifier.padding(4.dp).size(if (selected) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (selected) WrombleRed else Color(0xFFD5D5DB))
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                else finish()
            },
            modifier = Modifier.fillMaxWidth().padding(24.dp).height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
        ) {
            Text(
                if (pagerState.currentPage < pages.lastIndex) "Næste" else "Kom i gang",
                fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}
