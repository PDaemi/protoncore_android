package me.proton.core.plan.data

import android.content.Context
import android.content.res.Resources
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import me.proton.core.featureflag.domain.ExperimentalProtonFeatureFlag
import me.proton.core.featureflag.domain.FeatureFlagManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalProtonFeatureFlag::class)
class IsDynamicPlanAdjustedPriceEnabledImplTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var resources: Resources

    @MockK
    private lateinit var featureFlagManager: FeatureFlagManager

    private lateinit var tested: IsDynamicPlanAdjustedPriceEnabledImpl

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.resources } returns resources
        tested = IsDynamicPlanAdjustedPriceEnabledImpl(context, featureFlagManager)
    }

    @Test
    fun localEnabledRemoteEnabledReturnEnabled() {
        every { resources.getBoolean(R.bool.core_feature_dynamic_plan_adjusted_prices_enabled) } returns true
        every { featureFlagManager.getValue(any(), IsDynamicPlanAdjustedPriceEnabledImpl.featureId) } returns true

        assertTrue(tested(userId = null))
    }

    @Test
    fun localEnabledRemoteDisabledReturnDisabled() {
        every { resources.getBoolean(R.bool.core_feature_dynamic_plan_adjusted_prices_enabled) } returns true
        every { featureFlagManager.getValue(any(), IsDynamicPlanAdjustedPriceEnabledImpl.featureId) } returns false

        assertFalse(tested(userId = null))
    }

    @Test
    fun localDisabledRemoteEnabledReturnDisabled() {
        every { resources.getBoolean(R.bool.core_feature_dynamic_plan_adjusted_prices_enabled) } returns false
        every { featureFlagManager.getValue(any(), IsDynamicPlanAdjustedPriceEnabledImpl.featureId) } returns true

        assertFalse(tested(userId = null))
    }

    @Test
    fun localDisabledRemoteDisabledReturnDisabled() {
        every { resources.getBoolean(R.bool.core_feature_dynamic_plan_adjusted_prices_enabled) } returns false
        every { featureFlagManager.getValue(any(), IsDynamicPlanAdjustedPriceEnabledImpl.featureId) } returns false

        assertFalse(tested(userId = null))
    }
}
