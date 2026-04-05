package com.example.bluetoothremote;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Android 11 蓝牙权限处理助手
 *
 * 负责检查和请求蓝牙相关运行时权限。
 * Android 11 (API 30) 需要：
 *   - BLUETOOTH
 *   - BLUETOOTH_ADMIN
 *   - ACCESS_FINE_LOCATION（BLE 扫描/广播需要）
 *
 * Android 12+ (API 31+) 额外需要：
 *   - BLUETOOTH_CONNECT
 *   - BLUETOOTH_ADVERTISE
 *   - BLUETOOTH_SCAN
 */
public class PermissionHelper {

    /** 权限请求码，用于 onRequestPermissionsResult 区分 */
    public static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1001;

    /**
     * 检查所有必要的蓝牙权限是否已授权
     *
     * @param activity 当前 Activity
     * @return true 表示所有权限已授权，false 表示缺少权限
     */
    public static boolean hasAllPermissions(Activity activity) {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 请求所有缺少的蓝牙权限
     *
     * @param activity 当前 Activity
     */
    public static void requestPermissions(Activity activity) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    missingPermissions.toArray(new String[0]),
                    REQUEST_CODE_BLUETOOTH_PERMISSIONS
            );
        }
    }

    /**
     * 检查权限请求结果，判断是否所有权限均已授予
     *
     * @param grantResults 权限授予结果数组
     * @return true 表示全部授予
     */
    public static boolean isGranted(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前 Android 版本需要请求的权限列表
     *
     * @return 权限字符串数组
     */
    private static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Android 9/10/11 基础蓝牙权限（普通权限，不需要运行时请求，但统一处理）
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);

        // BLE 扫描/广播在 Android 11 及以下需要位置权限
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        // Android 12+ 新增的细粒度蓝牙权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        return permissions.toArray(new String[0]);
    }
}
