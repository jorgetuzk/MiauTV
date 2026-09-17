package com.telegramtv.ui.mobile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telegramtv.data.model.FileItem
import com.telegramtv.data.model.Folder

@Composable
fun FileOptionsButton(
    file: FileItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDownload: () -> Unit,
    onExternalPlayer: () -> Unit,
    onCopyPublic: () -> Unit,
    onRevokePublic: () -> Unit,
    onCopyDownload: () -> Unit,
    onGoToFolder: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, "Opções", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Abrir no Player Externo") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                onClick = { onExternalPlayer(); showMenu = false }
            )
            onGoToFolder?.let {
                DropdownMenuItem(
                    text = { Text("Ir para a Pasta") },
                    leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                    onClick = { it(); showMenu = false }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Renomear") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { onRename(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Mover") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                onClick = { onMove(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Baixar") },
                leadingIcon = { Icon(Icons.Default.Download, null) },
                onClick = { onDownload(); showMenu = false }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (file.publicHash != null) "Copiar Link Público" else "Gerar Link Público") },
                leadingIcon = { Icon(Icons.Default.Share, null) },
                onClick = { onCopyPublic(); showMenu = false }
            )
            if (file.publicHash != null) {
                DropdownMenuItem(
                    text = { Text("Revogar Link Público") },
                    leadingIcon = { Icon(Icons.Default.LinkOff, null) },
                    onClick = { onRevokePublic(); showMenu = false }
                )
            }
            DropdownMenuItem(
                text = { Text("Copiar Link de Download") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = { onCopyDownload(); showMenu = false }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDelete(); showMenu = false }
            )
        }
    }
}

@Composable
fun FolderOptionsButton(onDelete: () -> Unit, onMove: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, "Opções", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Mover") }, onClick = { onMove(); showMenu = false })
            DropdownMenuItem(text = { Text("Excluir") }, onClick = { onDelete(); showMenu = false })
        }
    }
}

@Composable
fun InputDialog(title: String, initialValue: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun MovePickerDialog(
    title: String,
    currentFolderId: Int?,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Selecione a pasta de destino:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))

                // Option for root
                if (currentFolderId != null) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Home, null, modifier = Modifier.padding(end = 8.dp))
                        Text("Diretório Raiz")
                    }
                }

                folders.forEach { folder ->
                    if (folder.id != currentFolderId) {
                        MoveDestinationItem(
                            icon = Icons.Default.Folder,
                            name = folder.name,
                            onClick = { onConfirm(folder.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun MoveDestinationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
