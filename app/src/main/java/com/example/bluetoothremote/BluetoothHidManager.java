package com.example.bluetoothremote;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.UUID;

/**
 * BLE HID Device 管理器（核心）
 *
 * 负责：
 *   1. 注册蓝牙 HID Device Profile（BluetoothHidDevice）
 *   2. 启动 BLE 广播（Advertising），让手机能发现机顶盒
 *   3. 处理 HID Host（手机）的连接 / 断开事件
 *   4. 发送 HID 按键报告（Key Down + Key Up）
 *   5. 支持手动断开连接
 *
 * 使用方式：
 *   BluetoothHidManager manager = new BluetoothHidManager(context, callback);
 *   manager.start();       // 开始注册 HID 并广播
 *   manager.sendKey(...);  // 发送按键
 *   manager.stop();        // 停止广播并注销 HID
 */
@SuppressLint("MissingPermission")
public class BluetoothHidManager {

    private static final String TAG = "BluetoothHidManager";

    // HID Over GATT Profile Service UUID（标准 UUID）
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");

    // HID SDP 设置中的设备描述信息
    private static final String SDP_NAME        = "BT Remote Control";
    private static final String SDP_DESCRIPTION = "Android STB HID Remote";
    private static final String SDP_PROVIDER    = "BluetoothRemote";

    // 按键报告发送后，自动发送"按键释放"报告的延迟（毫秒）
    private static final long KEY_UP_DELAY_MS = 80L;

    private final Context mContext;
    private final ConnectionStateCallback mCallback;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHidDevice mHidDevice;          // HID Device Profile 代理
    private BluetoothDevice mConnectedDevice;       // 当前连接的 HID Host 设备

    private BluetoothLeAdvertiser mLeAdvertiser;    // BLE 广播器
    private boolean mIsAdvertising = false;
    private boolean mIsRegistered  = false;

    /**
     * 构造函数
     *
     * @param context  应用上下文
     * @param callback 连接状态回调（由 MainActivity 实现）
     */
    public BluetoothHidManager(Context context, ConnectionStateCallback callback) {
        mContext  = context.getApplicationContext();
        mCallback = callback;
    }

    // ==========================================================
    // 公开 API
    // ==========================================================

    /**
     * 初始化并启动 HID Device 注册流程
     * 注册成功后自动开始 BLE 广播
     */
    public void start() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            notifyError("设备不支持蓝牙管理器");
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            notifyError("蓝牙未启用，请先开启蓝牙");
            return;
        }

        // 注册 HID Device Profile（异步，结果通过 ServiceListener 回调）
        boolean success = mBluetoothAdapter.getProfileProxy(
                mContext, mServiceListener, BluetoothProfile.HID_DEVICE);
        if (!success) {
            notifyError("获取 HID Device Profile 代理失败");
        }
    }

    /**
     * 停止广播并注销 HID Device Profile
     */
    public void stop() {
        stopAdvertising();

        if (mHidDevice != null && mIsRegistered) {
            try {
                mHidDevice.unregisterApp();
            } catch (Exception e) {
                Log.w(TAG, "注销 HID App 异常: " + e.getMessage());
            }
            mIsRegistered = false;
        }

        if (mBluetoothAdapter != null && mHidDevice != null) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, mHidDevice);
            mHidDevice = null;
        }
    }

    /**
     * 断开当前已连接的设备
     */
    public void disconnect() {
        if (mHidDevice != null && mConnectedDevice != null) {
            mHidDevice.disconnect(mConnectedDevice);
            mConnectedDevice = null;
        }
    }

    /**
     * 获取当前连接的设备（如未连接则返回 null）
     */
    public BluetoothDevice getConnectedDevice() {
        return mConnectedDevice;
    }

    /**
     * 判断是否有设备已连接
     */
    public boolean isConnected() {
        return mConnectedDevice != null;
    }

    /**
     * 判断当前是否正在广播
     */
    public boolean isAdvertising() {
        return mIsAdvertising;
    }

    // ==========================================================
    // 按键发送（HID Report）
    // ==========================================================

    /**
     * 发送一次键盘按键（自动发送 Key Down + Key Up）
     *
     * @param modifier 修饰键字节（例如 Ctrl=0x01, Shift=0x02，无修饰=0x00）
     * @param keyCode  键盘键码，参见 {@link RemoteKeyCode}（Keyboard Usage Page 0x07）
     */
    public void sendKeyboardKey(byte modifier, byte keyCode) {
        if (!isConnected()) {
            Log.w(TAG, "sendKeyboardKey: 未连接设备");
            return;
        }

        // Key Down 报告：8 字节（modifier, reserved, key1, key2..6）
        byte[] keyDown = new byte[]{modifier, 0x00, keyCode, 0x00, 0x00, 0x00, 0x00, 0x00};
        sendReport(RemoteKeyCode.REPORT_ID_KEYBOARD, keyDown);

        // 延迟后发送 Key Up（所有字节清零）
        mMainHandler.postDelayed(() -> {
            byte[] keyUp = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            sendReport(RemoteKeyCode.REPORT_ID_KEYBOARD, keyUp);
        }, KEY_UP_DELAY_MS);
    }

    /**
     * 发送一次消费者控制按键（自动发送 Key Down + Key Up）
     *
     * @param consumerUsage 消费者键码（16-bit），参见 {@link RemoteKeyCode}（Consumer Usage Page 0x0C）
     */
    public void sendConsumerKey(int consumerUsage) {
        if (!isConnected()) {
            Log.w(TAG, "sendConsumerKey: 未连接设备");
            return;
        }

        // Consumer Control 报告：2 字节，小端序
        byte[] keyDown = new byte[]{
                (byte) (consumerUsage & 0xFF),
                (byte) ((consumerUsage >> 8) & 0xFF)
        };
        sendReport(RemoteKeyCode.REPORT_ID_CONSUMER, keyDown);

        // 延迟后发送 Key Up（释放）
        mMainHandler.postDelayed(() -> {
            byte[] keyUp = new byte[]{0x00, 0x00};
            sendReport(RemoteKeyCode.REPORT_ID_CONSUMER, keyUp);
        }, KEY_UP_DELAY_MS);
    }

    // ==========================================================
    // 内部实现：HID Profile 注册
    // ==========================================================

    /**
     * BluetoothProfile.ServiceListener
     * 当 HID Device Profile 代理获取成功时，注册 HID App
     */
    private final BluetoothProfile.ServiceListener mServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile != BluetoothProfile.HID_DEVICE) return;
                    Log.d(TAG, "HID Device Profile 代理已获取");
                    mHidDevice = (BluetoothHidDevice) proxy;
                    registerHidApp();
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    Log.d(TAG, "HID Device Profile 代理已断开");
                    mHidDevice = null;
                    mIsRegistered = false;
                }
            };

    /**
     * 向 BluetoothHidDevice 注册 HID 应用（包含 SDP 设置和 Report Descriptor）
     */
    private void registerHidApp() {
        if (mHidDevice == null) return;

        // SDP（Service Discovery Protocol）设置：定义设备名称、描述、SubClass 等
        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                SDP_NAME,
                SDP_DESCRIPTION,
                SDP_PROVIDER,
                BluetoothHidDevice.SUBCLASS1_COMBO,  // 复合设备（键盘+消费者控制）
                HidReportDescriptor.DESCRIPTOR
        );

        // QoS 设置：使用默认值（null 表示不限制）
        BluetoothHidDeviceAppQosSettings qosSettings = new BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, (int) (11.25 * 1000), (int) (11.25 * 1000)
        );

        boolean registered = mHidDevice.registerApp(
                sdpSettings,
                null,           // inQos，使用 null 表示默认
                qosSettings,
                Runnable::run,  // executor，在调用线程执行回调
                mHidCallback
        );

        if (registered) {
            Log.d(TAG, "HID App 注册请求已发送，等待回调...");
        } else {
            Log.e(TAG, "HID App 注册请求失败");
            notifyError("HID App 注册失败，请确认设备支持 HID Device 模式");
        }
    }

    /**
     * BluetoothHidDevice.Callback
     * 处理 HID 设备的连接状态变化和报告发送结果
     */
    private final BluetoothHidDevice.Callback mHidCallback = new BluetoothHidDevice.Callback() {

        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            Log.d(TAG, "onAppStatusChanged: registered=" + registered);
            mIsRegistered = registered;
            if (registered) {
                // HID App 注册成功，开始 BLE 广播
                mMainHandler.post(() -> startAdvertising());
            } else {
                mMainHandler.post(() -> stopAdvertising());
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.d(TAG, "onConnectionStateChanged: " + device.getName() + " state=" + state);
            mMainHandler.post(() -> {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    mConnectedDevice = device;
                    stopAdvertising(); // 已连接，停止广播
                    if (mCallback != null) mCallback.onDeviceConnected(device);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectedDevice = null;
                    if (mCallback != null) mCallback.onDeviceDisconnected(device);
                    // 断开后重新开始广播，等待重新连接
                    startAdvertising();
                }
            });
        }

        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            // HID Host 请求获取 Report，返回空报告即可
            Log.d(TAG, "onGetReport: type=" + type + " id=" + id);
            if (mHidDevice != null) {
                mHidDevice.replyReport(device, type, id, new byte[bufferSize]);
            }
        }

        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            Log.d(TAG, "onSetReport: type=" + type + " id=" + id);
            if (mHidDevice != null) {
                mHidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
            }
        }

        @Override
        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            Log.d(TAG, "onSetProtocol: protocol=" + protocol);
        }

        @Override
        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
            Log.d(TAG, "onInterruptData: reportId=" + reportId);
        }

        @Override
        public void onVirtualCableUnplug(BluetoothDevice device) {
            Log.d(TAG, "onVirtualCableUnplug");
            mMainHandler.post(() -> {
                mConnectedDevice = null;
                if (mCallback != null) mCallback.onDeviceDisconnected(device);
            });
        }
    };

    // ==========================================================
    // 内部实现：BLE 广播
    // ==========================================================

    /**
     * 启动 BLE 广播（让手机能发现机顶盒作为 HID 设备）
     */
    private void startAdvertising() {
        if (mIsAdvertising) {
            Log.d(TAG, "已经在广播中，跳过");
            return;
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            notifyError("蓝牙未启用，无法开始广播");
            return;
        }

        mLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mLeAdvertiser == null) {
            notifyError("设备不支持 BLE 广播（可能不是 BLE 外设）");
            return;
        }

        // 广播设置：低延迟模式，可连接，包含设备名称
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)  // 持续广播
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        // 广播数据：包含 HID 服务 UUID
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(HID_SERVICE_UUID))
                .build();

        // 扫描响应数据（可选）
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        mLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);
        Log.d(TAG, "BLE 广播已启动");
    }

    /**
     * 停止 BLE 广播
     */
    private void stopAdvertising() {
        if (!mIsAdvertising) return;
        if (mLeAdvertiser != null) {
            try {
                mLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            } catch (Exception e) {
                Log.w(TAG, "停止广播异常: " + e.getMessage());
            }
        }
        mIsAdvertising = false;
        Log.d(TAG, "BLE 广播已停止");
        if (mCallback != null) mCallback.onAdvertisingStopped(0);
    }

    /**
     * BLE 广播回调
     */
    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE 广播启动成功");
            mIsAdvertising = true;
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onAdvertisingStarted();
            });
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "BLE 广播启动失败，错误码: " + errorCode);
            mIsAdvertising = false;
            String reason;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    reason = "广播已在运行";
                    mIsAdvertising = true;
                    return;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    reason = "广播数据过大";
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    reason = "设备不支持 BLE 广播";
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    reason = "内部错误";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    reason = "广播者数量超限";
                    break;
                default:
                    reason = "未知错误 " + errorCode;
            }
            mMainHandler.post(() -> notifyError("BLE 广播失败：" + reason));
        }
    };

    // ==========================================================
    // 内部工具方法
    // ==========================================================

    /**
     * 向已连接设备发送 HID Input Report
     *
     * @param reportId 报告 ID（1=键盘, 2=消费者控制）
     * @param data     报告数据字节数组
     */
    private void sendReport(byte reportId, byte[] data) {
        if (mHidDevice == null || mConnectedDevice == null) {
            Log.w(TAG, "sendReport: HID Device 或连接设备为空");
            return;
        }
        boolean success = mHidDevice.sendReport(mConnectedDevice, reportId, data);
        if (!success) {
            Log.w(TAG, "sendReport 失败: reportId=" + reportId);
        }
    }

    /**
     * 在主线程通知错误回调
     *
     * @param msg 错误信息
     */
    private void notifyError(String msg) {
        Log.e(TAG, "错误: " + msg);
        mMainHandler.post(() -> {
            if (mCallback != null) mCallback.onError(msg);
        });
    }
}
