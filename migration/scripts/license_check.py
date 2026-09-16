#!/usr/bin/env python3
"""M1.5 许可证一致性校验（python3 标准库）。

校验项：
  1. 仓库根 LICENSE 与 MPL 2.0 官方全文一致（规范化空白后比对 SHA-256）；
  2. mod_info/META-INF/mods.toml 的 jar 级 license 字段为 MPL-2.0（该字段
     位于所有 [[mods]] 条目之前，对 jar 内全部 mod 条目生效）；
  3. gradle.properties 的 mod_license 元数据为 MPL-2.0；
  4. LICENSE-NEW 不存在（已归一到 LICENSE，避免双文本漂移）；
  5. 跟踪文件中无 "MMPL" 字样残留。豁免（按 M1.5 裁决保留原样）：
     - *.java 源文件（历史文件头保留旧许可证，不做批量修改）；
     - 三个 git 子模块（BuildCraftAPI / BuildCraft-Localization / BuildCraftGuide）；
     - build 输出目录、buildcraft_resources_generated（数据生成产物）；
     - migration/、AGENTS.md（迁移历史记录文档，保留旧名属正常引用）。

通过：逐项输出 OK，exit 0；失败：输出具体差异，exit 1。
"""

import hashlib
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent

# MPL 2.0 官方全文（即原 LICENSE-NEW 内容）规范化空白后的 SHA-256。
# 规范化方式见 normalize_text()：统一换行、去行尾空白、去首尾空行、单一结尾换行。
EXPECTED_MPL2_SHA256 = "1f256ecad192880510e84ad60474eab7589218784b9a50bc7ceee34c2b91f1d5"

MODS_TOML = REPO_ROOT / "mod_info" / "META-INF" / "mods.toml"
# M2.9 删除 legacy 根构建后，license 元数据的权威位置是 neo/gradle.properties（mod_license=MPL-2.0）
GRADLE_PROPERTIES = REPO_ROOT / "neo" / "gradle.properties"
LICENSE_NEW = REPO_ROOT / "LICENSE-NEW"

# MMPL 残留扫描的目录豁免（git 子模块、构建产物、数据生成产物、迁移历史记录）
MMPL_EXCLUDED_DIRS = {
    ".git",
    "BuildCraftAPI",
    "BuildCraft-Localization",
    "BuildCraftGuide",
    "build",
    "buildcraft_resources_generated",
    "migration",
}
MMPL_EXCLUDED_FILES = {REPO_ROOT / "AGENTS.md", Path(__file__).resolve()}


def normalize_text(raw: str) -> str:
    lines = [l.rstrip() for l in raw.replace("\r\n", "\n").replace("\r", "\n").split("\n")]
    while lines and not lines[0]:
        lines.pop(0)
    while lines and not lines[-1]:
        lines.pop()
    return "\n".join(lines) + "\n"


def check_license_text():
    lic = REPO_ROOT / "LICENSE"
    if not lic.is_file():
        return False, "LICENSE 文件不存在"
    digest = hashlib.sha256(normalize_text(lic.read_text(encoding="utf-8")).encode("utf-8")).hexdigest()
    if digest != EXPECTED_MPL2_SHA256:
        return False, f"LICENSE 内容与 MPL 2.0 不一致：期望 SHA-256 {EXPECTED_MPL2_SHA256}，实际 {digest}"
    return True, "LICENSE 与 MPL 2.0 全文一致（规范化 SHA-256 匹配）"


def check_mods_toml():
    if not MODS_TOML.is_file():
        return False, f"{MODS_TOML} 不存在"
    lines = MODS_TOML.read_text(encoding="utf-8").splitlines()
    first_mods = next((i for i, l in enumerate(lines) if l.strip() == "[[mods]]"), None)
    if first_mods is None:
        return False, "mods.toml 中没有任何 [[mods]] 条目"
    license_lines = [(i, l.strip()) for i, l in enumerate(lines) if l.strip().startswith("license=")]
    if not license_lines:
        return False, "mods.toml 中没有 license 字段"
    bad = [(i, l) for i, l in license_lines if l != 'license="MPL-2.0"']
    if bad:
        return False, "license 字段非 MPL-2.0：" + "; ".join(f"第{i + 1}行 {l}" for i, l in bad)
    if license_lines[0][0] > first_mods:
        return False, "license 字段出现在 [[mods]] 之后，未对全部 mod 条目生效"
    mod_count = sum(1 for l in lines if l.strip() == "[[mods]]")
    return True, f"jar 级 license=\"MPL-2.0\" 对全部 {mod_count} 个 [[mods]] 条目生效"


def check_gradle_properties():
    if not GRADLE_PROPERTIES.is_file():
        return False, f"{GRADLE_PROPERTIES} 不存在"
    for line in GRADLE_PROPERTIES.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if s.startswith("mod_license="):
            value = s.split("=", 1)[1].strip()
            if value == "MPL-2.0":
                return True, "gradle.properties mod_license=MPL-2.0"
            return False, f"gradle.properties mod_license={value}，期望 MPL-2.0"
    return False, "gradle.properties 中没有 mod_license 字段"


def check_license_new_absent():
    if LICENSE_NEW.exists():
        return False, "LICENSE-NEW 仍存在，应已删除归一到 LICENSE"
    return True, "LICENSE-NEW 已移除"


def check_no_mmpl_residue():
    try:
        out = subprocess.run(
            ["git", "ls-files", "-z"], cwd=REPO_ROOT, capture_output=True, check=True
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        return False, f"无法枚举 git 跟踪文件：{e}"
    residue = []
    java_exempt = 0
    for chunk in out.split(b"\0"):
        if not chunk:
            continue
        rel = Path(chunk.decode("utf-8"))
        path = REPO_ROOT / rel
        if path in MMPL_EXCLUDED_FILES:
            continue
        if any(part in MMPL_EXCLUDED_DIRS for part in rel.parts):
            continue
        if rel.suffix == ".java":
            try:
                if b"MMPL" in path.read_bytes():
                    java_exempt += 1
            except OSError:
                pass
            continue
        try:
            if b"MMPL" in path.read_bytes():
                residue.append(str(rel))
        except OSError:
            pass
    if residue:
        return False, "以下非豁免文件仍含 MMPL 字样：\n  " + "\n  ".join(residue)
    return True, f"非豁免跟踪文件无 MMPL 残留（{java_exempt} 个 .java 源码头按裁决豁免，保留历史头）"


def main():
    checks = [
        ("LICENSE=MPL 2.0 全文", check_license_text),
        ("mods.toml license 元数据", check_mods_toml),
        ("gradle.properties mod_license", check_gradle_properties),
        ("LICENSE-NEW 已移除", check_license_new_absent),
        ("无 MMPL 残留", check_no_mmpl_residue),
    ]
    failed = False
    for name, fn in checks:
        try:
            ok, detail = fn()
        except Exception as e:  # noqa: BLE001
            ok, detail = False, f"校验异常：{e!r}"
        print(f"[{'OK' if ok else 'FAIL'}] {name}: {detail}")
        failed |= not ok
    if failed:
        print("license_check: FAILED")
        return 1
    print("license_check: all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
