package com.np.lekotlin.blemodule

import com.np.lekotlin.blemodule.BleDeviceData

interface OnDeviceScanListener {

    /**
     * Scan Completed -
     *
     * @param deviceDataList - Send available devices as a list to the init Activity
     * The List Contain, device name and mac address,
     */
     fun onScanCompleted(deviceDataList: BleDeviceData)
}