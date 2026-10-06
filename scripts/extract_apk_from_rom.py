#!/usr/bin/env python3
"""从运营商盒子卡刷包 update.zip 提取任意文件(如桌面APK)。

链路: update.zip → system.new.dat.br --brotli--> system.new.dat
      --(transfer.list 拼块)--> system.img(ext4) --(python ext4 库)--> 文件

依赖: pip install brotli ext4
用法:
  python3 extract_apk_from_rom.py update.zip /system/app/launcher/Launcher.apk out.apk
  python3 extract_apk_from_rom.py update.zip --list /system/app      # 列目录
"""
import io
import os
import sys
import zipfile

import brotli
import ext4

BLOCK = 4096


def sdat2img(transfer_list: str, new_dat: bytes) -> bytes:
    """极简 sdat2img: transfer.list + new.dat -> raw ext4 bytes."""
    with open(transfer_list) as f:
        version = int(f.readline())
        f.readline()  # new blocks
        if version >= 2:
            f.readline()  # stash
        f.readline()  # command count (不信任它,读到 EOF)

    cmds = []
    for line in open(transfer_list).readlines()[4 if version >= 2 else 3:]:
        line = line.strip()
        if not line:
            continue
        parts = line.split(" ", 1)
        ranges = []
        if len(parts) > 1:
            nums = parts[1].split(",")
            ranges = [(int(nums[i]), int(nums[i + 1])) for i in range(0, len(nums), 2)]
        cmds.append((parts[0], ranges))

    max_block = max((e for _, rs in cmds for _, e in rs), default=0)
    out = bytearray(max_block * BLOCK)
    pos = 0  # 顺序消费 new.dat 的游标
    for cmd, ranges in cmds:
        if cmd == "new":
            for s, e in ranges:
                n = (e - s) * BLOCK
                out[s * BLOCK:s * BLOCK + n] = new_dat[pos:pos + n]
                pos += n
        # zero/erase: 保持全零即可
    return bytes(out)


def clean_name(b: bytes) -> str:
    """ext4 dirent 名字常带垃圾填充字节,只保留可打印 ASCII。"""
    return "".join(chr(c) for c in b if 0x20 <= c <= 0x7E)


def main():
    zipp, path, *rest = sys.argv[1:]
    if not zipp:
        sys.exit(__doc__)
    zf = zipfile.ZipFile(zipp)
    workdir = "/tmp/rom_extract"
    os.makedirs(workdir, exist_ok=True)
    br = zf.extract("system.new.dat.br", workdir)
    tl = zf.extract("system.transfer.list", workdir)
    print("[1/3] brotli decompress ...")
    dat = brotli.decompress(open(br, "rb").read())
    print("[2/3] sdat2img ...")
    img = os.path.join(workdir, "system.img")
    open(img, "wb").write(sdat2img(tl, dat))
    vol = ext4.Volume(open(img, "rb"))

    def lookup(p):
        d = vol.inode_at(os.path.dirname(p))
        name = clean_name(os.path.basename(p).encode())
        return d, name

    print("[3/3] extracting", path)
    if path.endswith("/"):
        d = vol.inode_at(path[:-1])
        for e, ft in d.opendir():
            print(" ", clean_name(e.name), ft)
        return
    d, name = lookup(path)
    inode = d.inode_at(name)
    data = inode.open().read()
    out = rest[0] if rest else os.path.basename(path)
    open(out, "wb").write(data)
    print("saved", out, len(data))


if __name__ == "__main__":
    main()
