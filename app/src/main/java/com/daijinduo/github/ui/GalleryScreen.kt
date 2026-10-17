package com.daijinduo.github.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.daijinduo.github.R
import com.daijinduo.github.data.GalleryLoader
import com.daijinduo.github.data.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun readPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

/**
 * 相册页：申请权限 → 网格展示最近图片 → 点击进入全屏效果。
 * 顶部提供"系统相册"（Photo Picker，无需权限）作为兜底入口。
 */
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onPick: (Uri) -> Unit,
    onSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val permission = readPermission()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(permission)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackGlyph(onClick = onBack)
                Spacer(Modifier.size(14.dp))
                Text(
                    text = stringResource(R.string.gallery_title),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.action_system_picker),
                    color = AccentB,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onSystemPicker)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            if (!granted) {
                PermissionPrompt(
                    onGrant = { permissionLauncher.launch(permission) },
                    onSystemPicker = onSystemPicker
                )
            } else {
                ImageGrid(onPick = onPick)
            }
        }
    }
}

@Composable
private fun BackGlyph(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "‹", color = TextPrimary, fontSize = 26.sp)
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit, onSystemPicker: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "▦", fontSize = 32.sp, color = TextPrimary)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.gallery_permission_denied),
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onGrant,
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = AccentA,
                contentColor = Color.White
            )
        ) {
            Text(stringResource(R.string.action_grant))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.action_system_picker),
            color = AccentB,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSystemPicker)
                .padding(10.dp)
        )
    }
}

@Composable
private fun ImageGrid(onPick: (Uri) -> Unit) {
    val context = LocalContext.current
    var images by remember { mutableStateOf<List<ImageItem>?>(null) }

    LaunchedEffect(Unit) {
        images = try {
            GalleryLoader.loadRecent(context)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val list = images
    when {
        list == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = AccentB,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        list.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.gallery_empty),
                    color = TextSecondary
                )
            }
        }

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    ThumbnailCell(item = item, onClick = { onPick(item.uri) })
                }
            }
        }
    }
}

@Composable
private fun ThumbnailCell(item: ImageItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            GalleryLoader.loadThumbnail(context.contentResolver, item.uri, 480)
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentB.copy(alpha = 0.6f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
