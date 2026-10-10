import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
contract = ROOT / "docs" / "api" / "openapi.json"
with contract.open(encoding="utf-8") as handle:
    document = json.load(handle)

assert document["openapi"].startswith("3."), "OpenAPI version missing"
paths = document.get("paths", {})
schemas = document["components"]["schemas"]
refs = []
for path_item in paths.values():
    for operation in path_item.values():
        if not isinstance(operation, dict):
            continue
        for response in operation.get("responses", {}).values():
            if not isinstance(response, dict):
                continue
            if "$ref" in response:
                refs.append(response["$ref"])
            for media in response.get("content", {}).values():
                schema = media.get("schema", {})
                if "$ref" in schema:
                    refs.append(schema["$ref"])
for ref in refs:
    if ref.startswith("#/components/schemas/"):
        assert ref.rsplit("/", 1)[1] in schemas, ref
    if ref.startswith("#/components/responses/"):
        assert ref.rsplit("/", 1)[1] in document["components"]["responses"], ref

assert "/api/v1/auth/register" in paths
assert "/api/v1/auth/login" in paths
assert "/api/v1/admin/tools/{toolId}/policy" in paths
assert "clearGuestQuota" in schemas["ToolPolicyPatch"]["properties"]
print(f"OPENAPI_CONTRACT_PASS paths={len(paths)} refs={len(refs)}")
