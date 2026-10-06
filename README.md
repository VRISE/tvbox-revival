# M401H 电视盒子解锁（去登录页 / 当贝桌面 / 网络ADB固化）

> 适用机型：中国移动浙江 M401H（国科 GK6323V100C，board=kunlun，Android 9，armeabi-v7a）
> 思路对同期的移动/电信运营商盒子（Sumavision/国科/海思方案）大概率通用，机制细节请对照自家机器。

## 这是什么

一台刷成运营商版本的移动魔百和盒子，开机强制进入登录页，无法正常使用。本项目用**全程不删除、不冻结任何运营商应用**的方式，实现：

| 目标 | 结果 |
| --- | --- |
| 开机直达当贝桌面（跳过运营商登录页） | ✅ 登录引导器被替换成 8.6KB 的"转发壳" |
| 网络 ADB（5555）每次开机自动可用 | ✅ 免 root，应用级实现 |
| 设置 / 遥控热键 / 官方调试入口（10086→其他→左键32下） | ✅ 原样保留，一条未动 |
| 变砖风险 | 接近零：所有改动可回滚，且有 U盘 recovery 卡刷兜底 |

**免 root 是本项目的核心亮点**：这台机器 SELinux 是 Permissive，普通应用可以直接
`SystemProperties.set("ctl.restart","adbd")` 重启 adbd、写 `service.adb.tcp.port`——
根本不需要 su（该机的 su 自带调用方白名单，应用身份会被 "not allowed" 拒绝，见踩坑大全）。

## 仓库结构

```
├── README.md                       ← 本文件
├── docs/
│   ├── 01-设计方案.md               架构与取舍（为什么这么做，为什么不用别的方案）
│   ├── 02-执行实录.md               完整时间线，含三次事故与恢复（最值得读）
│   ├── 03-踩坑大全.md               平台特性与坑，可迁移经验
│   └── 04-人机交互决策记录.md        关键决策与用户约束（如何避免好心办坏事）
├── src/
│   ├── tvfix/                      盒子修复助手（开机自启ADB+桌面兜底，priv-app）
│   └── loaderstub/                 SkyLoader 同包名替身（登录页终结者）
├── scripts/
│   ├── build_apk.sh                无 Gradle 构建脚本（aapt2+javac+d8+apksigner）
│   ├── extract_apk_from_rom.py     从卡刷包 update.zip 提取任意 APK
│   ├── setup_on_box.sh             盒子端 root 配置（build.prop/priv-app/白名单）
│   ├── deploy_loader.sh            替换 SkyLoader（自动备份原版）
│   └── privapp-permissions-*.xml   priv-app 权限白名单
├── dist/                           编译好的 APK（可直接用）
│   ├── TvFix-v4.apk                盒子修复助手 v4
│   └── SkyLoader_patched.apk       SkyLoader 替身
└── reference/memory-notes.md       全程运维记忆笔记（原始工作记录）
```

## 快速开始

前提：盒子已开启网络调试（本机入口：设置 → 密码 `10086` → 其他 → 遥控器左键按约 32 下 → 打开调试模式），且 ROM 自带开放 root（`adb shell` 后 `su 0 id` 能回 uid=0）。

```bash
# 0. 连接
adb connect <盒子IP>:5555

# 1. 安装"盒子修复助手"（该机 adb install 会报 failed to stat，走 push+pm install）
adb push dist/TvFix-v4.apk /data/local/tmp/TvFix.apk
adb shell "pm install -r /data/local/tmp/TvFix.apk"
adb shell "am start -n com.kaixin.tvfix/.MainActivity"   # 必须打开一次！否则收不到开机广播

# 2. 部署 SkyLoader 替身（自动备份原版到 /data/local/tmp/SkyLoaderA9_orig_backup.apk）
adb push dist/SkyLoader_patched.apk /data/local/tmp/SkyLoader_patched.apk
adb push scripts/deploy_loader.sh /data/local/tmp/
adb shell "su 0 sh /data/local/tmp/deploy_loader.sh"

# 3. ADB 永久自启的静态层（可选项，动态层 TvFix 已覆盖）
adb push scripts/setup_on_box.sh /data/local/tmp/
adb shell "su 0 sh /data/local/tmp/setup_on_box.sh"

# 4. 重启验收：开机 → 当贝桌面 + adb connect 即通，全程零人工
adb reboot
```

## 从事零接触回滚

```bash
# 恢复原厂登录引导器
adb shell "su 0 sh -c 'mount -o remount,rw /system; \
  cp /data/local/tmp/SkyLoaderA9_orig_backup.apk /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk; \
  sync'"
adb reboot
```

终极大招：U盘 recovery 卡刷（FAT32 U盘根目录放 update.zip，进 recovery 升级），会清 /data 但入口机制不变。

## 免责声明

仅限**自己拥有的设备**做个性化修复与学习研究。见 [DISCLAIMER.md](DISCLAIMER.md)。刷机/改系统有风险，请先读 [docs/02-执行实录.md](docs/02-执行实录.md) 里的三次事故再动手。

## License

MIT
