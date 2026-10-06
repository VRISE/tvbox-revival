# 运营商盒子复活计划 · tvbox-revival

> 把三大运营商淘汰的安卓机顶盒，变回一台**正常好用的安卓盒子**：
> 跳过强制登录/认证页、装用户自选桌面、固化网络 ADB —— **不删运营商应用、不刷 boot、不装 Magisk、全程可回滚**。
>
> 这是一套**给人看的教程**，也是一套**给 AI 执行的手册**：约束、检查清单、脚本、构建链都在仓库里。

## 为什么做这件事

中国移动/电信/联通的定制盒子（魔百和、悦盒、天翼高清等）在 IPTV 业务退订后
就变成砖：开机强制进运营商登录页，没有账号就什么都干不了。
而这些机器的硬件（四核 SoC、2GB RAM、HDMI、遥控、千兆网口、电源）本可以再服役十年。
丢掉是纯粹的硬件浪费，修好它只需要一个晚上。

## 它解决什么

| 痛点 | 本项目的解法 |
| --- | --- |
| 开机强制进运营商登录页，没有业务账号 = 无法使用 | **替身启动器**：自建同包名迷你 APK 原位替换运营商的 HOME 引导器，登录代码物理消失（案例里 8.6KB） |
| 没有桌面 / 装不了自己的 App | 用户自选桌面（当贝等）+ `intent-filter priority` 稳定夺回 HOME 键 |
| 网络 ADB 重启就掉，每次要从遥控器手动开调试 | **免 root 固化**：应用开机广播里直写系统属性 + `ctl.restart adbd`（SELinux Permissive 的机器均可用） |
| 怕搞成砖 | 零删除原则 + 每一步双备份 + U盘 recovery 兜底 + 保留原厂自救入口 |

## 5 分钟看懂原理

```
侦察（机型/SELinux/root/HOME 候选/调试入口）
   ↓
拿到 ROM 里的 APK（卡刷包 → brotli → ext4 → 文件），不必联网下载
   ↓
目标A 登录页消失 = 同包名替身 APK 原位替换 + HOME priority
目标B ADB 永久在线 = priv-app 开机广播写属性 + ctl.restart adbd（免 root）
   ↓
重启前过一遍检查清单 → 重启验收
```

两条提权路径，取决于机器给不给：
- ROM 自带**开放 root** → 直接改 `/system`（多数运营商盒子有）
- **SELinux = Permissive** → 普通应用就能 `SystemProperties.set("ctl.restart","adbd")`，
  完全不需要 su。这是本项目最核心、最可迁移的一条经验。

## 仓库结构

```
├── README.md
├── AGENTS.md                        ← 把这个文件交给你的 AI（约束、红线、检查清单）
├── DISCLAIMER.md  LICENSE
├── docs/
│   ├── 00-通用方法论.md              ← 机型无关的六步流程 + 决策树（先读这个）
│   ├── 01-AI执行手册.md              ← 可直接复制投喂的 prompt 与回合脚本
│   └── cases/M401H/                 ← 案例一：浙江移动 M401H（国科 GK6323 / 安卓 9）
│       ├── README.md                设备身份、调试入口、一键命令
│       ├── 01-设计方案.md            架构与所有被否掉的替代方案
│       ├── 02-执行实录.md            完整时间线，含三次事故（最值得读）
│       ├── 03-踩坑大全.md            可迁移的平台特性
│       ├── 04-人机交互决策记录.md     人的三条禁令如何塑造了方案
│       └── 99-工作记录.md            原始运维笔记
├── src/
│   ├── tvfix/                       盒子修复助手（开机自启 ADB + 桌面兜底，priv-app）
│   └── loaderstub/                  运营商登录引导器的同包名替身
├── scripts/
│   ├── build_apk.sh                 通用：无 Gradle 构建 APK（aapt2+javac+d8+apksigner）
│   ├── extract_apk_from_rom.py      通用：从卡刷包 update.zip 提取任意文件
│   ├── setup_on_box.sh              案例：build.prop / priv-app / 权限白名单
│   ├── deploy_loader.sh             案例：替换登录引导器（自动备份原版）
│   └── privapp-permissions-*.xml    案例：priv-app 权限白名单
└── dist/                            已编译 APK，可直接用
```

标注「通用」的脚本换机型即可复用；标注「案例」的脚本路径写死了 M401H，
迁移时按 `docs/cases/M401H/README.md` 的说明改三处：包名、分区路径、调试入口。

## 快速开始（以 M401H 为例）

前提：盒子已开网络调试（该案例入口：设置 → 密码 `10086` → 其他 → 遥控器左键约 32 下），
并且 `adb shell` 后 `su 0 id` 能回 `uid=0`。

```bash
adb connect <盒子IP>:5555

# 1. 装"盒子修复助手"（该机 adb install 报 failed to stat，走 push+pm install）
adb push dist/TvFix-v4.apk /data/local/tmp/TvFix.apk
adb shell "pm install -r /data/local/tmp/TvFix.apk"
adb shell "am start -n com.kaixin.tvfix/.MainActivity"   # 必须打开一次，否则收不到开机广播

# 2. 部署登录引导器替身（自动备份原版）
adb push dist/SkyLoader_patched.apk /data/local/tmp/SkyLoader_patched.apk
adb push scripts/deploy_loader.sh /data/local/tmp/
adb shell "su 0 sh /data/local/tmp/deploy_loader.sh"

# 3.（可选）ADB 永久自启的静态层
adb push scripts/setup_on_box.sh /data/local/tmp/
adb shell "su 0 sh /data/local/tmp/setup_on_box.sh"

# 4. 重启验收：开机直达自选桌面 + adb connect 即通，全程零人工
adb reboot
```

回滚（零接触）：

```bash
adb shell "su 0 sh -c 'mount -o remount,rw /system; \
  cp /data/local/tmp/SkyLoaderA9_orig_backup.apk /system/app/SkyLoaderA9_release/SkyLoaderA9_release.apk; \
  sync'" && adb reboot
```

## 三条铁律（AGENTS.md 里有完整版）

1. **不删除、不冻结任何运营商应用** —— 只用替身、优先级、共存来解决问题。
   它们里面有且仅有一个是官方调试入口，动错一次 = 失联一晚上。
2. **重启前把准备工作做完并过检查清单** —— 网络 ADB 在多数此类机器上重启即掉。
3. **永远保留一条不依赖电脑/AI 的自救通道** —— 电视端 UI 上必须能自己把调试打开。

## 贡献新机型

欢迎按案例格式提交你打通的机型：`docs/cases/<你的型号>/` + 侦察输出 + 事故记录。
`docs/00-通用方法论.md` 的第六节给出了案例模板需要回答的 12 个问题。

## 免责声明

仅用于**你自己拥有**的设备做修复与学习；不含、也不分发任何运营商固件本体；
`dist/` 内两个 APK 均为原创代码。详见 [DISCLAIMER.md](DISCLAIMER.md)。刷机有风险，变砖自负。

## License

MIT
