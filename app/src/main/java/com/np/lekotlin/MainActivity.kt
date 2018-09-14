package com.np.lekotlin

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.np.lekotlin.blemodule.*

class MainActivity : AppCompatActivity(), OnDeviceScanListener, View.OnClickListener {

    private lateinit var mBtnReadMissedChar: Button
    private lateinit var mBtnReadBatteryLevel: Button
    private lateinit var mBtnReadEmergency: Button
    private lateinit var mBtnWriteEmergency: Button
    private lateinit var mBtnWriteMissedConnection: Button
    private lateinit var mBtnWriteBatteryLevel: Button
    private lateinit var mTvResult: TextView

    override fun onScanCompleted(deviceDataList: BleDeviceData) {

        //Initiate a dialog Fragment from here and ask the user to select his device
        // If the application already know the Mac address, we can simply call connect device

        BLEConnectionManager.connect(deviceDataList.mDeviceAddress)
    }

    private val REQUEST_LOCATION_PERMISSION = 2018
    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT = 1000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBtnReadMissedChar = findViewById(R.id.btn_read_missed_connection)
        mBtnReadEmergency = findViewById(R.id.btn_read_emergency_gatt)
        mBtnReadBatteryLevel = findViewById(R.id.btn_read_battery_level)
        mBtnWriteEmergency = findViewById(R.id.btn_write_emergency)
        mBtnWriteMissedConnection = findViewById(R.id.btn_write_missed_connection)
        mBtnWriteBatteryLevel = findViewById(R.id.btn_write_battery)
        mTvResult = findViewById(R.id.tv_result)

        findViewById<View>(R.id.btn_scan).setOnClickListener(this)
        mBtnReadMissedChar.setOnClickListener(this)
        mBtnWriteEmergency.setOnClickListener(this)

        mBtnReadEmergency.setOnClickListener(this)
        mBtnReadBatteryLevel.setOnClickListener(this)

        mBtnWriteBatteryLevel.setOnClickListener(this)
        mBtnWriteMissedConnection.setOnClickListener(this)

        checkLocationPermission()
    }

    /**
     * Check the Location Permission before calling the BLE API's
     */
    private fun checkLocationPermission() {
        if (isAboveMarshmallow()) {
            when {
                isLocationPermissionEnabled() -> initBLEModule()
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) -> displayRationale()
                else -> requestLocationPermission()
            }
        } else {
            initBLEModule()
        }
    }

    /**
     * Request Location API
     * If the request go to Android system and the System will throw a dialog message
     * user can accept or decline the permission from there
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION)
    }

    /**
     * If the user decline the Permission request and tick the never ask again message
     * Then the application can't proceed further steps
     * In such situation- App need to prompt the user to do the change form Settings Manually
     */
    private fun displayRationale() {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.location_permission_disabled))
                .setPositiveButton(getString(R.string.ok)
                ) { _, _ -> requestLocationPermission() }
                .setNegativeButton(getString(R.string.cancel)
                ) { _, _ -> }
                .show()
    }

    /**
     * If the user either accept or reject the Permission- The requested App will get a callback
     * Form the call back we can filter the user response with the help of request key
     * If the user accept the same- We can proceed further steps
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (permissions.size != 1 || grantResults.size != 1) {
                    throw RuntimeException("Error on requesting location permission.")
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initBLEModule()
                } else {
                    Toast.makeText(this, R.string.location_permission_not_granted,
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Check with the system- If the permission already enabled or not
     */
    private fun isLocationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * The location permission is incorporated in Marshmallow and Above
     */
    private fun isAboveMarshmallow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }


    /**
     *After receive the Location Permission, the Application need to initialize the
     * BLE Module and BLE Service
     */
    private fun initBLEModule() {
        // BLE initialization
        if (!BLEDeviceManager.init(this)) {
            Toast.makeText(this, "BLE NOT SUPPORTED", Toast.LENGTH_SHORT).show()
            return
        }
        registerServiceReceiver()
        BLEDeviceManager.setListener(this)

        if (!BLEDeviceManager.isEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        BLEConnectionManager.initBLEService(this@MainActivity)
    }


    /**
     * Register GATT update receiver
     */
    private fun registerServiceReceiver() {
        this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }



    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                BLEConstants.ACTION_GATT_CONNECTED.equals(action) -> {
                    Log.i(TAG, "ACTION_GATT_CONNECTED ")
                    BLEConnectionManager.findBLEGattService(this@MainActivity)
                }
                BLEConstants.ACTION_GATT_DISCONNECTED.equals(action) -> {
                    Log.i(TAG, "ACTION_GATT_DISCONNECTED ")
                }
                BLEConstants.ACTION_GATT_SERVICES_DISCOVERED.equals(action) -> {
                    Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED ")
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    BLEConnectionManager.findBLEGattService(this@MainActivity)
                }
                BLEConstants.ACTION_DATA_AVAILABLE.equals(action) -> {
                    val data = intent.getStringExtra(BLEConstants.EXTRA_DATA)
                    val uuId = intent.getStringExtra(BLEConstants.EXTRA_UUID)
                    Log.i(TAG, "ACTION_DATA_AVAILABLE $data")

                }
                BLEConstants.ACTION_DATA_WRITTEN.equals(action) -> {
                    val data = intent.getStringExtra(BLEConstants.EXTRA_DATA)
                    Log.i(TAG, "ACTION_DATA_WRITTEN ")
                }
            }
        }
    }

    /**
     * Intent filter for Handling BLEService broadcast.
     */
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BLEConstants.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BLEConstants.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BLEConstants.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BLEConstants.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BLEConstants.ACTION_DATA_WRITTEN)

        return intentFilter
    }

    /**
     * Unregister GATT update receiver
     */
    private fun unRegisterServiceReceiver() {
        try {
            this.unregisterReceiver(mGattUpdateReceiver)
        } catch (e: Exception) {
            //May get an exception while user denies the permission and user exists the app
            Log.e(TAG, e.message)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        BLEConnectionManager.disconnect()
        unRegisterServiceReceiver()
    }

    override fun onClick(v: View?) {

    }


}
