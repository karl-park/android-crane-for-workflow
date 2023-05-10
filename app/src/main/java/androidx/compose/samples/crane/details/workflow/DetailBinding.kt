package androidx.compose.samples.crane.details.workflow

import android.util.Log
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.samples.crane.details.DetailsScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.compose.composeScreenViewFactory
import com.squareup.workflow1.ui.compose.tooling.Preview

@OptIn(WorkflowUiExperimentalApi::class)
val DetailBinding = composeScreenViewFactory<DetailRendering> { rendering, _ ->
    DetailsScreen(
        onErrorLoading = {

        },
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    )
}

@OptIn(WorkflowUiExperimentalApi::class)
@Preview(heightDp = 150, showBackground = true)
@Composable
fun DrawHelloRenderingPreview() {
    DetailBinding.Preview(DetailRendering("Singapore!", city = null, {}, {}))
}
