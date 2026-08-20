package com.example.skbt_up_gibdd_eyewitness.feature.chat

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.skbt_up_gibdd_eyewitness.R
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.MediaAttachment
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.MediaKind
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.displaySize
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.createCaptureUri
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.galleryPermissions
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.hasGalleryPermission
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.loadRecentPhotoUris
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.resolveMediaSelection
import com.example.skbt_up_gibdd_eyewitness.ui.components.AppTopBar
import com.example.skbt_up_gibdd_eyewitness.ui.theme.IncomingBubble
import com.example.skbt_up_gibdd_eyewitness.ui.theme.OutgoingBubble
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PreviewMessage(val text: String, val time: String, val outgoing: Boolean)
private enum class AttachmentTab { GALLERY, LOCATION }

private val previewMessages = listOf(
    PreviewMessage("Вижу автомобиль, водитель ведёт себя подозрительно", "14:22", true),
    PreviewMessage("Уточните государственный номер.", "14:23", false),
    PreviewMessage("А123БВ44", "14:24", true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingSelection by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var recentPhotoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var galleryAccessGranted by remember { mutableStateOf(hasGalleryPermission(context)) }
    val localMediaMessages = remember { mutableStateListOf<MediaAttachment>() }
    val acceptMedia: (List<android.net.Uri>) -> Unit = { uris ->
        val result = resolveMediaSelection(context, uris)
        pendingSelection = (pendingSelection + result.accepted).distinctBy { it.uri }
        if (result.rejectionMessages.isNotEmpty()) {
            Toast.makeText(context, result.rejectionMessages.joinToString("\n"), Toast.LENGTH_LONG).show()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10), acceptMedia)
    val refreshRecentPhotos: () -> Unit = {
        scope.launch {
            recentPhotoUris = withContext(Dispatchers.IO) { loadRecentPhotoUris(context, limit = 5) }
        }
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        galleryAccessGranted = hasGalleryPermission(context)
        if (galleryAccessGranted) refreshRecentPhotos()
    }
    val photoCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingCaptureUri?.let { uri -> if (success) acceptMedia(listOf(uri)) }
        pendingCaptureUri = null
    }
    val videoCapture = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        pendingCaptureUri?.let { uri -> if (success) acceptMedia(listOf(uri)) }
        pendingCaptureUri = null
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(onBackClick)
        Text(
            "Сегодня, 14 августа",
            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(previewMessages) { MessageBubble(it) }
            items(localMediaMessages, key = { it.id }) { MediaBubble(it) }
        }
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    showAttachmentSheet = true
                    galleryAccessGranted = hasGalleryPermission(context)
                    if (galleryAccessGranted) {
                        refreshRecentPhotos()
                    } else {
                        galleryPermissionLauncher.launch(galleryPermissions())
                    }
                },
                modifier = Modifier.size(44.dp),
            ) { Icon(Icons.Rounded.AttachFile, "Прикрепить фото, видео или GIF", tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(6.dp))
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("Сообщение") },
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = {}, modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Отправить", tint = Color.White)
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAttachmentSheet = false
                pendingSelection = emptyList()
            },
            sheetState = attachmentSheetState,
            containerColor = Color.White,
        ) {
            AttachmentSheet(
                media = pendingSelection,
                recentPhotoUris = recentPhotoUris,
                galleryAccessGranted = galleryAccessGranted,
                onClose = {
                    showAttachmentSheet = false
                    pendingSelection = emptyList()
                },
                onOpenPicker = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onRequestGalleryAccess = { galleryPermissionLauncher.launch(galleryPermissions()) },
                onRecentPhoto = { uri -> acceptMedia(listOf(uri)) },
                onTakePhoto = {
                    runCatching {
                        createCaptureUri(context, MediaKind.PHOTO).also { uri ->
                            pendingCaptureUri = uri
                            photoCapture.launch(uri)
                        }
                    }.onFailure { Toast.makeText(context, "Камера для фото недоступна", Toast.LENGTH_SHORT).show() }
                },
                onTakeVideo = {
                    runCatching {
                        createCaptureUri(context, MediaKind.VIDEO).also { uri ->
                            pendingCaptureUri = uri
                            videoCapture.launch(uri)
                        }
                    }.onFailure { Toast.makeText(context, "Камера для видео недоступна", Toast.LENGTH_SHORT).show() }
                },
                onRemove = { removed -> pendingSelection = pendingSelection.filterNot { it.id == removed.id } },
                onAdd = {
                    localMediaMessages += pendingSelection
                    pendingSelection = emptyList()
                    showAttachmentSheet = false
                },
                onLocationSelected = { label ->
                    Toast.makeText(context, "$label: подключим на этапе геолокации", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

@Composable
private fun AttachmentSheet(
    media: List<MediaAttachment>,
    recentPhotoUris: List<android.net.Uri>,
    galleryAccessGranted: Boolean,
    onClose: () -> Unit,
    onOpenPicker: () -> Unit,
    onRequestGalleryAccess: () -> Unit,
    onRecentPhoto: (android.net.Uri) -> Unit,
    onTakePhoto: () -> Unit,
    onTakeVideo: () -> Unit,
    onRemove: (MediaAttachment) -> Unit,
    onAdd: () -> Unit,
    onLocationSelected: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AttachmentTab.GALLERY) }
    Column(Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 620.dp)) {
        Box(Modifier.fillMaxWidth().height(54.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(Icons.Rounded.Close, "Закрыть", tint = Color(0xFF455A7A))
            }
            Text(
                if (selectedTab == AttachmentTab.GALLERY) "Фото и видео" else "Геолокация",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTab == AttachmentTab.GALLERY) {
                GalleryAttachmentContent(
                    media = media,
                    recentPhotoUris = recentPhotoUris,
                    galleryAccessGranted = galleryAccessGranted,
                    onOpenPicker = onOpenPicker,
                    onRequestGalleryAccess = onRequestGalleryAccess,
                    onRecentPhoto = onRecentPhoto,
                    onTakePhoto = onTakePhoto,
                    onTakeVideo = onTakeVideo,
                    onRemove = onRemove,
                    onAdd = onAdd,
                )
            } else {
                LocationAttachmentContent(onLocationSelected)
            }
        }

        Row(Modifier.fillMaxWidth().height(82.dp).background(Color.White)) {
            AttachmentTabButton(
                selected = selectedTab == AttachmentTab.GALLERY,
                icon = { tint -> Icon(Icons.Rounded.PhotoLibrary, null, tint = tint) },
                label = "Галерея",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = AttachmentTab.GALLERY },
            )
            AttachmentTabButton(
                selected = selectedTab == AttachmentTab.LOCATION,
                icon = { tint -> Icon(Icons.Rounded.LocationOn, null, tint = tint) },
                label = "Геолокация",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = AttachmentTab.LOCATION },
            )
        }
    }
}

@Composable
private fun GalleryAttachmentContent(
    media: List<MediaAttachment>,
    recentPhotoUris: List<android.net.Uri>,
    galleryAccessGranted: Boolean,
    onOpenPicker: () -> Unit,
    onRequestGalleryAccess: () -> Unit,
    onRecentPhoto: (android.net.Uri) -> Unit,
    onTakePhoto: () -> Unit,
    onTakeVideo: () -> Unit,
    onRemove: (MediaAttachment) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (media.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CaptureAction(Icons.Rounded.PhotoCamera, "Снять фото", Modifier.weight(1f), onTakePhoto)
                CaptureAction(Icons.Rounded.Videocam, "Снять видео", Modifier.weight(1f), onTakeVideo)
            }
            Text(
                "Последние фото",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                !galleryAccessGranted -> {
                    OutlinedButton(onClick = onRequestGalleryAccess, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Разрешить доступ к фото")
                    }
                }
                recentPhotoUris.isEmpty() -> {
                    Text(
                        "Недавние фотографии не найдены",
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(recentPhotoUris, key = { it.toString() }) { uri ->
                            Surface(onClick = { onRecentPhoto(uri) }, modifier = Modifier.aspectRatio(1f), color = Color(0xFFE2E8F0)) {
                                AsyncImage(model = uri, contentDescription = "Недавнее фото", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onOpenPicker,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2840)),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text("Выбрать из галереи", fontWeight = FontWeight.Bold)
            }
            Text("Фото до 10 МБ · Видео и GIF до 100 МБ", modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
        } else {
            Spacer(Modifier.weight(1f))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(media, key = { it.id }) { attachment -> MediaPreviewTile(attachment, onRemove) }
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Добавить (${media.size})", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenPicker, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Выбрать другие")
            }
        }
    }
}

@Composable
private fun CaptureAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier.height(128.dp), color = Color(0xFF1A2840), shape = RoundedCornerShape(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(38.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LocationAttachmentContent(onSelected: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Как поделиться местоположением?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        LocationOption(Icons.Rounded.MyLocation, "Текущее местоположение", "Отправить координаты устройства") { onSelected("Текущее местоположение") }
        LocationOption(Icons.Rounded.Map, "Выбрать точку на карте", "Указать место вручную") { onSelected("Выбор точки на карте") }
        LocationOption(Icons.Rounded.ShareLocation, "Live-геолокация", "Передавать координаты 15 минут") { onSelected("Live-геолокация") }
    }
}

@Composable
private fun LocationOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFD9E4F5), modifier = Modifier.size(44.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AttachmentTabButton(
    selected: Boolean,
    icon: @Composable (Color) -> Unit,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF90A4AE)
    Column(
        modifier.background(if (selected) Color(0xFFF4F5F7) else Color.White).clickable(onClick = onClick).padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent))
        Spacer(Modifier.height(8.dp))
        icon(color)
        Text(label, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun MediaPreviewTile(attachment: MediaAttachment, onRemove: (MediaAttachment) -> Unit) {
    Box(Modifier.width(118.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2840))) {
        MediaContent(attachment, Modifier.fillMaxSize())
        IconButton(
            onClick = { onRemove(attachment) },
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).background(Color(0x99000000), CircleShape),
        ) { Icon(Icons.Rounded.Close, "Удалить", tint = Color.White, modifier = Modifier.size(17.dp)) }
        Text(
            attachment.displaySize(),
            modifier = Modifier.align(Alignment.BottomStart).background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun MediaBubble(attachment: MediaAttachment) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(Modifier.width(200.dp).height(140.dp).clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)).background(Color(0xFF1A2840))) {
            MediaContent(attachment, Modifier.fillMaxSize())
            Text(
                when (attachment.kind) { MediaKind.PHOTO -> "Фото"; MediaKind.VIDEO -> "Видео"; MediaKind.GIF -> "GIF" },
                modifier = Modifier.align(Alignment.BottomStart).background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp, end = 2.dp)) {
            Icon(Icons.Rounded.HourglassTop, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("Ожидает подключения", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun MediaContent(attachment: MediaAttachment, modifier: Modifier) {
    if (attachment.kind == MediaKind.VIDEO) {
        Box(modifier) { Icon(Icons.Rounded.PlayArrow, "Видео", tint = Color.White, modifier = Modifier.size(48.dp).align(Alignment.Center)) }
    } else {
        AsyncImage(
            model = attachment.uri,
            contentDescription = if (attachment.kind == MediaKind.GIF) "GIF" else "Фото",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MessageBubble(message: PreviewMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!message.outgoing) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(31.dp)) {
                Image(painterResource(R.drawable.ic_shield), null, Modifier.padding(7.dp))
            }
            Spacer(Modifier.width(9.dp))
        }
        Surface(
            color = if (message.outgoing) OutgoingBubble else IncomingBubble,
            shape = RoundedCornerShape(15.dp),
            shadowElevation = if (message.outgoing) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 264.dp),
        ) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(Modifier.align(if (message.outgoing) Alignment.End else Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
                    Text(message.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    if (message.outgoing) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.DoneAll, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 403, heightDp = 874)
@Composable
private fun ChatPreview() = SKBTUPGIBDDEYEWITNESSTheme { ChatScreen({}) }
