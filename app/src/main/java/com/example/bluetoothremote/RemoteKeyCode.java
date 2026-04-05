package com.example.bluetoothremote;

/**
 * 遥控器按键码映射常量类
 *
 * 将虚拟遥控器 UI 上的按键映射到 HID Usage Page 规范定义的键码：
 *   - Keyboard Usage Page (0x07)：方向键、Enter、Escape 等标准键盘键
 *   - Consumer Usage Page (0x0C)：音量、播放控制、电源等多媒体键
 *
 * 键码参考：
 *   HID Usage Tables 1.4 (https://usb.org/sites/default/files/hut1_4.pdf)
 */
public class RemoteKeyCode {

    // ==========================================================
    // Report ID 常量
    // ==========================================================

    /** 键盘报告 ID（Report 1） */
    public static final byte REPORT_ID_KEYBOARD = 0x01;

    /** 消费者控制报告 ID（Report 2） */
    public static final byte REPORT_ID_CONSUMER = 0x02;

    // ==========================================================
    // Keyboard Usage Page (0x07) — 键盘键码
    // 使用场景：Report ID 1 的 keycode 字段
    // ==========================================================

    /** 无按键（Key None / 释放） */
    public static final byte KEY_NONE = 0x00;

    /** Enter / 回车键 */
    public static final byte KEY_ENTER = 0x28;

    /** Escape 键 */
    public static final byte KEY_ESCAPE = 0x29;

    /** 上方向键 */
    public static final byte KEY_UP = 0x52;

    /** 下方向键 */
    public static final byte KEY_DOWN = 0x51;

    /** 左方向键 */
    public static final byte KEY_LEFT = 0x50;

    /** 右方向键 */
    public static final byte KEY_RIGHT = 0x4F;

    /** Home 键（Keyboard Home） */
    public static final byte KEY_HOME = 0x4A;

    /** End 键 */
    public static final byte KEY_END = 0x4D;

    /** Page Up */
    public static final byte KEY_PAGE_UP = 0x4B;

    /** Page Down */
    public static final byte KEY_PAGE_DOWN = 0x4E;

    /** F1 ~ F12（备用） */
    public static final byte KEY_F1 = 0x3A;
    public static final byte KEY_F2 = 0x3B;
    public static final byte KEY_F3 = 0x3C;
    public static final byte KEY_F4 = 0x3D;

    // ==========================================================
    // Consumer Usage Page (0x0C) — 消费者控制键码（16-bit）
    // 使用场景：Report ID 2 的 consumer usage 字段
    // ==========================================================

    /** 无消费者按键（释放） */
    public static final int CONSUMER_NONE = 0x0000;

    /** 音量加 */
    public static final int CONSUMER_VOLUME_UP = 0x00E9;

    /** 音量减 */
    public static final int CONSUMER_VOLUME_DOWN = 0x00EA;

    /** 静音 */
    public static final int CONSUMER_MUTE = 0x00E2;

    /** 播放 / 暂停 */
    public static final int CONSUMER_PLAY_PAUSE = 0x00CD;

    /** 下一首 */
    public static final int CONSUMER_NEXT_TRACK = 0x00B5;

    /** 上一首 */
    public static final int CONSUMER_PREV_TRACK = 0x00B6;

    /** 停止 */
    public static final int CONSUMER_STOP = 0x00B7;

    /** 电源（Consumer Power） */
    public static final int CONSUMER_POWER = 0x0030;

    /** Home（Consumer AC Home，部分系统识别为 Android Home） */
    public static final int CONSUMER_AC_HOME = 0x0223;

    /** 返回（Consumer AC Back） */
    public static final int CONSUMER_AC_BACK = 0x0224;

    /** 前进（Consumer AC Forward） */
    public static final int CONSUMER_AC_FORWARD = 0x0225;

    /** 搜索（Consumer AC Search） */
    public static final int CONSUMER_AC_SEARCH = 0x0221;

    /** 菜单（Consumer Menu） */
    public static final int CONSUMER_MENU = 0x0040;

    // 私有构造函数，禁止实例化此工具类
    private RemoteKeyCode() {}
}
