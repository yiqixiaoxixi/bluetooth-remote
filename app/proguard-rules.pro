# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's default ProGuard configuration.

# Keep BluetoothHidDevice related classes
-keep class android.bluetooth.BluetoothHidDevice { *; }
-keep class android.bluetooth.BluetoothHidDeviceAppSdpSettings { *; }
-keep class android.bluetooth.BluetoothHidDeviceAppQosSettings { *; }
