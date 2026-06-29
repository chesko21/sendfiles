package com.chesko.sendfiles.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.chesko.sendfiles.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NsdHelper(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = Constants.NSD_SERVICE_TYPE

    private val uniqueId = (1000..9999).random().toString()
    private val baseServiceName = "SendFiles-${android.os.Build.MODEL}"
    private val _serviceName = MutableStateFlow("$baseServiceName-$uniqueId")
    val serviceName: StateFlow<String> = _serviceName.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<Set<Device>>(emptySet())
    val discoveredDevices: StateFlow<Set<Device>> = _discoveredDevices.asStateFlow()

    enum class DiscoveryState { IDLE, SEARCHING, FINISHED, ERROR }
    private val _discoveryState = MutableStateFlow(DiscoveryState.IDLE)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _isServiceRegistered = MutableStateFlow(false)
    val isServiceRegistered: StateFlow<Boolean> = _isServiceRegistered.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timeoutJob: Job? = null

    fun registerService(port: Int) {
        if (_isServiceRegistered.value || registrationListener != null) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = _serviceName.value
            serviceType = this@NsdHelper.serviceType
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) {

                _serviceName.value = registeredServiceInfo.serviceName
                _isServiceRegistered.value = true
                Log.d("NsdHelper", "Service registered: ${_serviceName.value}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isServiceRegistered.value = false
                registrationListener = null
                Log.e("NsdHelper", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                _isServiceRegistered.value = false
                registrationListener = null
                Log.d("NsdHelper", "Service unregistered")
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isServiceRegistered.value = false
                registrationListener = null
                Log.e("NsdHelper", "Unregistration failed: $errorCode")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        if (_discoveryState.value == DiscoveryState.SEARCHING) {
            stopDiscovery()
        }
        
        _discoveryState.value = DiscoveryState.SEARCHING
        _discoveredDevices.value = emptySet()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdHelper", "Discovery failed: $errorCode")
                _discoveryState.value = DiscoveryState.ERROR
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdHelper", "Stop discovery failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NsdHelper", "Discovery started")
                startTimeout()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NsdHelper", "Discovery stopped")
                if (_discoveryState.value == DiscoveryState.SEARCHING) {
                    _discoveryState.value = DiscoveryState.FINISHED
                }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Service found: ${serviceInfo.serviceName} type: ${serviceInfo.serviceType}")

                val isOwnService = serviceInfo.serviceName == _serviceName.value
                
                if (serviceInfo.serviceType.contains(serviceType) && !isOwnService) {
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Service lost: ${serviceInfo.serviceName}")
                _discoveredDevices.update { devices ->
                    devices.filterNot { it.name == serviceInfo.serviceName }.toSet()
                }
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            val callback = object : NsdManager.ServiceInfoCallback {
                override fun onServiceUpdated(resolvedServiceInfo: NsdServiceInfo) {
                    nsdManager.unregisterServiceInfoCallback(this)
                    val host = resolvedServiceInfo.hostAddresses.firstOrNull() ?: return
                    
                    Log.d("NsdHelper", "Service resolved (API 34+): $host:${resolvedServiceInfo.port}")
                    updateDiscoveredDevices(resolvedServiceInfo, host)
                }

                override fun onServiceLost() {}
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                    Log.e("NsdHelper", "Callback registration failed: $errorCode")
                }
                override fun onServiceInfoCallbackUnregistered() {}
            }
            nsdManager.registerServiceInfoCallback(serviceInfo, { it.run() }, callback)
        } else {
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e("NsdHelper", "Resolve failed: $errorCode")
                }

                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                    timeoutJob?.cancel()
                    @Suppress("DEPRECATION")
                    val host = resolvedServiceInfo.host
                    if (host == null) {
                        Log.e("NsdHelper", "Resolved service host is null")
                        return
                    }

                    Log.d("NsdHelper", "Service resolved: $host:${resolvedServiceInfo.port}")
                    updateDiscoveredDevices(resolvedServiceInfo, host)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= 33) {
                @Suppress("DEPRECATION")
                nsdManager.resolveService(serviceInfo, { it.run() }, resolveListener)
            } else {
                @Suppress("DEPRECATION")
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
        }
    }

    private fun updateDiscoveredDevices(serviceInfo: NsdServiceInfo, host: java.net.InetAddress) {
        val device = Device(
            name = serviceInfo.serviceName,
            host = host,
            port = serviceInfo.port
        )
        _discoveredDevices.update { it + device }
    }

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(15000)
            if (_discoveredDevices.value.isEmpty()) {
                Log.d("NsdHelper", "Discovery timeout - no devices found")
                stopDiscovery()
                _discoveryState.value = DiscoveryState.FINISHED
            }
        }
    }

    fun restartDiscovery() {
        stopDiscovery()
        discoverServices()
    }

    fun stopDiscovery() {
        timeoutJob?.cancel()
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e("NsdHelper", "Error stopping discovery", e)
            }
        }
        discoveryListener = null
        if (_discoveryState.value == DiscoveryState.SEARCHING) {
            _discoveryState.value = DiscoveryState.IDLE
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e("NsdHelper", "Error unregistering service", e)
            }
        }
        registrationListener = null
        _isServiceRegistered.value = false
    }
}
