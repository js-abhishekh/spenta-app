package com.abhishekhjs.spenta.nearby

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyManager(private val context: Context) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.abhishekhjs.spenta.BILL_SPLIT"

    private val _discoveredEndpoints = MutableStateFlow<List<Endpoint>>(emptyList())
    val discoveredEndpoints = _discoveredEndpoints.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<List<Endpoint>>(emptyList())
    val connectedEndpoints = _connectedEndpoints.asStateFlow()

    private val _incomingPayloads = MutableStateFlow<String?>(null)
    val incomingPayloads = _incomingPayloads.asStateFlow()

    data class Endpoint(val id: String, val name: String, val profileImage: String? = null)

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("NearbyManager", "Endpoint found: $endpointId (${info.endpointName})")
            val newList = _discoveredEndpoints.value.toMutableList()
            if (newList.none { it.id == endpointId }) {
                val parts = info.endpointName.split("|")
                val name = parts.getOrNull(0) ?: info.endpointName
                val profileImage = parts.getOrNull(1)
                newList.add(Endpoint(endpointId, name, profileImage))
                _discoveredEndpoints.value = newList
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("NearbyManager", "Endpoint lost: $endpointId")
            val newList = _discoveredEndpoints.value.toMutableList()
            newList.removeAll { it.id == endpointId }
            _discoveredEndpoints.value = newList
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("NearbyManager", "Connection initiated with $endpointId (${info.endpointName})")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d("NearbyManager", "Connection successful with $endpointId")
                val discovered = _discoveredEndpoints.value.find { it.id == endpointId }
                val endpointName = discovered?.name ?: "Unknown"
                val profileImage = discovered?.profileImage
                val newList = _connectedEndpoints.value.toMutableList()
                if (newList.none { it.id == endpointId }) {
                    newList.add(Endpoint(endpointId, endpointName, profileImage))
                    _connectedEndpoints.value = newList
                }
            } else {
                Log.e("NearbyManager", "Connection failed with $endpointId: ${result.status}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("NearbyManager", "Disconnected from $endpointId")
            val newList = _connectedEndpoints.value.toMutableList()
            newList.removeAll { it.id == endpointId }
            _connectedEndpoints.value = newList
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = String(payload.asBytes()!!)
                Log.d("NearbyManager", "Payload received from $endpointId: $data")
                _incomingPayloads.value = data
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    fun clearIncomingPayload() {
        _incomingPayloads.value = null
    }

    fun startAdvertising(userName: String) {
        if (!hasPermissions()) {
            Log.e("NearbyManager", "Cannot start advertising: Missing permissions")
            return
        }
        Log.d("NearbyManager", "Starting advertising as $userName")
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(userName, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener { Log.d("NearbyManager", "Advertising started successfully") }
            .addOnFailureListener { e -> Log.e("NearbyManager", "Advertising failed", e) }
    }

    fun startDiscovery() {
        if (!hasPermissions()) {
            Log.e("NearbyManager", "Cannot start discovery: Missing permissions")
            return
        }
        Log.d("NearbyManager", "Starting discovery")
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d("NearbyManager", "Discovery started successfully") }
            .addOnFailureListener { e -> Log.e("NearbyManager", "Discovery failed", e) }
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _discoveredEndpoints.value = emptyList()
        _connectedEndpoints.value = emptyList()
    }

    fun connectToEndpoint(endpointId: String, userName: String) {
        if (!hasPermissions()) {
            Log.e("NearbyManager", "Cannot connect: Missing permissions")
            return
        }
        connectionsClient.requestConnection(userName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e -> Log.e("NearbyManager", "Connection request failed", e) }
    }

    fun sendPayload(endpointId: String, data: String) {
        Log.d("NearbyManager", "Sending payload to $endpointId: $data")
        val payload = Payload.fromBytes(data.toByteArray())
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener { Log.d("NearbyManager", "Payload sent successfully to $endpointId") }
            .addOnFailureListener { e -> Log.e("NearbyManager", "Failed to send payload to $endpointId", e) }
    }

    private fun hasPermissions(): Boolean {
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, NEARBY_WIFI_DEVICES is the primary permission for Nearby
            val hasNearbyWifi = ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
            hasNearbyWifi || hasCoarse || hasFine
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On Android 12, Bluetooth and Location might both be needed
            val hasBtScan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            (hasBtScan && hasCoarse) || hasFine
        } else {
            // Below Android 12, Location is strictly required
            hasCoarse || hasFine
        }
    }
}
