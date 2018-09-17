package com.np.lekotlin.blemodule

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.np.lekotlin.R

object BLEConnectionManager {

    private val TAG = "BLEConnectionManager"
    private var mBLEService: BLEService? = null
    private var isBind = false
    private var mDataBLEForEmergency: BluetoothGattCharacteristic? = null

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBLEService = (service as BLEService.LocalBinder).getService()

            if (!mBLEService?.initialize()!!) {
                Log.e(TAG, "Unable to initialize")
            }
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            mBLEService = null
        }
    }

    /**
     * Initialize Bluetooth service.
     */
    fun initBLEService(context: Context) {
        try {

            if (mBLEService == null) {
                val gattServiceIntent = Intent(context, BLEService::class.java)

                if (context != null) {
                    isBind = context.bindService(gattServiceIntent, mServiceConnection,
                            Context.BIND_AUTO_CREATE)
                }

            }

        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }

    }

    /**
     * Unbind BLE Service
     */
    fun unBindBLEService(context: Context) {

        if (mServiceConnection != null && isBind) {
            context.unbindService(mServiceConnection)
        }

        mBLEService = null
    }

    /**
     * Connect to a BLE Device
     */
    fun connect(deviceAddress: String): Boolean {
        var result = false

        if (mBLEService != null) {
            result = mBLEService!!.connect(deviceAddress)
        }

        return result

    }

    /**
     * Disconnect
     */
    fun disconnect() {
        if (null != mBLEService) {
            mBLEService!!.disconnect()
            mBLEService = null
        }

    }

    fun writeEmergencyGatt(value: ByteArray) {
        if (mDataBLEForEmergency != null) {
            mDataBLEForEmergency!!.value = value
            writeBLECharacteristic(mDataBLEForEmergency)
        }
    }

    fun writeBatteryLevel(batteryLevel: String) {
        if(batteryLevel != null){
//            writeBLECharacteristic(mDataMDLPForMissedConnection);
        }
    }

    fun writeMissedConnection(value: String) {
        var gattCharacteristic =  BluetoothGattCharacteristic(java.util.UUID.
                fromString(value), PROPERTY_WRITE, PERMISSION_WRITE)
        if (gattCharacteristic != null) {
            gattCharacteristic.setValue(value)
            writeBLECharacteristic(gattCharacteristic)
        }
    }

    fun writeEmergencyGatt(value: String) {
        var mDataMDLPForEmergency =  BluetoothGattCharacteristic(java.util.UUID.
                fromString(value), PROPERTY_READ, PERMISSION_READ)
        if (mDataMDLPForEmergency != null) {
            mDataMDLPForEmergency.setValue(value)
            writeBLECharacteristic(mDataMDLPForEmergency)
        }
    }

    /**
     * Write BLE Characteristic.
     */
    private fun writeBLECharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (null != characteristic) {
            if (mBLEService != null) {
                mBLEService?.writeCharacteristic(characteristic)
            }
        }
    }

    fun readMissedConnection(UUID: String) {
        var gattCharacteristic =  BluetoothGattCharacteristic(java.util.UUID.
                fromString(UUID), PROPERTY_READ, PERMISSION_READ)
        if (gattCharacteristic != null) {
            readMLDPCharacteristic(gattCharacteristic)
        }
    }

    fun readBatteryLevel(UUID: String) {
        var gattCharacteristic =  BluetoothGattCharacteristic(java.util.UUID.
                fromString(UUID), PROPERTY_READ, PERMISSION_READ)
        if (gattCharacteristic != null) {
            readMLDPCharacteristic(gattCharacteristic)
        }
    }

    fun readEmergencyGatt(UUID: String) {
        var gattCharacteristic =  BluetoothGattCharacteristic(java.util.UUID.
                fromString(UUID), PROPERTY_READ, PERMISSION_READ)
        if (gattCharacteristic != null) {
            readMLDPCharacteristic(gattCharacteristic)
        }
    }

    /**
     * Write MLDP Characteristic.
     */
    private fun readMLDPCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (null != characteristic) {
            if (mBLEService != null) {
                mBLEService?.readCharacteristic(characteristic)
            }
        }
    }


    /**
     * findBLEGattService
     */
    fun findBLEGattService(mContext: Context) {

        if (mBLEService == null) {
            return
        }

        if (mBLEService!!.getSupportedGattServices() == null) {
            return
        }

        var uuid: String
        mDataBLEForEmergency = null
        val serviceList = mBLEService!!.getSupportedGattServices()

        if (serviceList != null) {
            for (gattService in serviceList) {

                if (gattService.getUuid().toString().equals(mContext.getString(R.string.char_uuid_emergency),
                                ignoreCase = true)) {
                    val gattCharacteristics = gattService.characteristics
                    for (gattCharacteristic in gattCharacteristics) {

                        uuid = if (gattCharacteristic.uuid != null) gattCharacteristic.uuid.toString() else ""

                        if (uuid.equals(mContext.resources.getString(R.string.char_uuid_emergency_call),
                                        ignoreCase = true)) {
                            var newChar = gattCharacteristic
                            newChar = setProperties(newChar)
                            mDataBLEForEmergency = newChar
                        }
                    }
                }

            }
        }

    }

    private fun setProperties(gattCharacteristic: BluetoothGattCharacteristic):
            BluetoothGattCharacteristic {
        val characteristicProperties = gattCharacteristic.properties

        if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            mBLEService?.setCharacteristicNotification(gattCharacteristic, true)
        }

        if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
            mBLEService?.setCharacteristicIndication(gattCharacteristic, true)
        }

        if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
            gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
            gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
            gattCharacteristic.writeType = BluetoothGattCharacteristic.PROPERTY_READ
        }
        return gattCharacteristic
    }

}