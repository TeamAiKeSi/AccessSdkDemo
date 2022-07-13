package com.vationx.accessdemo.data

import com.vationx.access.sdk.core.CommandResult
import com.vationx.access.sdk.model.presence.ExternalPresence
import com.vationx.access.sdk.model.presence.UnResolvedPresence
import com.vationx.access.sdk.sdkinterfaces.ServiceStatusEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

object DataSource {
    private val _serviceStatusFlow: MutableStateFlow<Event<ServiceStatusEvent>?> by lazy {
        MutableStateFlow(
            null
        )
    }
    val serviceStatusFlow: Flow<Event<ServiceStatusEvent>> = _serviceStatusFlow.filterNotNull()

    private val _presenceFlow: MutableStateFlow<ExternalPresence?> by lazy {
        MutableStateFlow(
            null
        )
    }
    val presenceFlow: Flow<ExternalPresence> = _presenceFlow.filterNotNull()

    private val _unresolvedPresenceFlow: MutableStateFlow<UnResolvedPresence?> by lazy {
        MutableStateFlow(
            null
        )
    }
    val unresolvedPresenceFlow: Flow<UnResolvedPresence> = _unresolvedPresenceFlow.filterNotNull()

    private val _commandResult: MutableStateFlow<Event<CommandResult>?> by lazy {
        MutableStateFlow(
            null
        )
    }
    val commandResult: Flow<Event<CommandResult>> = _commandResult.filterNotNull()

    fun onStatus(serviceStatus: ServiceStatusEvent) {
        _serviceStatusFlow.value = Event(serviceStatus)
    }

    fun onPresence(presence: ExternalPresence) {
        _presenceFlow.value = presence
    }

    fun onUnresolvedPresence(presence: UnResolvedPresence) {
        _unresolvedPresenceFlow.value = presence
    }

    fun onCommandResult(result: CommandResult) {
        _commandResult.value = Event(result)
    }
}