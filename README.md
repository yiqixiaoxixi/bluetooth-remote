# 蓝牙遥控器（Bluetooth HID Remote Control）

一个运行在 **Android 11 (API 30)** 机顶盒上的虚拟蓝牙遥控器应用。
机顶盒通过 **BLE HID Device Profile** 模拟成一个标准蓝牙遥控器/键盘，
手机作为 **HID Host** 连接后可接收遥控器发出的按键事件。

---

## 功能

- 📱 机顶盒作为 BLE Peripheral，模拟标准 HID 遥控器
- 🎮 支持方向键（上/下/左/右）+ OK 确认键
- 🏠 Home、Back（返回）、Menu（菜单）功能键
- 🔊 音量加/减、静音、播放/暂停多媒体键
- ⏻ 电源键
- 📡 自动 BLE 广播，等待手机连接
- 🔄 断开后自动重新广播，方便重连
- 🎨 深色遥控器风格 UI，适配 TV 大屏操作

---

## 技术方案

| 技术点 | 说明 |
|--------|------|
| **BLE HID Device Profile** | `android.bluetooth.BluetoothHidDevice` API |
| **HID Report Descriptor** | Report 1: Keyboard (8字节); Report 2: Consumer Control (2字节) |
| **BLE 广播** | `BluetoothLeAdvertiser` 广播 HID 服务 UUID |
| **目标 SDK** | API 30 (Android 11)，最低 API 28 (Android 9) |
| **语言** | Java + AndroidX |

---

## 项目结构

```
app/
├── build.gradle
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/bluetoothremote/
    │   ├── MainActivity.java            # 主 Activity，遥控器 UI
    │   ├── BluetoothHidManager.java     # BLE HID Device 管理器（核心）
    │   ├── HidReportDescriptor.java     # HID Report Descriptor 定义
    │   ├── RemoteKeyCode.java           # 遥控器按键码映射
    │   ├── ConnectionStateCallback.java # 连接状态回调接口
    │   └── PermissionHelper.java        # Android 11 权限处理
    └── res/
        ├── layout/activity_main.xml     # 遥控器 UI 布局
        ├── values/strings.xml
        ├── values/colors.xml
        ├── values/themes.xml
        └── drawable/
            ├── btn_round.xml            # 圆形按钮背景
            ├── btn_rect.xml             # 圆角矩形按钮背景
            └── ic_remote.xml            # 应用图标
```

---

## 编译与安装

### 环境要求

- Android Studio Dolphin (2021.3.1) 或更高版本
- JDK 8 或更高版本
- Android SDK API 30

### 编译步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/yiqixiaoxixi/bluetooth-remote.git
   cd bluetooth-remote
   ```

2. 用 Android Studio 打开项目根目录

3. 等待 Gradle 同步完成

4. 连接机顶盒（开启开发者模式 + USB 调试）

5. 点击 **Run** 安装到机顶盒

   或者命令行：
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 使用说明

### 机顶盒端（运行本应用）

1. 打开应用，点击「**开始广播**」按钮
2. 状态栏显示「**◎ 广播中，等待连接…**」
3. 等待手机连接

### 手机端（HID Host）

1. 打开手机蓝牙设置
2. 搜索并配对名称为 **「BT Remote Control」** 的设备
3. 配对成功后，机顶盒状态栏显示「**✔ 已连接**」

### 使用遥控器

连接成功后，点击机顶盒上的虚拟遥控器按键，手机即可收到对应的 HID 按键事件：

| 按键 | 功能 |
|------|------|
| ▲ ▼ ◄ ► | 方向键导航 |
| OK | 确认/回车 |
| 🏠 Home | 返回主界面 |
| ← Back | 返回上一页 |
| ☰ Menu | 打开菜单 |
| 🔊+ / 🔉- | 调节音量 |
| 🔇 | 静音 |
| ⏯ | 播放/暂停 |
| ⏻ | 电源 |

---

## 权限说明

| 权限 | 说明 |
|------|------|
| `BLUETOOTH` | 基础蓝牙权限（API < 31） |
| `BLUETOOTH_ADMIN` | 蓝牙管理权限（API < 31） |
| `ACCESS_FINE_LOCATION` | BLE 广播/扫描需要（API ≤ 30） |
| `BLUETOOTH_CONNECT` | 精细蓝牙连接权限（API 31+） |
| `BLUETOOTH_ADVERTISE` | 精细蓝牙广播权限（API 31+） |

---

## 已知限制

- 需要机顶盒硬件支持 **BLE Peripheral（外设）模式**，部分低端机顶盒可能不支持
- `BluetoothHidDevice` API 要求 API Level ≥ 28
- 初次使用需要手机端手动配对，配对后可自动重连

---

## License

MIT License
