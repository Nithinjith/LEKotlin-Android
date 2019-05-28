package com.np.lekotlin.blemodule

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import java.util.ArrayList

object BLEDeviceManager {

    private val TAG = "BLEDeviceManager"
    private var scanCallback: ScanCallback? = null
    private var mDeviceObject: BleDeviceData? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mHandler: Handler? = null
    private var mOnDeviceScanListener: OnDeviceScanListener? = null
    private var mLeScanCallback: BluetoothAdapter.LeScanCallback? = null
    private var mIsContinuesScan: Boolean = false

    private fun isLollyPopOrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    init {
        mHandler = Handler()
        if (isLollyPopOrAbove()) {
            createScanCallBackAboveLollipop()
        } else {
            createScanCallBackBelowLollipop()
        }
    }

    /**
     * ScanCallback for Lollipop and above
     * The Callback will trigger the Nearest available BLE devices
     * Search the BLE device in Range and pull the Name and Mac Address from it
     */
    private fun createScanCallBackAboveLollipop() {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                if (null != mOnDeviceScanListener && result != null &&
                        result.device != null && result.device.address != null) {
                    val data = BleDeviceData()
                    data.mDeviceName = if (result.device.name != null)
                        result.device.name else "Unknown"
                    // Some case the Device Name will return as Null from BLE
                    // because of Swathing from one device to another
                    data.mDeviceAddress = (result.device.address)
                    /**
                     * Save the Valid Device info into a list
                     * The List will display to the UI as a popup
                     * User has an option to select one BLE from the popup
                     * After selecting one BLE, the connection will establish and
                     * communication channel will create if its valid device.
                     */

                    if (data.mDeviceName.contains("Invisa") || data.mDeviceName.
                                    contains("invisa")) {
                        mDeviceObject = data
                        stopScan(mDeviceObject)
                    }
                }
            }
        }
    }

    /**
     * ScanCallback for below Lollipop.
     * The Callback will trigger the Nearest available BLE devices
     * Search the BLE device in Range and pull the Name and Mac Address from it
     */
    private fun createScanCallBackBelowLollipop() {
        mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
            if (device != null && device.address != null && null != mOnDeviceScanListener) {
                // Some case the Device Name will return as Null from BLE because of Swathing from one device to another
                val data = BleDeviceData()
                data.mDeviceName  = (device.name)
                data.mDeviceAddress = (device.address)

                /**
                 * Save the Valid Device info into a list
                 * The List will display to the UI as a popup
                 * User has an option to select one BLE from the popup
                 * After selecting one BLE, the connection will establish and
                 * communication channel will create if its valid device.
                 */

                /**
                 * Save the Valid Device info into a list
                 * The List will display to the UI as a popup
                 * User has an option to select one BLE from the popup
                 * After selecting one BLE, the connection will establish and communication
                 * channel will create if its valid device.
                 */

                if (data.mDeviceName.contains("Invisa") || data.mDeviceName.contains("invisa")) {
                    mDeviceObject = data
                    stopScan(mDeviceObject)
                }

            }
        }
    }

    /**
     * Initialize BluetoothAdapter
     * Check the device has the hardware feature BLE
     * Then enable the hardware,
     */
    fun init(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        return mBluetoothAdapter != null && context.packageManager.
                hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    /**
     * Check bluetooth is enabled or not.
     */
    fun isEnabled(): Boolean {

        return mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled
    }

    /**
     * setListener
     */
    fun setListener(onDeviceScanListener: OnDeviceScanListener) {
        mOnDeviceScanListener = onDeviceScanListener
    }

    /**
     * Scan The BLE Device
     * Check the available BLE devices in the Surrounding
     * If the device is Already scanning then stop Scanning
     * Else start Scanning and check 10 seconds
     * Send the available devices as a callback to the system
     * Finish Scanning after 10 Seconds
     */
    fun scanBLEDevice(isContinuesScan: Boolean) {
        try {
            mIsContinuesScan = isContinuesScan

            if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled) {
                scan()
            }
            /**
             * Stop Scanning after a Period of Time
             * Set a 10 Sec delay time and Stop Scanning
             * collect all the available devices in the 10 Second
             */
           /* if (!isContinuesScan) {
                mHandler?.postDelayed({
                    // Set a delay time to Scanning
                    stopScan(mDeviceObject)
                }, BLEConstants.SCAN_PERIOD) // Delay Period
            }*/
        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }

    }



    private fun scan() {
        if (isLollyPopOrAbove()) {// Start Scanning For Lollipop devices
            mBluetoothAdapter?.bluetoothLeScanner?.startScan(/*scanFilters(),
            scanSettings(),*/scanCallback) // Start BLE device Scanning in a separate thread
        } else {
            mBluetoothAdapter?.startLeScan(mLeScanCallback) // Start Scanning for Below Lollipop device
        }
    }

    private fun scanFilters(): List<ScanFilter> {
        val missedConnectionUUID = ""// Your UUID
        val emergencyUDID = ""// Your UUID
        val catchUDID = ""// Your UUID
        val catchAllUDID = ""// Your UUID
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(emergencyUDID)).build()
        val list = ArrayList<ScanFilter>(1)
        list.add(filter)
        return list
    }

    private fun scanSettings(): ScanSettings {
        return ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
    }

    private fun stopScan(data: BleDeviceData?) {
        try {

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled &&
                    if (isLollyPopOrAbove()) scanCallback != null else mLeScanCallback != null) {
                if (mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled) { // check if its Already available
                    if (isLollyPopOrAbove()) {
                        mBluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
                    } else {
                        mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                    }
                }
                if (data != null) {
                    mOnDeviceScanListener?.onScanCompleted(data)
                }
            }
        }
    }
}