package androidx.compose.samples.crane.details.workflow

import androidx.compose.samples.crane.data.City
import androidx.compose.samples.crane.data.DestinationsRepository
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailProp
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailState
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailStatus.Fail
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailStatus.Initialise
import androidx.compose.samples.crane.details.workflow.DetailWorkflow.DetailStatus.Success
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.asWorker
import com.squareup.workflow1.runningWorker
import com.squareup.workflow1.ui.Screen
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailWorkflow @Inject constructor(
    private val destinationsRepository: DestinationsRepository,
) : StatefulWorkflow<DetailProp, DetailState, Unit, DetailRendering>() {

    override fun initialState(
        props: DetailProp,
        snapshot: Snapshot?
    ): DetailState {
        return DetailState(
            city = null,
            throwError = false,
            status = Initialise,
        )
    }

    override fun render(
        renderProps: DetailProp,
        renderState: DetailState,
        context: RenderContext
    ): DetailRendering {
        if (renderState.status == Initialise) {
            context.runningWorker(
                worker = flow {
                    emit(retrieveCity(renderProps.cityName))
                }.asWorker(),
            ) {
                changeTheStateWith(it)
            }
        }

        return renderState.toRendering(renderProps.cityName)
    }

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

    private fun retrieveCity(cityName: String): City? {
        return destinationsRepository.getDestination(cityName)
    }

    private fun changeTheStateWith(destination: City?) = action {
        state = DetailState(
            city = destination,
            status = if (destination != null) Success else Fail
        )
    }

    private fun DetailState.toRendering(
        cityName: String,
    ): DetailRendering {

        return DetailRendering(
            cityName = city?.name ?: cityName,
            city = city,
            isLoading = status !in listOf(Success, Fail),
            throwError = status == Fail,
            zoomIn = {

            },
            zoomOut = {

            },
        )
    }

}



@OptIn(WorkflowUiExperimentalApi::class)
data class DetailRendering(
    val cityName: String,

    val isLoading: Boolean = false,
    val throwError: Boolean = false,

    val city: City?,
    val zoomIn: () -> Unit,
    val zoomOut: () -> Unit,
) : Screen
