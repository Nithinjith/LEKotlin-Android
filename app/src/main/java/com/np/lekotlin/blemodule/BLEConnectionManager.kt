package com.np.lekotlin.blemodule

import android.bluetooth.BluetoothGattCharacteristic
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
    private var mDataMDLPForEmergency: BluetoothGattCharacteristic? = null

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
        if (mDataMDLPForEmergency != null) {
            mDataMDLPForEmergency!!.value = value
            writeMLDPCharacteristic(mDataMDLPForEmergency)
        }
    }

    /**
     * Write MLDP Characteristic.
     */
    private fun writeMLDPCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (null != characteristic && mBLEService != null) {
                mBLEService!!.writeCharacteristic(characteristic)
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
        mDataMDLPForEmergency = null
        val serviceList = mBLEService!!.getSupportedGattServices()

        if (serviceList != null) {
            for (gattService in serviceList) {

                if (gattService.getUuid().toString().equals(mContext.getString(R.string.char_uuid_emergency),
                                ignoreCase = true)) {
                    val gattCharacteristics = gattService.characteristics
                    for (gattCharacteristic in gattCharacteristics) {

                        uuid = if (gattCharacteristic.uuid != null) gattCharacteristic.uuid.toString() else ""

                        if (uuid.equals(mContext.resources.getString(R.string.char_uuid_emergency_gatt),
                                        ignoreCase = true)) {
                            var newChar = gattCharacteristic
                            newChar = setProperties(newChar)
                            mDataMDLPForEmergency = newChar
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