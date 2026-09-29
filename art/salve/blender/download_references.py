import concurrent.futures, hashlib, json, pathlib, urllib.request
root = pathlib.Path(__file__).parent
manifest = json.loads((root/'references/manifest.json').read_text(encoding='utf-8'))
commit = '1ccd26e276925b95ec6f5becd7d53070e1c5498e'
def get(v):
    target = root/'references'/pathlib.Path(v['path']).name
    if not target.exists():
        url = f'https://raw.githubusercontent.com/bryanstevenriverabejarano-svg/Convert/{commit}/app/src/main/assets/{v["path"]}'
        with urllib.request.urlopen(url, timeout=60) as response: data = response.read()
        assert hashlib.sha256(data).hexdigest() == v['sha256'], v['id']
        target.write_bytes(data)
    assert hashlib.sha256(target.read_bytes()).hexdigest() == v['sha256'], v['id']
    return v['id']
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
    verified = list(pool.map(get, manifest['views']))
(root/'reference_verification.json').write_text(json.dumps({'commit':commit,'verified':verified,'count':len(verified)},indent=2),encoding='utf-8')
print('VERIFIED',len(verified),'original references')
