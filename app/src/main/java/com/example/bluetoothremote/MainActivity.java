package com.example.bluetoothremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 主 Activity：虚拟蓝牙遥控器界面
 *
 * 界面包含：
 *   - 顶部连接状态栏（显示状态和已连接设备名称）
 *   - 方向键区域（上下左右 + 中间 OK 键）
 *   - 功能键区域（Home、Back、Menu）
 *   - 多媒体键区域（音量加减、静音、播放/暂停）
 *   - 电源键
 *   - 连接/断开按钮
 */
public class MainActivity extends AppCompatActivity implements ConnectionStateCallback {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 2001;

    // 蓝牙 HID 管理器
    private BluetoothHidManager mHidManager;

    // UI 组件
    private TextView mTvStatus;          // 连接状态文本
    private TextView mTvDeviceName;      // 已连接设备名称
    private Button   mBtnConnect;        // 连接 / 断开按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 UI 组件
        initViews();

        // 检查并请求蓝牙权限
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestPermissions(this);
        } else {
            initBluetooth();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHidManager != null) {
            mHidManager.stop();
        }
    }

    // ==========================================================
    // UI 初始化
    // ==========================================================

    /**
     * 初始化所有视图和按钮点击监听
     */
    private void initViews() {
        mTvStatus     = findViewById(R.id.tv_status);
        mTvDeviceName = findViewById(R.id.tv_device_name);
        mBtnConnect   = findViewById(R.id.btn_connect);

        // 连接 / 断开按钮
        mBtnConnect.setOnClickListener(v -> {
            if (mHidManager != null && mHidManager.isConnected()) {
                mHidManager.disconnect();
            } else if (mHidManager != null && !mHidManager.isAdvertising()) {
                mHidManager.stop();
                mHidManager.start();
            }
        });

        // ---------- 方向键 + OK 键 ----------
        findViewById(R.id.btn_up).setOnClickListener(v ->
                sendKeyboard(RemoteKeyCode.KEY_UP));

        findViewById(R.id.btn_down).setOnClickListener(v ->
                sendKeyboard(RemoteKeyCode.KEY_DOWN));

        findViewById(R.id.btn_left).setOnClickListener(v ->
                sendKeyboard(RemoteKeyCode.KEY_LEFT));

        findViewById(R.id.btn_right).setOnClickListener(v ->
                sendKeyboard(RemoteKeyCode.KEY_RIGHT));

        findViewById(R.id.btn_ok).setOnClickListener(v ->
                sendKeyboard(RemoteKeyCode.KEY_ENTER));

        // ---------- 功能键 ----------
        findViewById(R.id.btn_home).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_AC_HOME));

        findViewById(R.id.btn_back).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_AC_BACK));

        findViewById(R.id.btn_menu).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_MENU));

        // ---------- 多媒体键 ----------
        findViewById(R.id.btn_vol_up).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_VOLUME_UP));

        findViewById(R.id.btn_vol_down).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_VOLUME_DOWN));

        findViewById(R.id.btn_mute).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_MUTE));

        findViewById(R.id.btn_play_pause).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_PLAY_PAUSE));

        // ---------- 电源键 ----------
        findViewById(R.id.btn_power).setOnClickListener(v ->
                sendConsumer(RemoteKeyCode.CONSUMER_POWER));

        // 初始状态
        updateStatus(getString(R.string.status_not_connected), null);
    }

    // ==========================================================
    // 蓝牙初始化
    // ==========================================================

    /**
     * 初始化蓝牙模块：检查蓝牙是否开启，创建 HidManager 并启动
     */
    private void initBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showToast(getString(R.string.error_bt_not_supported));
            return;
        }
        if (!adapter.isEnabled()) {
            // 请求开启蓝牙
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            return;
        }
        startHidManager();
    }

    /**
     * 创建并启动 BluetoothHidManager
     */
    private void startHidManager() {
        mHidManager = new BluetoothHidManager(this, this);
        mHidManager.start();
        updateStatus(getString(R.string.status_initializing), null);
    }

    // ==========================================================
    // 按键发送辅助方法
    // ==========================================================

    /**
     * 发送键盘按键（无修饰键）
     *
     * @param keyCode 键码，参见 {@link RemoteKeyCode}
     */
    private void sendKeyboard(byte keyCode) {
        if (mHidManager == null || !mHidManager.isConnected()) {
            showToast(getString(R.string.error_not_connected));
            return;
        }
        mHidManager.sendKeyboardKey((byte) 0x00, keyCode);
    }

    /**
     * 发送消费者控制按键
     *
     * @param usage 消费者键码，参见 {@link RemoteKeyCode}
     */
    private void sendConsumer(int usage) {
        if (mHidManager == null || !mHidManager.isConnected()) {
            showToast(getString(R.string.error_not_connected));
            return;
        }
        mHidManager.sendConsumerKey(usage);
    }

    // ==========================================================
    // ConnectionStateCallback 实现
    // ==========================================================

    @Override
    public void onAdvertisingStarted() {
        runOnUiThread(() -> {
            updateStatus(getString(R.string.status_advertising), null);
            mBtnConnect.setText(R.string.btn_stop_advertising);
        });
    }

    @Override
    public void onAdvertisingStopped(int errorCode) {
        runOnUiThread(() -> {
            if (!mHidManager.isConnected()) {
                updateStatus(getString(R.string.status_not_connected), null);
                mBtnConnect.setText(R.string.btn_start_advertising);
            }
        });
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        runOnUiThread(() -> {
            String name = device.getName();
            if (name == null || name.isEmpty()) name = device.getAddress();
            updateStatus(getString(R.string.status_connected), name);
            mBtnConnect.setText(R.string.btn_disconnect);
            showToast(getString(R.string.toast_connected, name));
        });
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        runOnUiThread(() -> {
            updateStatus(getString(R.string.status_advertising), null);
            mBtnConnect.setText(R.string.btn_stop_advertising);
            showToast(getString(R.string.toast_disconnected));
        });
    }

    @Override
    public void onError(String errorMsg) {
        runOnUiThread(() -> {
            showToast(getString(R.string.toast_error, errorMsg));
            updateStatus(getString(R.string.status_error), null);
        });
    }

    // ==========================================================
    // 权限和 Activity 结果处理
    // ==========================================================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (PermissionHelper.isGranted(grantResults)) {
                initBluetooth();
            } else {
                showToast(getString(R.string.error_permission_denied));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startHidManager();
            } else {
                showToast(getString(R.string.error_bt_not_enabled));
            }
        }
    }

    // ==========================================================
    // 工具方法
    // ==========================================================

    /**
     * 更新顶部状态栏显示
     *
     * @param status     状态文本
     * @param deviceName 设备名称（可为 null）
     */
    private void updateStatus(String status, String deviceName) {
        mTvStatus.setText(status);
        if (deviceName != null) {
            mTvDeviceName.setText(deviceName);
            mTvDeviceName.setVisibility(View.VISIBLE);
        } else {
            mTvDeviceName.setVisibility(View.GONE);
        }
    }

    /**
     * 显示简短的 Toast 提示
     *
     * @param msg 提示信息
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
