// Capture the real GLB before/after a renderer change, not an illustration.
// Usage: node tools/avatar3d/review-appearance.cjs <baseline-git-ref> [output-directory]
const { chromium } = require('playwright');
const esbuild = require('esbuild');
const { execFileSync } = require('child_process');
const fs = require('fs'), path = require('path'), http = require('http');
const root = path.resolve(__dirname, '../..');
const baseline = process.argv[2];
if (!baseline) throw new Error('Provide the baseline Git commit to compare.');
const output = path.resolve(process.argv[3] || path.join(root, 'build/appearance-review'));
fs.mkdirSync(output, { recursive: true });
const sources = {
    before: execFileSync('git', ['show', `${baseline}:tools/avatar3d/viewer.js`], { cwd: root, encoding: 'utf8' }),
    after: fs.readFileSync(path.join(__dirname, 'viewer.js'), 'utf8'),
};
// This access point exists only in the in-memory review bundles, never in the APK.
const reviewHook = `
window.captureReview = function (closeUp) {
    pause(); state = clean({pose:'IDLE', x:.5});
    mixer.setTime(0); step(0);
    if (closeUp) {
        camera.zoom = 4;
        camera.position.set(0, modelHeight * .89, 4);
        camera.lookAt(0, modelHeight * .89, 0);
        camera.updateProjectionMatrix(); renderer.render(scene, camera);
    }
};`;
const bundles = Object.fromEntries(Object.entries(sources).map(([name, source]) => [name,
    esbuild.buildSync({ stdin: { contents: source + reviewHook, resolveDir: __dirname }, bundle: true,
        format: 'iife', write: false }).outputFiles[0].contents]));
const html = fs.readFileSync(path.join(root, 'app/src/main/assets/avatar3d/index.html'), 'utf8');
const modelPath = path.join(root, 'art/salve/cabello-cerrado-animada-v2/SALVE_Animada.glb');
const server = http.createServer((req, res) => {
    const [name, file] = req.url.slice(1).split('/');
    if (!bundles[name]) { res.writeHead(404); return res.end(); }
    if (file === 'index.html') { res.setHeader('Content-Type', 'text/html'); return res.end(html); }
    if (file === 'viewer.js') { res.setHeader('Content-Type', 'text/javascript'); return res.end(bundles[name]); }
    if (file === 'SALVE_Animada.glb') { res.setHeader('Content-Type', 'model/gltf-binary'); return fs.createReadStream(modelPath).pipe(res); }
    res.writeHead(404); res.end();
});
(async () => {
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const browser = await chromium.launch({ headless: true,
        args: ['--no-sandbox', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
    try {
        for (const name of Object.keys(sources)) {
            const page = await browser.newPage({ viewport: { width: 600, height: 900 } });
            const errors = [];
            page.on('pageerror', e => errors.push(e.message));
            await page.goto(`http://127.0.0.1:${server.address().port}/${name}/index.html`);
            for (let i = 0; i < 600; i++) {
                if (await page.evaluate(() => window.Salve3D?.inspect().status !== 'loading' && !!window.Salve3D)) break;
                await page.waitForTimeout(100);
            }
            if (await page.evaluate(() => window.Salve3D?.inspect().status) !== 'ready') throw new Error('GLB failed to load');
            await page.addStyleTag({ content: 'body { background: #112130; }' });
            await page.evaluate(() => window.captureReview(false));
            await page.screenshot({ path: path.join(output, `${name}.png`) });
            await page.evaluate(() => window.captureReview(true));
            await page.screenshot({ path: path.join(output, `${name}-face.png`) });
            if (errors.length) throw new Error(errors.join('\n'));
            await page.close();
        }
        for (const [suffix, title] of [['', 'SALVE · CUERPO'], ['-face', 'SALVE · ROSTRO']]) {
            const page = await browser.newPage({ viewport: { width: 1200, height: 1040 } });
            const image = name => `data:image/png;base64,${fs.readFileSync(path.join(output, name + suffix + '.png')).toString('base64')}`;
            await page.setContent(`<html><style>*{box-sizing:border-box}body{margin:0;background:#112130;color:#ecf5fa;font:24px Arial}header{height:85px;padding:24px 30px}main{display:flex}section{width:600px}h2{height:40px;margin:0;padding-left:30px;font-size:21px}img{display:block;width:600px;height:900px}footer{height:15px}</style><header>${title} · RENDERS DEL MISMO GLB</header><main><section><h2>ANTES</h2><img src="${image('before')}"></section><section><h2>PROPUESTA · AÚN EN REVISIÓN</h2><img src="${image('after')}"></section></main></html>`);
            await page.evaluate(async () => Promise.all([...document.images].map(img => img.decode())));
            await page.screenshot({ path: path.join(output, `comparison${suffix}.png`) });
            await page.close();
        }
        console.log(`Real GLB comparisons saved to ${output}`);
    } finally { await browser.close(); server.close(); }
})().catch(error => { console.error(error); server.close(); process.exitCode = 1; });
