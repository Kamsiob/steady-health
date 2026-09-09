package com.kamsiob.steadyhealth.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kamsiob.steadyhealth.ui.SettingsViewModel

/**
 * The data screen, and the one place this app opens a file it did not write.
 *
 * The file comes from the system picker, which is the only way this app should
 * ever read one: the person chooses it, the app is handed that one file and
 * nothing else, and there is no storage permission anywhere in this because none
 * is needed for it. Reading a folder would need one, and this never reads a
 * folder.
 *
 * It is a screen of its own rather than more of the navigation graph because the
 * picker has to be remembered by a composable, and the graph is shared.
 */
@Composable
fun DataRoute(
    viewModel: SettingsViewModel,
    onSummary: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val confirming by viewModel.confirmingDelete.collectAsStateWithLifecycle()
    val restore by viewModel.restore.collectAsStateWithLifecycle()
    // Every type, because a zip arrives named half a dozen different ways
    // depending on what wrote it, and a file somebody can see but cannot pick is
    // worse than a list they have to read. What is in it is checked afterwards.
    val pick = rememberLauncherForActivityResult(OpenDocument()) { chosen ->
        if (chosen != null) viewModel.readBackup(chosen)
    }
    DataScreen(
        onExport = viewModel::exportEverything,
        onSummary = onSummary,
        onRestore = { pick.launch(arrayOf("*/*")) },
        onDelete = viewModel::askToDelete,
        onBack = {
            viewModel.keepEverything()
            viewModel.keepWhatIsHere()
            onBack()
        },
        confirming = confirming,
        onConfirm = { viewModel.deleteEverything(onDeleted) },
        onCancel = viewModel::keepEverything,
        restore = restore,
        onRestoreConfirm = viewModel::restoreEverything,
        onRestoreCancel = viewModel::keepWhatIsHere,
    )
}
