package com.vationx.accessdemo

import android.app.Application
import com.vationx.access.sdk.AccessSDK
import com.vationx.access.sdk.core.CommandResult
import com.vationx.access.sdk.model.config.Config
import com.vationx.access.sdk.model.config.SdkEnv
import com.vationx.access.sdk.model.config.ServiceNotificationInfo
import com.vationx.access.sdk.model.presence.ExternalPresence
import com.vationx.access.sdk.model.presence.UnResolvedPresence
import com.vationx.access.sdk.sdkinterfaces.ServiceListener
import com.vationx.access.sdk.sdkinterfaces.ServiceStatusEvent
import com.vationx.accessdemo.data.DataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSdk()
    }

    private fun initSdk() {
        // vationx sdk is running a foreground service to scan nearby devices, and it needs
        // a notification, here you can specify notification detail for display
        val notificationInfo = ServiceNotificationInfo.Builder()
            .setIcon(R.mipmap.ic_launcher)
            .setTitle("VationX service is runinng")
            .setMessage("Service is running to scan nearby devices")
            .setForegroundServiceNotificationId(9996)
            .setNotificationChannelName("signal channel")
            .build()
        //set sdk configuration
        val config = Config.Builder()
            .setAppKey("YOUR APP KEY") //PUT YOUR APP KEY HERE!!
            .setAppId("com.vationx.accessdemo") // here put your app package name
            .setServiceNotificationInfo(notificationInfo)
            .build()

        AccessSDK.init(this, config)
        AccessSDK.addServiceListener(object : ServiceListener {
            override fun onStatus(status: ServiceStatusEvent) {
                //here sdk is giving event when sdk status changes or notified, please check our document for different status types
                //ServiceStatusEvent is a sealed class with different status child classes
                DataSource.onStatus(status)
            }

            override fun onUnResolvedPresence(unResolvedPresence: UnResolvedPresence) {
                //nearby devices with no permission
                DataSource.onUnresolvedPresence(unResolvedPresence)
            }

            override fun onPresence(presence: ExternalPresence) {
                //nearby devices that user have access
                DataSource.onPresence(presence)
            }

            override fun onCommandResult(result: CommandResult) {
                //when there is an action taken, for example, manual unlock the reader. the result would be notified here
                DataSource.onCommandResult(result)
            }
        })
    }
}