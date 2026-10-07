import json
import os
import sys
import tempfile
import unittest

SOURCE_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../main/python"))
sys.path.insert(0, SOURCE_ROOT)

import vault_backend


class VaultBackendTest(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()
        self.database_path = os.path.join(self.tempdir.name, "vault.db")
        self.assertEqual("success", self.call("init_db", self.database_path)["status"])

    def tearDown(self):
        self.tempdir.cleanup()

    def call(self, name, *args):
        return json.loads(getattr(vault_backend, name)(*args))

    def test_setup_creates_a_persistent_vault_configuration(self):
        self.assertFalse(self.call("vault_exists")["exists"])

        result = self.call("setup_vault", "salt", "verification-hash")

        self.assertEqual("success", result["status"])
        self.assertTrue(self.call("vault_exists")["exists"])
        self.assertEqual(
            {"salt": "salt", "verification_hash": "verification-hash"},
            self.call("get_master_config")["data"],
        )

    def test_credential_lifecycle_keeps_encrypted_value_opaque(self):
        self.call("setup_vault", "salt", "verification-hash")
        created = self.call(
            "add_credential", "Example", "user@example.com", "ciphertext", "Work"
        )
        credential_id = created["id"]

        listing = self.call("list_credentials")["data"]
        self.assertEqual(1, len(listing))
        self.assertNotIn("encrypted_secret", listing[0])
        self.assertEqual("Example", listing[0]["website"])

        detail = self.call("get_credential", credential_id)["data"]
        self.assertEqual("ciphertext", detail["encrypted_secret"])
        self.assertEqual("success", self.call("toggle_favorite", credential_id, True)["status"])
        self.assertTrue(self.call("list_credentials", True)["data"][0]["is_favorite"])
        self.assertEqual("success", self.call("delete_credential", credential_id)["status"])
        self.assertEqual([], self.call("list_credentials")["data"])

    def test_bulk_category_assignment_and_summary(self):
        self.call("setup_vault", "salt", "verification-hash")
        first = self.call("add_credential", "One", "one", "one-cipher", "")
        second = self.call("add_credential", "Two", "two", "two-cipher", "")

        self.assertEqual(
            "success",
            self.call("assign_category", [first["id"], second["id"]], "Personal")["status"],
        )
        self.assertEqual({"Personal": 2}, self.call("category_summary")["data"])


if __name__ == "__main__":
    unittest.main()
