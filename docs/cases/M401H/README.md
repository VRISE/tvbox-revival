# 案例一：浙江移动 M401H

打通时间：2026-10-06。全程由 AI 执行、人在电视前配合，含三次事故。

## 设备身份

| 项目 | 值 |
| --- | --- |
| 型号 | M401H_CMIOT20_ZJ42（浙江移动魔百和） |
| SoC / board | 国科 GK6323V100C / `kunlun` |
| 系统 | Android 9（API 28），armeabi-v7a |
| 原厂 build | 111.319.106（不含当贝；92 号卡刷包里是 111.319.108 当贝版） |
| root | `/system/xbin/su` 开放型，语法 `su 0 <cmd>`，**不支持 `su -c`**；su 自带调用方白名单，应用身份被拒 |
| SELinux | **Permissive** ← 免 root 路线成立的前提 |
| 运营商 HOME | `com.sumavision.loader`（SkyLoader 登录页），另有 `com.yst.whitebox` 抢前台 |
| 官方调试入口 | 设置 → 密码 `10086` → 其他 → 遥控器左键约 32 下 → 调试模式（开监听 5555） |
| 已知坏味道 | `adb install` 报 failed to stat（走 push + `pm install -r`）；shell 读不了 `/system/build.prop`（用 `su 0 grep`）；`/system/etc/init/` 新增 .rc 不生效；preferred activity 记录被 PMS 秒丢 |

## 成果

- 开机 0.1 秒黑屏 → 直达当贝桌面，登录页物理消失（8.6KB 替身 APK）
- 网络 ADB 每次开机自动可用，**全程无 su**（TvFix v4 反射写属性 + `ctl.restart adbd`）
- 设置、遥控热键、官方调试入口原样保留；零冻结、零删除
- 回滚：拷回 `/data/local/tmp/SkyLoaderA9_orig_backup.apk` 重启；终极兜底 U盘 recovery 卡刷

## 复现命令

仓库根目录执行（`<盒子IP>` 换成你的）：

```bash
adb connect <盒子IP>:5555
adb push dist/TvFix-v4.apk /data/local/tmp/TvFix.apk
adb shell "pm install -r /data/local/tmp/TvFix.apk"
adb shell "am start -n com.kaixin.tvfix/.MainActivity"          # 必须打开一次
adb push dist/SkyLoader_patched.apk scripts/deploy_loader.sh /data/local/tmp/
adb shell "su 0 sh /data/local/tmp/deploy_loader.sh"
adb push scripts/setup_on_box.sh scripts/privapp-permissions-com.kaixin.tvfix.xml /data/local/tmp/
adb shell "su 0 sh /data/local/tmp/setup_on_box.sh"
adb reboot
```

注意：第 4、5 行的 push 文件名要和 `setup_on_box.sh` 里读的名字保持一致
（`TvFix.apk` / `privapp-permissions-com.kaixin.tvfix.xml`）。通用与案例脚本的分工见根 README。

## 阅读顺序

1. `02-执行实录.md` —— 三次事故与恢复，最有参考价值
2. `01-设计方案.md` —— 架构取舍 + 被否掉的方案（Magisk / init.rc / set-home-activity）
3. `03-踩坑大全.md` —— 可迁移到其它机型的平台特性
4. `04-人机交互决策记录.md` —— 人的三条禁令如何塑造了方案形态
5. `99-工作记录.md` —— 原始运维笔记，含当时每条真实命令输出

## 源码

- `../../../src/loaderstub/` —— 登录引导器替身（`com.sumavision.loader` 同包名）
- `../../../src/tvfix/` —— 盒子修复助手（开机自启 ADB + 桌面兜底，priv-app）
- `../../../dist/` —— 两个 APK 成品
