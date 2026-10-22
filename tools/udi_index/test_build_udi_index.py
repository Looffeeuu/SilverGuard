"""Run with: python -m unittest discover -s tools/udi_index"""

import gzip
import json
import os
import tempfile
import unittest
import zipfile

import build_udi_index as udi

HERE = os.path.dirname(os.path.abspath(__file__))
FULL = os.path.join(HERE, "testdata", "synthetic_full.xml")
UPDATE = os.path.join(HERE, "testdata", "synthetic_update.xml")


def read_index(out_dir):
    rows = {}
    for name in os.listdir(out_dir):
        if name.endswith(".tsv.gz"):
            with gzip.open(os.path.join(out_dir, name), "rt", encoding="utf-8") as shard:
                for line in shard:
                    key, number, holder, names, category = line.rstrip("\n").split("\t")
                    rows[key] = (name, number, holder, names, category)
    return rows


class BuildIndexTest(unittest.TestCase):

    def build(self, *inputs):
        out_dir = tempfile.mkdtemp()
        udi.main(["--out", out_dir, "--built-at", "2026-10-22", *inputs])
        return out_dir

    def test_normalize_key_matches_app_rules(self):
        self.assertEqual("京大械备20260001", udi.normalize_key("京大械备20260001号"))
        self.assertEqual("国械注准20263090001", udi.normalize_key(" 国械注准 2026-3090001 "))
        self.assertEqual("国械注准20263090001", udi.normalize_key("国械注准２０２６３０９０００１"))

    def test_shard_is_crc32_mod_64(self):
        # The app's DeviceRegistryKeyTest checks the same numbers.
        self.assertEqual(udi.shard_of("国械注准20263090001"), udi.shard_of(udi.normalize_key("国械注准20263090001号")))
        self.assertEqual(31, udi.shard_of("国械注准20263090001"))
        self.assertEqual(45, udi.shard_of("京大械备20260001"))

    def test_full_then_update(self):
        rows = read_index(self.build(FULL, UPDATE))
        self.assertEqual({"国械注准20263090001", "苏械注准20262090002", "京大械备20260001", "国械注进20263460004"}, set(rows))
        _, number, holder, names, category = rows["国械注准20263090001"]
        self.assertEqual(("国械注准20263090001", "安心医疗科技有限公司", "电子血压计", "器械"), (number, holder, names, category))
        self.assertEqual("苏州康复器械股份有限公司", rows["苏械注准20262090002"][2])
        self.assertNotIn("粤械注准20262400003", rows)

    def test_rows_are_in_the_shard_the_app_will_open(self):
        out_dir = self.build(FULL, UPDATE)
        for key, (shard_name, *_rest) in read_index(out_dir).items():
            self.assertEqual("shard-%02d.tsv.gz" % udi.shard_of(key), shard_name)

    def test_contact_details_never_reach_the_index(self):
        out_dir = self.build(FULL)
        for name in os.listdir(out_dir):
            path = os.path.join(out_dir, name)
            data = gzip.open(path).read() if name.endswith(".gz") else open(path, "rb").read()
            self.assertNotIn(b"synthetic-contact", data)
            self.assertNotIn(b"00000000000", data)

    def test_manifest_and_reproducible_output(self):
        first, second = self.build(FULL, UPDATE), self.build(FULL, UPDATE)
        with open(os.path.join(first, "manifest.json"), encoding="utf-8") as manifest_file:
            manifest = json.load(manifest_file)
        self.assertEqual(1, manifest["format"])
        self.assertEqual(64, manifest["shards"])
        self.assertEqual("2026-10-05", manifest["dataDate"])
        self.assertEqual(4, manifest["records"])
        for name in os.listdir(first):
            with open(os.path.join(first, name), "rb") as a, open(os.path.join(second, name), "rb") as b:
                self.assertEqual(a.read(), b.read(), name)

    def test_reads_zip_packages(self):
        archive = os.path.join(tempfile.mkdtemp(), "UDID_FULL_RELEASE_TEST.zip")
        with zipfile.ZipFile(archive, "w") as out:
            out.write(FULL, "UDID_FULL_DOWNLOAD_PART1_Of_1.xml")
        self.assertIn("京大械备20260001", read_index(self.build(archive)))


if __name__ == "__main__":
    unittest.main()
