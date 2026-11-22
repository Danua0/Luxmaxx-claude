package com.luxmaxx.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.luxmaxx.app.AppViewModel
import com.luxmaxx.app.ui.theme.BorderGradient
import com.luxmaxx.app.ui.theme.Stroke
import com.luxmaxx.app.ui.theme.Surface1
import com.luxmaxx.app.ui.theme.TextHi
import java.io.File

/** Две кнопки: камера и галерея. Возвращает путь к сохранённому в приложении фото. */
@Composable
fun PhotoPickerRow(vm: AppViewModel, prefix: String, onPicked: (String) -> Unit, cameraLabel: String = "Камера") {
    val ctx = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pending
        if (ok && p != null) onPicked(p) else p?.let { File(it).delete() }
        pending = null
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.importFacePhoto(it, prefix)?.let { f -> onPicked(f.absolutePath) } }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GradientButton(cameraLabel, icon = Icons.Filled.CameraAlt, modifier = Modifier.weight(1f), onClick = {
            val f = vm.newFacePhotoFile(prefix)
            pending = f.absolutePath
            take.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f))
        })
        Row(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(Surface1)
                .border(1.dp, Stroke, RoundedCornerShape(16.dp))
                .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PhotoLibrary, null, tint = TextHi, modifier = Modifier.size(20.dp))
            Text("  Галерея", color = TextHi, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PhotoPreview(path: String?, modifier: Modifier = Modifier, ratio: Float = 1f) {
    val f = path?.let { File(it) }?.takeIf { it.exists() } ?: return
    AsyncImage(
        model = f, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderGradient, RoundedCornerShape(20.dp)),
    )
}
