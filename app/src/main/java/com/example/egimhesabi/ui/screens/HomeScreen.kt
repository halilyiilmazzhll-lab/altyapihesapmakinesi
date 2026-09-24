package com.example.egimhesabi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.theme.*

@Composable
fun HomeScreen(
    onNavigateStakeout: () -> Unit,
    onNavigateTracking: () -> Unit,
    onNavigateSlope: () -> Unit,
    onNavigateInterpolation: () -> Unit,
    onNavigateReverse: () -> Unit,
    onNavigateImpact: () -> Unit,
    onNavigateLeveling: () -> Unit,
    onNavigateElevationTransfer: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // iOS grouped background
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ana Menü",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(onClick = onNavigateSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayarlar",
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        com.example.egimhesabi.ui.components.ActiveStakeoutCard(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                HomeGridCard(
                    title = "Aplikasyon Tutanağı",
                    subtitle = "İlçe ve mahalleye göre baca kayıtları",
                    icon = Icons.Default.TableChart,
                    onClick = onNavigateStakeout
                )
            }
            item {
                HomeGridCard(
                    title = "İmalat Takibi",
                    subtitle = "Saha imalat durumunu takip et",
                    icon = Icons.Default.Map,
                    onClick = onNavigateTracking
                )
            }
            item {
                HomeGridCard(
                    title = "Eğim Hesabı",
                    subtitle = "İki nokta arasındaki eğimi bul",
                    icon = Icons.Default.TrendingDown,
                    onClick = onNavigateSlope
                )
            }
            item {
                HomeGridCard(
                    title = "Ara Kot",
                    subtitle = "Belirli mesafedeki ara kotu bulur",
                    icon = Icons.Default.LinearScale,
                    onClick = onNavigateInterpolation
                )
            }
            item {
                HomeGridCard(
                    title = "Tersine Hesap",
                    subtitle = "İstenen eğime göre kot bulur",
                    icon = Icons.Default.KeyboardReturn,
                    onClick = onNavigateReverse
                )
            }
            item {
                HomeGridCard(
                    title = "Etki Hesabı",
                    subtitle = "Baca kotu değişiminin hatlara etkisini incele",
                    icon = Icons.Default.Polyline,
                    onClick = onNavigateImpact
                )
            }
            item {
                HomeGridCard(
                    title = "Nivo & Boru",
                    subtitle = "RS ile kotlandır, boru mira kontrolü yap",
                    icon = Icons.Default.Straighten,
                    onClick = onNavigateLeveling
                )
            }
            item {
                HomeGridCard(
                    title = "Kot Taşıma",
                    subtitle = "RS'den yeni bir noktaya kot taşı",
                    icon = Icons.Default.SwapVert,
                    onClick = onNavigateElevationTransfer
                )
            }
        }
    }
}

@Composable
fun HomeGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Tam kare (karo) olması için
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}
