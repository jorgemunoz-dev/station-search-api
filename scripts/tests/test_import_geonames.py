import importlib.util, pathlib, unittest
spec=importlib.util.spec_from_file_location("import_geonames",pathlib.Path(__file__).parents[1]/"import_geonames.py")
module=importlib.util.module_from_spec(spec)
try: spec.loader.exec_module(module)
except ModuleNotFoundError: module=None
@unittest.skipIf(module is None,"psycopg 3 is not installed")
class IdTest(unittest.TestCase):
 def test_uuid_is_stable(self): self.assertEqual(module.stable_id("ES","29550","ardales","andalucia","malaga","ardales"),module.stable_id("ES","29550","ardales","andalucia","malaga","ardales"))
 def test_hierarchy_changes_uuid(self): self.assertNotEqual(module.stable_id("ES","1","springfield","a","b","c"),module.stable_id("ES","1","springfield","x","b","c"))
if __name__=="__main__":unittest.main()
