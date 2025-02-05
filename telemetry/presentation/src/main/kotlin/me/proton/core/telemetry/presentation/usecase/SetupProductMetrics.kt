/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.telemetry.presentation.usecase

import android.app.Application
import android.view.View
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import me.proton.core.presentation.ui.view.AdditionalOnClickListener
import me.proton.core.presentation.ui.view.AdditionalOnFocusChangeListener
import me.proton.core.presentation.ui.view.ProtonMaterialToolbar
import me.proton.core.presentation.utils.UiComponent
import me.proton.core.presentation.utils.launchOnUiComponentCreated
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.presentation.ProductMetricsDelegate
import me.proton.core.telemetry.presentation.ProductMetricsDelegate.Companion.KEY_ITEM
import me.proton.core.telemetry.presentation.ProductMetricsDelegateOwner
import me.proton.core.telemetry.presentation.annotation.MenuItemClicked
import me.proton.core.telemetry.presentation.annotation.ProductMetrics
import me.proton.core.telemetry.presentation.annotation.ScreenClosed
import me.proton.core.telemetry.presentation.annotation.ScreenDisplayed
import me.proton.core.telemetry.presentation.annotation.ViewClicked
import me.proton.core.telemetry.presentation.annotation.ViewFocused
import me.proton.core.telemetry.presentation.measureOnScreenClosed
import me.proton.core.telemetry.presentation.measureOnScreenDisplayed
import me.proton.core.telemetry.presentation.measureOnViewClicked
import me.proton.core.telemetry.presentation.measureOnViewFocused
import me.proton.core.telemetry.presentation.setupViewMetrics
import javax.inject.Inject

internal class SetupProductMetrics @Inject constructor(
    private val application: Application,
    private val telemetryManager: TelemetryManager
) {
    operator fun invoke() {
        application.launchOnUiComponentCreated(this::onUiComponentCreated)
    }

    private fun onUiComponentCreated(
        lifecycleOwner: LifecycleOwner,
        onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        component: UiComponent
    ) {
        val delegateOwner = component.value as? ProductMetricsDelegateOwner
        val productMetrics = component.value.findAnnotation<ProductMetrics>()
        val hasProductMetricsAnnotation = component.value.hasProductMetricsAnnotation()

        when {
            delegateOwner != null && productMetrics != null -> error(
                "Cannot use both the ${ProductMetricsDelegateOwner::class.simpleName} and " +
                        "${ProductMetrics::class.simpleName} annotation in ${component.value::class.qualifiedName}."
            )

            delegateOwner == null && productMetrics == null && hasProductMetricsAnnotation -> error(
                "${component.value::class.qualifiedName} must implement either " +
                        "${ProductMetricsDelegateOwner::class.simpleName} or " +
                        "annotate ${ProductMetrics::class.simpleName} annotation."
            )

            delegateOwner == null && productMetrics == null -> return
        }

        val resolvedDelegateOwner = when {
            delegateOwner != null -> delegateOwner
            productMetrics != null -> ProductMetricsDelegateOwner(
                AnnotationProductMetricsDelegate(
                    productMetrics,
                    telemetryManager
                )
            )

            else -> error("Fatal error: both delegateOwner and screenMetrics and null.")
        }

        component.value.findAnnotation<ScreenDisplayed>()?.let { screenDisplayed ->
            measureOnScreenDisplayed(
                productEvent = screenDisplayed.event,
                productDimensions = screenDisplayed.dimensions.toMap(),
                delegateOwner = resolvedDelegateOwner,
                lifecycleOwner = lifecycleOwner,
                savedStateRegistryOwner = savedStateRegistryOwner,
                priority = screenDisplayed.priority
            )
        }

        component.value.findAnnotation<ScreenClosed>()?.let { screenClosed ->
            measureOnScreenClosed(
                productEvent = screenClosed.event,
                productDimensions = screenClosed.dimensions.toMap(),
                delegateOwner = resolvedDelegateOwner,
                lifecycleOwner = lifecycleOwner,
                onBackPressedDispatcherOwner = onBackPressedDispatcherOwner,
                priority = screenClosed.priority
            )
        }

        component.value.findAnnotation<ViewClicked>()?.let { viewClicked ->
            setupViewClicked(component, lifecycleOwner, resolvedDelegateOwner, viewClicked)
        }

        component.value.findAnnotation<ViewFocused>()?.let { viewFocused ->
            setupViewFocused(component, lifecycleOwner, resolvedDelegateOwner, viewFocused)
        }

        component.value.findAnnotation<MenuItemClicked>()?.let { menuItemClicked ->
            setupMenuItemClicked(component, lifecycleOwner, menuItemClicked, resolvedDelegateOwner)
        }
    }

    private fun setupViewClicked(
        component: UiComponent,
        lifecycleOwner: LifecycleOwner,
        resolvedDelegateOwner: ProductMetricsDelegateOwner,
        viewClicked: ViewClicked
    ) = lifecycleOwner.setupViewMetrics {
        for (viewId in viewClicked.viewIds) {
            val id = component.getIdentifier(viewId)
            val view = component.findViewById<View>(id)

            view?.setOnClickListener(object : AdditionalOnClickListener {
                override fun onClick(p0: View?) {
                    measureOnViewClicked(
                        event = viewClicked.event,
                        delegateOwner = resolvedDelegateOwner,
                        productDimensions = mapOf("item" to viewId),
                        priority = viewClicked.priority
                    )
                }
            })
        }
    }

    private fun setupViewFocused(
        component: UiComponent,
        lifecycleOwner: LifecycleOwner,
        resolvedDelegateOwner: ProductMetricsDelegateOwner,
        viewFocused: ViewFocused
    ) = lifecycleOwner.setupViewMetrics {
        for (viewId in viewFocused.viewIds) {
            val id = component.getIdentifier(viewId)
            val view = component.findViewById<View>(id)

            view?.onFocusChangeListener = object : AdditionalOnFocusChangeListener {
                override fun onFocusChange(view: View?, hasFocus: Boolean) {
                    if (hasFocus) {
                        measureOnViewFocused(
                            event = viewFocused.event,
                            delegateOwner = resolvedDelegateOwner,
                            productDimensions = mapOf(KEY_ITEM to viewId),
                            priority = viewFocused.priority
                        )
                    }
                }
            }
        }
    }

    private fun setupMenuItemClicked(
        component: UiComponent,
        lifecycleOwner: LifecycleOwner,
        menuItemClicked: MenuItemClicked,
        resolvedDelegateOwner: ProductMetricsDelegateOwner
    ) = lifecycleOwner.setupViewMetrics {
        val toolbarId = component.getIdentifier(menuItemClicked.toolbarId)
        val toolbar = component.findViewById<View>(toolbarId) as? ProtonMaterialToolbar
        val itemIds = menuItemClicked.itemIds.associateBy { component.getIdentifier(it) }
        toolbar?.setAdditionalOnMenuItemClickListener {
            val itemIdName = itemIds[it.itemId] ?: return@setAdditionalOnMenuItemClickListener false
            measureOnViewClicked(
                event = menuItemClicked.event,
                delegateOwner = resolvedDelegateOwner,
                productDimensions = mapOf("item" to itemIdName),
                priority = menuItemClicked.priority
            )
            true
        }
    }
}

private class AnnotationProductMetricsDelegate(
    productMetrics: ProductMetrics,
    override val telemetryManager: TelemetryManager
) : ProductMetricsDelegate {
    override val productGroup: String = productMetrics.group
    override val productFlow: String = productMetrics.flow
    override val productDimensions: Map<String, String> = productMetrics.dimensions.toMap()
}

private inline fun <reified T : Annotation> Any.findAnnotation(): T? =
    javaClass.annotations.filterIsInstance<T>().firstOrNull()

private inline fun Any.hasProductMetricsAnnotation(): Boolean =
    javaClass.annotations.filterIsInstance<ScreenDisplayed>().firstOrNull() != null ||
            javaClass.annotations.filterIsInstance<ScreenClosed>().firstOrNull() != null ||
            javaClass.annotations.filterIsInstance<ViewClicked>().firstOrNull() != null ||
            javaClass.annotations.filterIsInstance<ViewFocused>().firstOrNull() != null ||
            javaClass.annotations.filterIsInstance<MenuItemClicked>().firstOrNull() != null

@VisibleForTesting
internal fun <T> Array<T>.toMap(): Map<T, T> {
    require(size % 2 == 0) { "The array must have an even number of elements." }
    return toList().chunked(2).associate { it[0] to it[1] }
}
