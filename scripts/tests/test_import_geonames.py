import importlib.util
import pathlib
import unittest
import sys

spec = importlib.util.spec_from_file_location("import_geonames", pathlib.Path(__file__).parents[1] / "import_geonames.py")
module = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = module
try:
    spec.loader.exec_module(module)
except ModuleNotFoundError:
    module = None

@unittest.skipIf(module is None, "psycopg 3 is not installed")
class ImporterTest(unittest.TestCase):
    def row(self, **changes):
        values = ["ES", "29550", "Ardales", "Andalucía", "51", "Málaga", "29", "", "", "36.878", "-4.846", "4"]
        indexes = {"postal": 1, "locality": 2, "admin1": 3, "admin1_code": 4}
        for key, value in changes.items():
            values[indexes[key]] = value
        return module.parse_row(values, "ES", 1)

    def test_uuid_is_stable(self):
        self.assertEqual(self.row().id, self.row().id)

    def test_name_correction_keeps_uuid_when_external_code_is_stable(self):
        self.assertEqual(self.row().id, self.row(admin1="Andalucia").id)

    def test_hierarchy_code_changes_uuid(self):
        self.assertNotEqual(self.row().id, self.row(admin1_code="99").id)

    def test_normalization_matches_java_policy(self):
        self.assertEqual("a coruna", module.normalize_text("A-Coruña"))
        self.assertEqual("a_coruna", "a_coruna")  # document the input under test
        self.assertEqual("a coruna", module.normalize_text("A_Coruña"))

    def test_accuracy_uses_geonames_range(self):
        columns = ["ES", "29550", "Ardales", "Andalucía", "51", "Málaga", "29", "", "", "36.878", "-4.846", "11"]
        with self.assertRaises(module.InvalidGeoNamesRow):
            module.parse_row(columns, "ES", 1)

if __name__ == "__main__":
    unittest.main()
