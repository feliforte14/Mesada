package com.mesada.app.hardware

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mesada.app.data.HardwareSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Cliente BLE para la balanza (ver firmware/03_lectura_ble). Coordinado con [HardwareSettings]:
 * si el usuario no activó la balanza, [start] no toca ninguna API de Bluetooth y el estado queda
 * en [ScaleConnectionState.DISABLED]. Pensado para funcionar (o fallar en silencio, sin crashear
 * la app) tanto si el hardware nunca se compró como si está apagado o fuera de rango.
 */
class BleScaleSource(
    private val context: Context,
    private val settings: HardwareSettings,
) : ScaleSource {

    private val _connectionState = MutableStateFlow(ScaleConnectionState.DISABLED)
    override val connectionState: StateFlow<ScaleConnectionState> = _connectionState

    private val _grams = MutableStateFlow<Double?>(null)
    override val grams: StateFlow<Double?> = _grams

    private var gatt: BluetoothGatt? = null

    override fun start() {
        if (!settings.scaleEnabled.value) {
            _connectionState.value = ScaleConnectionState.DISABLED
            return
        }
        if (!hasBlePermissions()) {
            _connectionState.value = ScaleConnectionState.DISCONNECTED
            return
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ScaleConnectionState.DISCONNECTED
            return
        }

        _connectionState.value = ScaleConnectionState.CONNECTING
        try {
            adapter.bluetoothLeScanner?.startScan(scanCallback)
        } catch (e: SecurityException) {
            _connectionState.value = ScaleConnectionState.DISCONNECTED
        }
    }

    override fun stop() {
        try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            gatt?.disconnect()
            gatt?.close()
        } catch (e: SecurityException) {
            // Sin permiso no hay nada que cerrar del lado del sistema.
        }
        gatt = null
        _connectionState.value = if (settings.scaleEnabled.value) ScaleConnectionState.DISCONNECTED else ScaleConnectionState.DISABLED
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name != DEVICE_NAME) return
            try {
                val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                adapter?.bluetoothLeScanner?.stopScan(this)
                gatt = result.device.connectGatt(context, false, gattCallback)
            } catch (e: SecurityException) {
                _connectionState.value = ScaleConnectionState.DISCONNECTED
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ScaleConnectionState.CONNECTED
                    try { g.discoverServices() } catch (e: SecurityException) { }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    try { g.close() } catch (e: SecurityException) { }
                    gatt = null
                    _connectionState.value = if (settings.scaleEnabled.value) ScaleConnectionState.DISCONNECTED else ScaleConnectionState.DISABLED
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val characteristic = g.getService(SERVICE_UUID)?.getCharacteristic(WEIGHT_CHAR_UUID) ?: return
            try {
                g.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(CCCD_UUID) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(descriptor)
                }
            } catch (e: SecurityException) { }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            onWeightPayload(value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return // el overload de 3 args ya lo maneja
            onWeightPayload(characteristic.value ?: return)
        }
    }

    private fun onWeightPayload(value: ByteArray) {
        value.toString(Charsets.UTF_8).trim().toDoubleOrNull()?.let { _grams.value = it }
    }

    private fun hasBlePermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    companion object {
        private const val DEVICE_NAME = "Mesada-Balanza"
        // Coordinados con firmware/03_lectura_ble/03_lectura_ble.ino — cambiar en ambos lados a la vez.
        private val SERVICE_UUID: UUID = UUID.fromString("5b1e0001-1a2b-4c3d-9e8f-abc123456789")
        private val WEIGHT_CHAR_UUID: UUID = UUID.fromString("5b1e0002-1a2b-4c3d-9e8f-abc123456789")
        // Client Characteristic Configuration Descriptor — estándar BLE, no propio de Mesada.
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
