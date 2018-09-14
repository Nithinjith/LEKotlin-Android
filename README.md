# LEKotlin-Android
This is Kotlin Application of Bluetooth Low Energy Device Integration. The module explains a working sample of the BLE device. The App will initially scan the surrounding devices, the scan result will pass to the Activity. The user can select a device from the scan result. If the scanning process over, the user can connect the device. After connecting the device, the application will check the Available Service in the device and Available characteristics in the Device. If the Service and characteristics are identified then the user can start read-write and notify process.

The Application creates a server-client model.

A bounded service is used to read, write and notify data from the BLE

The data will be updated to the Activity through a Broadcast receiver

Two singleton class is used in the BLE module

One for Scan Handling and the Other one for reading, writing and find services.

