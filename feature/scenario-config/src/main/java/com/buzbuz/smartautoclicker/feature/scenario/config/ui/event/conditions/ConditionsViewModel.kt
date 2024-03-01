/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.conditions

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.trigger.TriggerConditionTypeChoice
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getImageConditionBitmap

import kotlinx.coroutines.*

import kotlinx.coroutines.flow.*

class ConditionsViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)
    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** Currently configured event. */
    val configuredEventConditions: Flow<List<Condition>> = editionRepository.editionState.editedEventConditionsState
        .mapNotNull { it.value }

    /** Tells if the limitation in conditions count have been reached. */
    val isConditionLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(configuredEventConditions) { isProModePurchased, conditions ->
            !isProModePurchased && (conditions.size  >= ProModeAdvantage.Limitation.CONDITION_COUNT_LIMIT.limit)
        }

    /** Tells if there is at least one condition to copy. */
    val canCopyCondition: Flow<Boolean> = editionRepository.editionState.canCopyConditions

    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    fun getEditedEvent(): Event? = editionRepository.editionState.getEditedEvent()

    /**
     * Get a new condition based on the provided one.
     * @param condition the condition to copy.
     */
    fun createNewImageConditionFromCopy(condition: ImageCondition): ImageCondition =
        editionRepository.editedItemsBuilder.createNewImageConditionFrom(condition)

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param type the type of condition to create.
     */
    fun createNewTriggerCondition(context: Context, type: TriggerConditionTypeChoice): TriggerCondition =
        when (type) {
            TriggerConditionTypeChoice.OnBroadcastReceived ->
                editionRepository.editedItemsBuilder.createNewOnBroadcastReceived(context)
            TriggerConditionTypeChoice.OnCounterReached ->
                editionRepository.editedItemsBuilder.createNewOnCounterReached(context)
            TriggerConditionTypeChoice.OnTimerReached ->
                editionRepository.editedItemsBuilder.createNewOnTimerReached(context)
        }

    /**
     * Get a new condition based on the provided one.
     * @param condition the condition to copy.
     */
    fun createNewTriggerConditionFromCopy(condition: TriggerCondition): TriggerCondition =
        editionRepository.editedItemsBuilder.createNewTriggerConditionFrom(condition)

    fun startConditionEdition(condition: Condition) = editionRepository.startConditionEdition(condition)

    /** Insert/update a new condition to the event. */
    fun upsertEditedCondition() =
        editionRepository.upsertEditedCondition()

    /** Remove a condition from the event. */
    fun removeEditedCondition() =
        editionRepository.deleteEditedCondition()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedCondition() = editionRepository.stopConditionEdition()

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(repository, condition, onBitmapLoaded)

    fun onConditionCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Limitation.CONDITION_COUNT_LIMIT)
    }

    fun monitorCreateConditionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_CONDITION, view)
    }

    fun monitorFirstConditionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION, view)
    }

    fun stopFirstConditionViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION)

    }

    fun stopAllViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_CONDITION)
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION)
    }
}
