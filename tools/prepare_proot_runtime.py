#!/usr/bin/env python3
"""Prepare APK runtime payloads from the signed Termux repository, without host APT state.

Build tool only. Requires Python 3, gpgv and dpkg-deb; never installs host packages.
The destination must be a new directory so existing files cannot be overwritten.
"""
import argparse
import gzip
import hashlib
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import urllib.request

REPOSITORY = "https://packages.termux.dev/apt/termux-main/"
KEY_COMMIT = "cf263f833188fc7421eded4149fee617c0aad55b"
KEY_NAMES = ("agnostic-apollo", "grimler", "kcubeterm", "landfillbaby", "mradityaalok",
             "2096779623", "thunder-coding", "termux-autobuilds")
PACKAGES = {"proot": "5.1.107.102", "libtalloc": "2.5.0", "libandroid-shmem": "0.7"}
MAX_DOWNLOAD = 32 * 1024 * 1024


def download(url, target, expected=None):
    if not url.startswith("https://"):
        raise ValueError("HTTPS required")
    with urllib.request.urlopen(url, timeout=60) as response:
        if not response.url.startswith("https://"):
            raise ValueError("Insecure redirect")
        data = response.read(MAX_DOWNLOAD + 1)
    if len(data) > MAX_DOWNLOAD:
        raise ValueError("Runtime download exceeds limit")
    if expected and hashlib.sha256(data).hexdigest() != expected:
        raise ValueError("Download checksum mismatch")
    target.write_bytes(data)
    return data


def release_hash(release, filename):
    # SHA256 entries end at the next field; never use an MD5/SHA1 entry.
    for line in release.split("\nSHA256:\n", 1)[1].splitlines():
        if not line.startswith(" "):
            break
        digest, size, name = line.split()
        if name == filename and re.fullmatch("[0-9a-f]{64}", digest):
            return digest
    raise ValueError("Index absent from signed SHA256 section")


def package_records(index):
    records = {}
    for paragraph in index.split("\n\n"):
        fields = dict(line.split(": ", 1) for line in paragraph.splitlines()
                      if ": " in line and not line.startswith(" "))
        name = fields.get("Package")
        if name not in PACKAGES:
            continue
        if fields.get("Version") != PACKAGES[name] or fields.get("Architecture") != "aarch64":
            raise ValueError(f"Pinned ARM64 version unavailable: {name}={PACKAGES[name]}")
        path = PurePosixPath(fields["Filename"])
        if path.is_absolute() or ".." in path.parts or not str(path).startswith("pool/"):
            raise ValueError("Unsafe package filename")
        if not re.fullmatch("[0-9a-f]{64}", fields.get("SHA256", "")):
            raise ValueError("Missing package SHA256")
        records[name] = fields
    if records.keys() != PACKAGES.keys():
        raise ValueError("Pinned packages missing from signed index")
    return records


def prepare(destination):
    destination.mkdir(parents=True, exist_ok=False)
    evidence = destination / "repository"
    evidence.mkdir()
    keys = []
    for name in KEY_NAMES:
        target = evidence / (name + ".gpg")
        download(f"https://raw.githubusercontent.com/termux/termux-packages/{KEY_COMMIT}"
                 f"/packages/termux-keyring/{name}.gpg", target)
        keys += ["--keyring", str(target.resolve())]
    download(REPOSITORY + "dists/stable/InRelease", evidence / "InRelease")
    subprocess.run(["gpgv", *keys, str(evidence / "InRelease")], check=True)
    # Parse the cleartext only after gpgv authenticated this exact file.
    signed = (evidence / "InRelease").read_text()
    release = signed.split("\n\n", 1)[1].split("\n-----BEGIN PGP SIGNATURE-----", 1)[0]
    release = "\n".join(line[2:] if line.startswith("- ") else line for line in release.splitlines())
    (evidence / "Release").write_text(release)
    index_path = "main/binary-aarch64/Packages.gz"
    digest = release_hash((evidence / "Release").read_text(), index_path)
    compressed = download(REPOSITORY + "dists/stable/" + index_path,
                          evidence / "Packages.gz", digest)
    records = package_records(gzip.decompress(compressed).decode())
    native = destination / "jniLibs/arm64-v8a"
    assets = destination / "assets/proot"
    native.mkdir(parents=True)
    (assets / "licenses").mkdir(parents=True)
    manifest = []
    files = {"proot": (("bin/proot", "libproot.so", "bin/proot"),
                       ("libexec/proot/loader", "libproot-loader.so", "loader")),
             "libtalloc": (("lib/libtalloc.so", "libtalloc.so", "lib/libtalloc.so"),),
             "libandroid-shmem": (("lib/libandroid-shmem.so", "libandroid-shmem.so", "lib/libandroid-shmem.so"),)}
    for name, record in records.items():
        archive = evidence / (name + ".deb")
        download(REPOSITORY + record["Filename"], archive, record["SHA256"])
        extracted = destination / ("extracted-" + name)
        subprocess.run(["dpkg-deb", "-x", str(archive), str(extracted)], check=True)
        prefix = extracted / "data/data/com.termux/files/usr"
        manifest.append(f"# {name}={PACKAGES[name]}")
        for source_name, library, runtime_name in files[name]:
            source = (prefix / source_name).resolve()
            if not source.is_relative_to(extracted.resolve()):
                raise ValueError("Package symlink escapes extraction directory")
            target = native / library
            shutil.copyfile(source, target)
            target.chmod(0o755 if name == "proot" else 0o644)
            manifest.append(f"{hashlib.sha256(target.read_bytes()).hexdigest()} {runtime_name}")
        copyright_file = prefix / "share/doc" / name / "copyright"
        if copyright_file.is_file():
            shutil.copyfile(copyright_file, assets / "licenses" / (name + "-copyright"))
    (assets / "runtime-manifest.txt").write_text("\n".join(manifest) + "\n")
    print(f"Verified runtime prepared in {destination}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path)
    prepare(parser.parse_args().destination)
