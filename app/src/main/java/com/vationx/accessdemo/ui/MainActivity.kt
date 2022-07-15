package com.vationx.accessdemo.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vationx.access.sdk.AccessSDK
import com.vationx.access.sdk.core.CommandResult
import com.vationx.access.sdk.model.presence.PresenceAccessStatus
import com.vationx.access.sdk.model.presence.ProvisionedState
import com.vationx.access.sdk.sdkinterfaces.SDKCallback
import com.vationx.access.sdk.sdkinterfaces.ServiceError
import com.vationx.access.sdk.sdkinterfaces.ServiceStatusEvent
import com.vationx.access.sdk.sdkinterfaces.UserCommand
import com.vationx.accessdemo.R
import com.vationx.accessdemo.data.DataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {
    private val readersList = mutableListOf<Reader>()
    lateinit var adapter: ReaderListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btSignOut = findViewById<AppCompatButton>(R.id.bt_signout)
        val btStart = findViewById<AppCompatButton>(R.id.bt_start)
        val btStop = findViewById<AppCompatButton>(R.id.bt_stop)
        val btReport = findViewById<AppCompatButton>(R.id.bt_report)
        val btRefresh = findViewById<AppCompatButton>(R.id.bt_refresh)
        val list = findViewById<RecyclerView>(R.id.list)
        adapter = ReaderListAdapter()
        list.adapter = adapter

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                GlobalScope.launch {
                    withContext(Main) {
                        //if we don't see a presence change for 15 sec, we treat is as lost or not nearby and remove from list
                        readersList.removeAll { (System.currentTimeMillis() - it.updatedAt) > 15000L }
                        adapter.submitList(readersList)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }, 10000L, 15000L)
        /******************************set on click listeners for buttons*****************************/
        btStart.setOnClickListener {
            //here since sdk is using bluetooth to scan nearby device, please make sure you have correct permission and bluetooth adapter is on
            //https://developer.android.com/guide/topics/connectivity/bluetooth/permissions
            AccessSDK.startService()
        }
        btStop.setOnClickListener {
            AccessSDK.stopService()
        }
        btSignOut.setOnClickListener {
            AccessSDK.userModule.revokeUser(object : SDKCallback<Unit> {
                override fun onFailure(code: Int, message: String) {
                    Toast.makeText(
                        this@MainActivity,
                        "sign out failed, error code:$code message: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@MainActivity, EntryActivity::class.java))
                    finish()
                }

                override fun onSuccess(result: Unit) {
                    startActivity(Intent(this@MainActivity, EntryActivity::class.java))
                    finish()
                }

            })
        }
        btRefresh.setOnClickListener {
            AccessSDK.userModule.refreshUserPermission(object : SDKCallback<Unit> {
                override fun onFailure(code: Int, message: String) {
                    Toast.makeText(
                        this@MainActivity,
                        "permission refresh failed, error code:$code message: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onSuccess(result: Unit) {
                    Toast.makeText(
                        this@MainActivity,
                        "permission refresh success",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
        btReport.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Report")
            val input = EditText(this)
            input.hint = "enter message"
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)
            builder.setPositiveButton("Report") { _, _ ->
                AccessSDK.settingModule.sendReport(input.text.toString(),
                    object : SDKCallback<Unit> {
                        override fun onFailure(code: Int, message: String) {
                            Toast.makeText(
                                this@MainActivity,
                                "report issue failed, error code:$code message: $message",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onSuccess(result: Unit) {
                            Toast.makeText(
                                this@MainActivity,
                                "report issue success!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    })
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
        }

        /*******************************receive sdk events and display*****************************/
        DataSource.commandResult.onEach {
            withContext(Main) {
                when (val result = it.consume()) {
                    is CommandResult.Success -> {
                        Toast.makeText(
                            this@MainActivity,
                            "command ${result.id} success",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is CommandResult.Failed -> {
                        Toast.makeText(
                            this@MainActivity,
                            "command result failed, ${result.reason}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> Unit
                }
            }
        }.launchIn(GlobalScope)


        DataSource.presenceFlow.sample(1200L).onEach { newPresence ->
            withContext(Main) {
                readersList.removeIf { it.presenceId == newPresence.presenceId }
                readersList.add(
                    Reader(
                        newPresence.presenceId,
                        newPresence.name,
                        newPresence.rssi,
                        newPresence.accessStatus,
                        newPresence.provisionedState,
                        true
                    )
                )
                readersList.sortBy { it.name }
                adapter.submitList(readersList)
                adapter.notifyDataSetChanged()
            }
        }.launchIn(GlobalScope)

        DataSource.unresolvedPresenceFlow.sample(1800L).onEach { newPresence ->
            withContext(Main) {
                readersList.removeIf { it.presenceId == newPresence.presenceId }
                readersList.add(
                    Reader(
                        newPresence.presenceId,
                        newPresence.name,
                        newPresence.rssi,
                        null,
                        null,
                        false
                    )
                )
                readersList.sortBy { it.name }
                adapter.submitList(readersList)
                adapter.notifyDataSetChanged()
            }
        }.launchIn(GlobalScope)

        DataSource.serviceStatusFlow.onEach {
            withContext(Main) {
                val event = it.consume()
                var message = ""
                when (event) {
                    is ServiceStatusEvent.ServiceStarted -> message = "ServiceStarted"
                    is ServiceStatusEvent.ServiceStopped -> message = "ServiceStarted"
                    is ServiceStatusEvent.Error -> {
                        when (event.error) {
                            is ServiceError.AdvertiserError -> message = "AdvertiserError"
                            is ServiceError.BlueToothAdapterNotSupported -> message =
                                "BlueToothAdapterNotSupported"
                            is ServiceError.PermissionNotSufficient -> message =
                                "PermissionNotSufficient"
                            is ServiceError.SdkNotInitialized -> message = "SdkNotInitialized"
                            is ServiceError.UserAuthenticatedTimedOut -> message =
                                "UserAuthenticatedTimedOut"
                            is ServiceError.UserNotAuthenticated -> message = "UserNotAuthenticated"
                        }
                    }
                    else -> message = "other"
                }
                Toast.makeText(
                    this@MainActivity,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.launchIn(GlobalScope)
    }

    class ReaderListAdapter : ListAdapter<Reader, ReaderListAdapter.ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_reader, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        @ExperimentalCoroutinesApi
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(item: Reader) {
                val image = itemView.findViewById<ImageView>(R.id.image)
                val unlock = itemView.findViewById<Button>(R.id.bt_unlock)
                val tvRssi = itemView.findViewById<TextView>(R.id.tv_rssi)
                val tvName = itemView.findViewById<TextView>(R.id.tv_name)

                if (item.resolved) {
                    if (item.provisionedState is ProvisionedState.Unprovisioned) {
                        image.setImageResource(R.drawable.ic_baseline_fiber_new_24)
                    } else {
                        image.setImageResource(R.drawable.ic_access_granted_rounded)
                    }
                    unlock.visibility = View.VISIBLE
                } else {
                    image.setImageResource(R.drawable.ic_access_denied_rounded)
                    unlock.visibility = View.INVISIBLE
                }
                tvName.text = item.name
                tvRssi.text = "Rssi: ${item.rssi}"
                unlock.setOnClickListener {
                    AccessSDK.enqueueCommand(item.presenceId, UserCommand.ManualUnlock)
                }
            }
        }

        @ExperimentalCoroutinesApi
        internal class DiffCallback : DiffUtil.ItemCallback<Reader>() {
            override fun areItemsTheSame(
                oldItem: Reader,
                newItem: Reader
            ): Boolean {
                return oldItem.presenceId == newItem.presenceId
            }

            override fun areContentsTheSame(
                oldItem: Reader,
                newItem: Reader
            ): Boolean {
                return false
            }
        }
    }

    data class Reader(
        val presenceId: String,
        val name: String,
        val rssi: Int,
        val accessState: PresenceAccessStatus?,
        val provisionedState: ProvisionedState?,
        val resolved: Boolean,
        val updatedAt: Long = System.currentTimeMillis()
    )
}