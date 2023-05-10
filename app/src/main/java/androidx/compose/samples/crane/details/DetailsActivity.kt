/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.samples.crane.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.samples.crane.data.City
import androidx.compose.samples.crane.data.ExploreModel
import androidx.compose.samples.crane.details.workflow.DetailBinding
import androidx.compose.samples.crane.details.workflow.DetailRendering
import androidx.compose.samples.crane.details.workflow.DetailWorkflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewRegistry
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.compose.WorkflowRendering
import com.squareup.workflow1.ui.compose.renderAsState
import com.squareup.workflow1.ui.plus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val KEY_ARG_DETAILS_CITY_NAME = "KEY_ARG_DETAILS_CITY_NAME"

fun launchDetailsActivity(context: Context, item: ExploreModel) {
    context.startActivity(createDetailsActivityIntent(context, item))
}

@VisibleForTesting
fun createDetailsActivityIntent(context: Context, item: ExploreModel): Intent {
    val intent = Intent(context, DetailsActivity::class.java)
    intent.putExtra(KEY_ARG_DETAILS_CITY_NAME, item.city.name)
    return intent
}

@AndroidEntryPoint
class DetailsActivity : ComponentActivity() {

    @Inject
    lateinit var detailWorkflow: DetailWorkflow

    @OptIn(WorkflowUiExperimentalApi::class)
    private val viewEnvironment = ViewEnvironment.EMPTY + ViewRegistry(DetailBinding)

    @OptIn(WorkflowUiExperimentalApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val cityName = intent.getStringExtra(KEY_ARG_DETAILS_CITY_NAME) ?: ""

        setContent {
            val rendering by detailWorkflow.renderAsState(
                props = DetailWorkflow.DetailProp(
                    cityName = cityName
                ),
                onOutput = {}
            )

            WorkflowRendering(
                rendering = rendering,
                viewEnvironment = viewEnvironment
            )
        }
    }
}

@Composable
fun DetailsScreen(
    onErrorLoading: () -> Unit,
    modifier: Modifier = Modifier,
    detailRendering: DetailRendering,
) {
    Crossfade(targetState = detailRendering, modifier) { rendering ->
        when {
            rendering.city != null -> {
                DetailsContent(rendering.city, Modifier.fillMaxSize())
            }
            rendering.isLoading -> {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            else -> {
                onErrorLoading()
            }
        }
    }
}

@Composable
fun DetailsContent(
    city: City,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(32.dp))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = city.nameToDisplay,
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        CityMapView(city.latitude, city.longitude)
    }
}

/**
 * CityMapView
 * A composable that shows a map centered on a location with a marker.
 */
@Composable
fun CityMapView(
    latitude: String,
    longitude: String,
    onMapLoadedWithCameraState: ((CameraPositionState) -> Unit)? = null, // Exposed for use in tests
    onZoomChanged: (() -> Unit)? = null
) {
    val cityLocation = remember(latitude, longitude) {
        LatLng(latitude.toDouble(), longitude.toDouble())
    }

    val cameraPositionState = rememberCameraPositionState(cityLocation.toString()) {
        position = CameraPosition.fromLatLngZoom(
            cityLocation,
            InitialZoom
        )
    }

    MapViewContainer(
        cameraPositionState = cameraPositionState,
        onMapLoaded = {
            onMapLoadedWithCameraState?.invoke(cameraPositionState)
        },
        onZoomChanged = onZoomChanged
    ) {
        Marker(state = MarkerState(position = cityLocation))
    }
}

/**
 * MapViewContainer
 * A MapView styled with custom zoom controls.
 */
@Composable
fun MapViewContainer(
    cameraPositionState: CameraPositionState,
    onMapLoaded: () -> Unit = {},
    onZoomChanged: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    val mapProperties = remember {
        MapProperties(
            maxZoomPreference = MaxZoom,
            minZoomPreference = MinZoom,
        )
    }

    val mapUiSettings = remember {
        // We are providing our own zoom controls so disable the built-in ones.
        MapUiSettings(zoomControlsEnabled = false)
    }

    val animationScope = rememberCoroutineScope()
    Column {
        ZoomControls(
            onZoomIn = {
                animationScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    onZoomChanged?.invoke()
                }
            },
            onZoomOut = {
                animationScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    onZoomChanged?.invoke()
                }
            }
        )

        GoogleMap(
            properties = mapProperties,
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            onMapLoaded = onMapLoaded,
            content = content
        )
    }
}

@Composable
private fun ZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        ZoomButton("-", onClick = onZoomOut)
        ZoomButton("+", onClick = onZoomIn)
    }
}

@Composable
private fun ZoomButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.onPrimary,
            contentColor = MaterialTheme.colors.primary
        ),
        onClick = onClick
    ) {
        Text(text = text, style = MaterialTheme.typography.h5)
    }
}

private const val InitialZoom = 5f
const val MinZoom = 2f
const val MaxZoom = 20f
