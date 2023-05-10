package androidx.compose.samples.crane.details.workflow

import androidx.compose.samples.crane.data.City
import androidx.compose.samples.crane.data.DestinationsRepository
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailProp
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailState
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailStatus.Initialise
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailStatus.Success
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.runningWorker
import com.squareup.workflow1.ui.Screen
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi


class DetailWorkflow(private val destinationsRepository: DestinationsRepository) :
    StatefulWorkflow<DetailProp, DetailState, Unit, DetailRendering>() {


    override fun initialState(
        props: DetailProp,
        snapshot: Snapshot?
    ): DetailState = DetailState(
        city = null,
        throwError = false,
        status = Initialise,
    )

    override fun render(
        renderProps: DetailProp,
        renderState: DetailState,
        context: RenderContext
    ): DetailRendering {

        if (renderState.status == Initialise) {
            context.actionSink.send(actionRetrieveCity)
        }
        return renderState.toRendering(renderProps.cityName)
    }

    // todo restore zoom in and zoom out
    override fun snapshotState(state: DetailState): Snapshot? = null

    data class DetailProp(
        val cityName: String,
    )

    data class DetailState(
        val city: City?,
        val throwError: Boolean = false,
        val status: DetailStatus = Initialise,
    )

    enum class DetailStatus {
        Success, Fail, Initialise
    }

    private val actionRetrieveCity = action {
        val destination = destinationsRepository.getDestination(props.cityName)
        state = DetailState(
            city = destination,
            throwError = destination == null
        )
    }
}

private fun DetailState.toRendering(cityName: String): DetailRendering {

    return DetailRendering(
        cityName = cityName,
        city = city,
        zoomIn = {},
        zoomOut = {},
    )
}

@OptIn(WorkflowUiExperimentalApi::class)
data class DetailRendering(
    val cityName: String,
    val city: City?,
    val zoomIn: () -> Unit,
    val zoomOut: () -> Unit,
) : Screen
