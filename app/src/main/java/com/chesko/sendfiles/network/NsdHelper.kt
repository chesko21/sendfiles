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
    private val _serviceName = MutableStateFlow("SendFiles-${android.os.Build.MODEL}")
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
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = _serviceName.value
            serviceType = this@NsdHelper.serviceType
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                _serviceName.value = NsdServiceInfo.serviceName
                _isServiceRegistered.value = true
                Log.d("NsdHelper", "Service registered: ${_serviceName.value}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isServiceRegistered.value = false
                Log.e("NsdHelper", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                _isServiceRegistered.value = false
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isServiceRegistered.value = false
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        if (_discoveryState.value == DiscoveryState.SEARCHING) return
        
        _discoveryState.value = DiscoveryState.SEARCHING
        _discoveredDevices.value = emptySet()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdHelper", "Discovery failed: $errorCode")
                _discoveryState.value = DiscoveryState.ERROR
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NsdHelper", "Stop discovery failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
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
                // Clean up service names for comparison (some devices append numbers)
                val isOwnService = serviceInfo.serviceName == _serviceName.value || 
                                 serviceInfo.serviceName.startsWith(_serviceName.value)
                
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
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NsdHelper", "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                // If we found a device, cancel timeout and keep searching
                timeoutJob?.cancel()
                
                val host = if (android.os.Build.VERSION.SDK_INT >= 34) {
                    resolvedServiceInfo.host
                } else {
                    @Suppress("DEPRECATION")
                    resolvedServiceInfo.host
                }
                Log.d("NsdHelper", "Service resolved: $host:${resolvedServiceInfo.port}")
                val device = Device(
                    name = resolvedServiceInfo.serviceName,
                    host = host,
                    port = resolvedServiceInfo.port
                )
                _discoveredDevices.update { it + device }
            }
        }

        @Suppress("DEPRECATION")
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(15000) // 15 seconds timeout
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
            nsdManager.unregisterService(it)
        }
        registrationListener = null
    }
}
