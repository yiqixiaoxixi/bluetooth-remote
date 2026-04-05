package com.example.bluetoothremote;

import android.bluetooth.BluetoothDevice;

/**
 * 蓝牙连接状态回调接口
 *
 * BluetoothHidManager 通过此接口将连接状态变化通知给 MainActivity，
 * 实现解耦的事件驱动架构。
 */
public interface ConnectionStateCallback {

    /**
     * 当 BLE HID 广播开始时回调
     */
    void onAdvertisingStarted();

    /**
     * 当 BLE HID 广播停止时回调
     *
     * @param errorCode 0 表示主动停止，非 0 表示错误码
     */
    void onAdvertisingStopped(int errorCode);

    /**
     * 当手机（HID Host）成功连接时回调
     *
     * @param device 已连接的蓝牙设备
     */
    void onDeviceConnected(BluetoothDevice device);

    /**
     * 当手机（HID Host）断开连接时回调
     *
     * @param device 断开连接的蓝牙设备
     */
    void onDeviceDisconnected(BluetoothDevice device);

    /**
     * 当发生错误时回调
     *
     * @param errorMsg 错误描述信息
     */
    void onError(String errorMsg);
}
