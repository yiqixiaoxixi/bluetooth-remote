package com.example.bluetoothremote;

/**
 * HID Report Descriptor 定义
 *
 * 本类定义了符合 USB HID 规范的 Report Descriptor 字节数组，
 * 包含两个报告：
 *   - Report ID 1: Keyboard（键盘报告，8 字节）
 *   - Report ID 2: Consumer Control（消费者控制报告，2 字节）
 *
 * 机顶盒作为 BLE HID Device，通过此描述符告知 HID Host（手机）
 * 自己能够发送哪些类型的按键报告。
 */
public class HidReportDescriptor {

    /**
     * 完整的 HID Report Descriptor 字节数组。
     *
     * 包含两个顶层 Collection：
     * 1. Keyboard（Usage Page 0x01, Usage 0x06）
     *    - Report ID 1
     *    - 8 字节：modifier(1) + reserved(1) + keycodes(6)
     *
     * 2. Consumer Control（Usage Page 0x0C, Usage 0x01）
     *    - Report ID 2
     *    - 2 字节：16-bit Consumer Usage
     */
    public static final byte[] DESCRIPTOR = {
            // =====================================================
            // Report 1: Keyboard
            // =====================================================
            (byte) 0x05, (byte) 0x01,       // Usage Page (Generic Desktop)
            (byte) 0x09, (byte) 0x06,       // Usage (Keyboard)
            (byte) 0xA1, (byte) 0x01,       // Collection (Application)
            (byte) 0x85, (byte) 0x01,       //   Report ID (1)

            // Modifier keys (左右 Ctrl/Shift/Alt/GUI)
            (byte) 0x05, (byte) 0x07,       //   Usage Page (Keyboard/Keypad)
            (byte) 0x19, (byte) 0xE0,       //   Usage Minimum (0xE0 = Left Control)
            (byte) 0x29, (byte) 0xE7,       //   Usage Maximum (0xE7 = Right GUI)
            (byte) 0x15, (byte) 0x00,       //   Logical Minimum (0)
            (byte) 0x25, (byte) 0x01,       //   Logical Maximum (1)
            (byte) 0x75, (byte) 0x01,       //   Report Size (1 bit)
            (byte) 0x95, (byte) 0x08,       //   Report Count (8)
            (byte) 0x81, (byte) 0x02,       //   Input (Data, Variable, Absolute) — modifier byte

            // Reserved byte
            (byte) 0x75, (byte) 0x08,       //   Report Size (8 bits)
            (byte) 0x95, (byte) 0x01,       //   Report Count (1)
            (byte) 0x81, (byte) 0x01,       //   Input (Constant) — reserved

            // 6-key rollover keycodes
            (byte) 0x05, (byte) 0x07,       //   Usage Page (Keyboard/Keypad)
            (byte) 0x19, (byte) 0x00,       //   Usage Minimum (0)
            (byte) 0x29, (byte) 0x73,       //   Usage Maximum (0x73)
            (byte) 0x15, (byte) 0x00,       //   Logical Minimum (0)
            (byte) 0x25, (byte) 0x73,       //   Logical Maximum (0x73)
            (byte) 0x75, (byte) 0x08,       //   Report Size (8 bits)
            (byte) 0x95, (byte) 0x06,       //   Report Count (6)
            (byte) 0x81, (byte) 0x00,       //   Input (Data, Array, Absolute) — keycodes

            (byte) 0xC0,                     // End Collection

            // =====================================================
            // Report 2: Consumer Control（多媒体按键）
            // =====================================================
            (byte) 0x05, (byte) 0x0C,       // Usage Page (Consumer Devices)
            (byte) 0x09, (byte) 0x01,       // Usage (Consumer Control)
            (byte) 0xA1, (byte) 0x01,       // Collection (Application)
            (byte) 0x85, (byte) 0x02,       //   Report ID (2)

            (byte) 0x15, (byte) 0x00,       //   Logical Minimum (0)
            (byte) 0x26, (byte) 0xFF, (byte) 0x03, // Logical Maximum (1023)
            (byte) 0x19, (byte) 0x00,       //   Usage Minimum (0)
            (byte) 0x2A, (byte) 0xFF, (byte) 0x03, // Usage Maximum (0x03FF)
            (byte) 0x75, (byte) 0x10,       //   Report Size (16 bits)
            (byte) 0x95, (byte) 0x01,       //   Report Count (1)
            (byte) 0x81, (byte) 0x00,       //   Input (Data, Array, Absolute)

            (byte) 0xC0                      // End Collection
    };
}
