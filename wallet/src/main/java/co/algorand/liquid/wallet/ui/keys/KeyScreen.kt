package co.algorand.liquid.wallet.ui.keys

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import co.algorand.liquid.wallet.Dimensions
import co.algorand.liquid.wallet.R
import co.algorand.liquid.wallet.ui.icons.Delete
import co.algorand.liquid.wallet.ui.icons.Medical
import co.algorand.liquid.wallet.ui.icons.Save
import co.algorand.liquid.wallet.ui.icons.Undo

@Composable
fun KeyScreen(
    navController: NavController,
    keyViewModel: KeyViewModel,
    modifier: Modifier = Modifier,
){
    val uiState by keyViewModel.uiState.collectAsStateWithLifecycle()

    KeyScreen(
        isError = uiState.error !== null,
        uiState = uiState,
        onNavigate = { navController.navigate(it) },
        onUndo = keyViewModel::onUndo,
        onChange = keyViewModel::onChange,
        onDelete = keyViewModel::onDelete,
        onCancelDelete = keyViewModel::onCancelDelete,
        onConfirmDelete = keyViewModel::onConfirmDelete,
        onGenerate = keyViewModel::onGenerate,
        onRecoverPasskey = keyViewModel::onRecoverPasskey,
        onCancelRecovery = keyViewModel::onCancelRecovery,
        onSave = {
            keyViewModel.onSave()
            navController.navigate("credentials")
        },
        modifier = modifier,
    )
}

@Composable
fun KeyScreen(
    uiState: KeyUiState,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onRecoverPasskey: (origin: String, userHandle: String) -> Unit,
    onUndo: ()-> Unit = {},
    onChange: (String)-> Unit = {},
    onDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelRecovery: () -> Unit = {},
    onSave: (mnemonic: String) -> Unit = {},
    onGenerate: () -> Unit = {},
    onNavigate: (path: String) -> Unit = {},
) {
    if(uiState.showConfirm){
        ConfirmDeleteDialog(
            onConfirmDelete = onConfirmDelete,
            onCancelDelete = onCancelDelete,
        )
    }
    if(uiState.showRecovery){
        RecoverPasskeyDialog(
            onCancelRecovery =onCancelRecovery,
            onRecoverPasskey = onRecoverPasskey,
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                isSaved = uiState.isSaved,
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomAppBar(
                uiState=uiState,
                isError=isError,
                onGenerate = onGenerate,
                onSave = onSave,
                onUndo = onUndo,
                onDelete = onDelete,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        KeyEditor(
            uiState=uiState,
            isError=isError,
            onChange = onChange,
            modifier = Modifier.padding(innerPadding)
        )

    }
}

@Composable
fun RecoverPasskeyDialog(
    onRecoverPasskey: (origin: String, userHandle: String) -> Unit,
    onCancelRecovery: ()-> Unit = {}
){
    var origin by remember { mutableStateOf("") }
    var userHandle by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onCancelRecovery
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "This is a dialog with buttons and an image.",
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = onCancelRecovery,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = {
                            onRecoverPasskey(origin, userHandle)
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportPasskey(
    onConfirmDelete: ()-> Unit = {},
    onCancelDelete: ()-> Unit = {},
){
    AlertDialog(
        icon = {
            Icon(Icons.Filled.Warning, contentDescription = "Warning Icon")
        },
        title = {
            Text(text = "Dangerously Delete")
        },
        text = {
            Text(text = "This will destroy all keys on this device. Make sure you have backed up the phrase in a safe place")
        },
        onDismissRequest = onCancelDelete,
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelDelete
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    onConfirmDelete: ()-> Unit = {},
    onCancelDelete: ()-> Unit = {},
){
    AlertDialog(
        icon = {
            Icon(Icons.Filled.Warning, contentDescription = "Warning Icon")
        },
        title = {
            Text(text = "Dangerously Delete")
        },
        text = {
            Text(text = "This will destroy all keys on this device. Make sure you have backed up the phrase in a safe place")
        },
        onDismissRequest = onCancelDelete,
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelDelete
            ) {
                Text("Dismiss")
            }
        }
    )
}



data class Instruction(val text: String, val icon: ImageVector)


@Composable
fun KeyEditor(
    uiState: KeyUiState,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onChange: (String) -> Unit = {},
){
    var instructions = if(!uiState.isSaved) mapOf(
        Pair("generate", Instruction("Generate a mnemonic", Icons.Filled.Refresh)),
        Pair("save", Instruction("Save record", Save)),
    ) else mapOf(
        Pair("generate", Instruction("Generate a mnemonic", Icons.Filled.Refresh)),
        Pair("save", Instruction("Save record", Save)),
        Pair("undo", Instruction("Revert unsaved changes", Undo)),
        Pair("recover", Instruction("Recover deleted Passkeys", Medical)),
        Pair("delete", Instruction("Delete all records", Delete))
    )

    val text = buildAnnotatedString {
        instructions.forEach {
            append("\u2022 ")
            appendInlineContent(it.component1(), "[${it.component1()}]")
            append(it.component2().text + "\n")
        }
    }

    val inlineContent = instructions.mapValues { entry ->
        InlineTextContent(
            // Placeholder tells text layout the expected size and vertical alignment of
            // children composable.
            Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            // This Icon will fill maximum size, which is specified by the [Placeholder]
            // above. Notice the width and height in [Placeholder] are specified in TextUnit,
            // and are converted into pixel by text layout.

            Icon(entry.component2().icon,"",tint = Color.Green,  modifier = Modifier.fillMaxSize())
        }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.instructions),
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = text,
            inlineContent = inlineContent,
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
        TextField(
            isError=isError,
            label = { Text("24 Word Mnemonic") },
            value= uiState.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.padding_medium),
            onValueChange = onChange
        )
        if(isError){
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.padding_medium),
                color = MaterialTheme.colorScheme.error,
                text = uiState.error?.message ?: "Something went wrong"
            )
        }
        if(uiState.isSaved && !uiState.isDirty) {
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
            KeysList(
                modifier = Modifier,
                keys = uiState.secretList,
                onKeySelected = {}
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBar(
    isSaved: Boolean,
    onNavigate: (path: String)-> Unit,
){
    CenterAlignedTopAppBar(
        modifier = Modifier,
        title = {
            Text(
                text = "Seed Phrase",
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            if(isSaved){
                IconButton(onClick = {onNavigate("credentials")}) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
                }
            }
        }
    )
}

@Composable
fun BottomAppBar(
    uiState: KeyUiState,
    isError: Boolean = false,
    onGenerate: ()-> Unit = {},
    onDelete: () -> Unit = {},
    onSave: (String) -> Unit = {},
    onUndo: ()-> Unit = {},
    onRecoverPasskey: () -> Unit = {},
){
    BottomAppBar(
        floatingActionButton = {
            if(!isError && (uiState.isDirty || !uiState.isSaved)) {
                FloatingActionButton(
                    onClick = { onSave(uiState.text) },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                ) {
                    Icon(
                        imageVector = Save,
                        contentDescription = "Save changes",
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onGenerate) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Generate"
                )
            }
            if(uiState.isSaved){
                IconButton(
                    enabled = uiState.isDirty,
                    onClick = onUndo,
                ) {
                    Icon(
                        imageVector = Undo,
                        contentDescription = "Undo changes",
                    )
                }
                if(!uiState.isDirty){
                    IconButton(
                        onClick = onRecoverPasskey
                    ) {
                        Icon(
                            imageVector = Medical,
                            contentDescription = "Recover passkey",
                        )
                    }
                    IconButton(
                        enabled = uiState.isSaved,
                        onClick = onDelete,
                    ) {
                        Icon(
                            imageVector = Delete,
                            contentDescription = "Delete everything",
                        )
                    }
                }
            }
        },
    )
}