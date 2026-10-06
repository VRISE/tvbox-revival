#!/usr/bin/env python3
"""给运营商盒子的 setuid su 打补丁: 移除调用方白名单, 让任意 uid 通过检查。

⚠️ 注意: 即使补丁成功, Android 8+ 的 zygote seccomp 过滤器仍然禁止【应用进程】
   调用 setuid/setgid 系统调用 —— 所以补丁只让 shell/adb 上下文之外的非白名单
   原生进程受益(如 init 启动的 root 服务)。应用(App)永远拿不到 root, 这是
   系统级硬限制, 与 su 本身无关。

用法: python3 patch_su_whitelist.py su_orig.bin su_patched.bin
原理: 定位 "cmp r0, #2000; beq allowed" 之后的拒绝块(12字节), 整段 NOP,
      使所有 uid 落入允许路径。已知适用: M401H (GK6323, Android 9) 的 su。
"""
import sys

DENY_OFFSET = 0xB64   # 拒绝块偏移 (以本机型 16072 字节 su 为准, 换机型先反汇编确认!)
DENY_LEN = 12
NOP = b"\x00\xbf"


def main():
    src, dst = sys.argv[1], sys.argv[2]
    data = bytearray(open(src, "rb").read())
    orig = bytes(data[DENY_OFFSET:DENY_OFFSET + DENY_LEN])
    print("original deny block:", orig.hex())
    data[DENY_OFFSET:DENY_OFFSET + DENY_LEN] = NOP * (DENY_LEN // 2)
    open(dst, "wb").write(bytes(data))
    print("patched:", bytes(data[DENY_OFFSET:DENY_OFFSET + DENY_LEN]).hex())

    # 校验: 前面的白名单指令必须原样存在 (blx getuid; cbz; cmp #0x7d0; beq)
    import capstone
    md = capstone.Cs(capstone.CS_ARCH_ARM, capstone.CS_MODE_THUMB)
    seq = [(0xB58, "blx"), (0xB5C, "cbz"), (0xB5E, "cmp.w"), (0xB62, "beq")]
    for addr, mn in seq:
        ins = next(md.disasm(bytes(data[addr:addr + 4]), addr))
        assert ins.mnemonic == mn, f"unexpected at {hex(addr)}: {ins.mnemonic}"
    print("whitelist instructions intact; deny path NOPed; OK")


if __name__ == "__main__":
    main()
