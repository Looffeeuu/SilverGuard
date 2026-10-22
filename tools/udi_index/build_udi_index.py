"""Build SilverGuard's offline medical device registration index from NMPA UDI database packages.

Input: the official packages from https://udi.nmpa.gov.cn/download.html, a full release first and then
any incremental packages in date order (.zip as downloaded, or the .xml inside).
Output: a directory with manifest.json and 64 gzip shards. The app looks a registration number up by
normalising it, picking shard crc32(key) % 64, and scanning that one small shard.

Only the registration number, registrant, product names and device category are kept.
Contact names, phone numbers and e-mail addresses in the packages are never read.

Usage:
    python build_udi_index.py --out udi-index UDID_FULL_RELEASE_20261001.zip UDID_DAY_UPDATE_20261002.zip ...
"""

import argparse
import datetime
import gzip
import json
import os
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
import zipfile
import zlib

FORMAT_VERSION = 1
SHARD_COUNT = 64
MAX_NAMES = 3
NUMBER_SEPARATORS = re.compile(r"[,，、;；/\s]+")
REMOVED_CHARS = re.compile(r"[\s:：()（）\-—]")
REMOVED_STATUSES = ("删除", "注销")


def normalize_key(number):
    """Must match DeviceRegistryKey.normalize in the app."""
    text = unicodedata.normalize("NFKC", number).upper()
    text = REMOVED_CHARS.sub("", text)
    return text[:-1] if text.endswith("号") else text


def shard_of(key):
    """Must match DeviceRegistryKey.shardOf in the app."""
    return zlib.crc32(key.encode("utf-8")) % SHARD_COUNT


def split_numbers(field):
    return [part for part in NUMBER_SEPARATORS.split(field or "") if part.strip()]


def clean(value):
    return " ".join((value or "").split()).replace("\t", " ")


def open_xml_streams(path):
    if path.lower().endswith(".zip"):
        with zipfile.ZipFile(path) as archive:
            for name in sorted(archive.namelist()):
                if name.lower().endswith(".xml"):
                    with archive.open(name) as stream:
                        yield name, stream
    else:
        with open(path, "rb") as stream:
            yield os.path.basename(path), stream


def read_devices(stream):
    """Yields (creation_date, record) for each <device>, keeping only the fields the index needs."""
    creation_date = ""
    for event, element in ET.iterparse(stream, events=("end",)):
        if element.tag == "creationDate":
            creation_date = (element.text or "").strip()
        elif element.tag == "device":
            yield creation_date, {
                "numbers": element.findtext("zczbhhzbapzbh") or "",
                "holder": clean(element.findtext("ylqxzcrbarmc")),
                "name": clean(element.findtext("cpmctymc")),
                "category": clean(element.findtext("qxlb")),
                "status": clean(element.findtext("versionStauts")),
            }
            element.clear()


def build(inputs):
    entries = {}
    latest_date = ""
    device_count = 0
    for path in inputs:
        for _, stream in open_xml_streams(path):
            for creation_date, device in read_devices(stream):
                device_count += 1
                latest_date = max(latest_date, creation_date[:10])
                removed = any(word in device["status"] for word in REMOVED_STATUSES)
                for number in split_numbers(device["numbers"]):
                    key = normalize_key(number)
                    if not key:
                        continue
                    if removed:
                        entries.pop(key, None)
                        continue
                    entry = entries.setdefault(key, {"number": clean(number), "holder": "", "names": [], "category": ""})
                    # Later packages are newer, so their registrant and category win.
                    if device["holder"]:
                        entry["holder"] = device["holder"]
                    if device["category"]:
                        entry["category"] = device["category"]
                    name = device["name"]
                    if name and name not in entry["names"] and len(entry["names"]) < MAX_NAMES:
                        entry["names"].append(name)
    return entries, latest_date, device_count


def write_index(entries, out_dir, inputs, data_date, device_count, built_at=None):
    os.makedirs(out_dir, exist_ok=True)
    shards = [[] for _ in range(SHARD_COUNT)]
    for key in sorted(entries):
        entry = entries[key]
        row = "\t".join([key, entry["number"], entry["holder"], "；".join(entry["names"]), entry["category"]])
        shards[shard_of(key)].append(row)
    for index, rows in enumerate(shards):
        data = ("\n".join(rows) + ("\n" if rows else "")).encode("utf-8")
        # mtime=0 keeps the output byte-identical for identical input.
        with gzip.GzipFile(os.path.join(out_dir, "shard-%02d.tsv.gz" % index), "wb", compresslevel=9, mtime=0) as out:
            out.write(data)
    manifest = {
        "format": FORMAT_VERSION,
        "shards": SHARD_COUNT,
        "dataDate": data_date,
        "records": len(entries),
        "devices": device_count,
        "sources": [os.path.basename(path) for path in inputs],
        "builtAt": built_at or datetime.date.today().isoformat(),
    }
    with open(os.path.join(out_dir, "manifest.json"), "w", encoding="utf-8", newline="\n") as out:
        json.dump(manifest, out, ensure_ascii=False, indent=2)
        out.write("\n")
    return manifest


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--out", required=True, help="output directory")
    parser.add_argument("--built-at", help="override builtAt, for reproducible fixtures")
    parser.add_argument("inputs", nargs="+", help="full package first, then incremental packages in date order")
    args = parser.parse_args(argv)
    entries, data_date, device_count = build(args.inputs)
    write_index(entries, args.out, args.inputs, data_date, device_count, args.built_at)
    total = sum(os.path.getsize(os.path.join(args.out, name)) for name in os.listdir(args.out))
    print("%d devices -> %d registration numbers, %d bytes in %s (data date %s)"
          % (device_count, len(entries), total, args.out, data_date), file=sys.stderr)


if __name__ == "__main__":
    main()
