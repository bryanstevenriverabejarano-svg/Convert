const { chromium } = require('playwright');
const http = require('http');
const fs = require('fs');
const path = require('path');
const assert = require('assert/strict');
const root = path.resolve(__dirname, '../..');
const output = path.join(root, 'build/avatar3d-review');
fs.mkdirSync(output, { recursive: true });
const files = {
  '/index.html': 'app/src/main/assets/avatar3d/index.html',
  '/viewer.js': 'app/src/main/assets/avatar3d/viewer.js',
  '/SALVE_Animada.glb': 'art/salve/cabello-cerrado-animada-v2/SALVE_Animada.glb',
};
const server = http.createServer((req, res) => {
  if (!files[req.url]) { res.writeHead(404); return res.end(); }
  res.setHeader('Content-Type', req.url.endsWith('.js') ? 'text/javascript' : req.url.endsWith('.html') ? 'text/html' : 'model/gltf-binary');
  fs.createReadStream(path.join(root, files[req.url])).pipe(res);
});
(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  try {
    const page = await browser.newPage({ viewport: { width: 412, height: 780 }, deviceScaleFactor: 1 });
    const errors = [], external = [];
    page.on('pageerror', e => errors.push(e.message));
    page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
    page.on('request', r => { if (!r.url().startsWith('http://127.0.0.1:') && !r.url().startsWith('blob:')) external.push(r.url()); });
    await page.goto(`http://127.0.0.1:${server.address().port}/index.html`);
    for (let i = 0; i < 600; i++) {
      if (await page.evaluate(() => window.Salve3D && window.Salve3D.inspect().status !== 'loading')) break;
      await page.waitForTimeout(100);
    }
    const inspect = () => page.evaluate(() => window.Salve3D.inspect());
    assert.equal((await inspect()).status, 'ready');
    assert.ok((await inspect()).head.every(Number.isFinite), 'first frame before native input must have a valid head');
    await page.addStyleTag({ content: 'body { background: #112130; }' });
    assert.equal((await inspect()).bones, 81);
    assert.equal((await inspect()).clips.length, 10);
    async function publish(state, milliseconds = 300) {
      const before = (await inspect()).frames;
      await page.evaluate(s => {
        clearInterval(window.pump);
        window.visualState = s;
        window.Salve3D.update(s);
        window.pump = setInterval(() => window.Salve3D.update(window.visualState), 33);
      }, state);
      await page.waitForTimeout(milliseconds);
      // A software GPU on shared CI may need more than 300 ms for its next frame.
      // Assert only after this state has actually been rendered; keep a bounded deadline.
      for (let i = 0; i < 100 && (await inspect()).frames <= before; i++) await page.waitForTimeout(100);
      assert.ok((await inspect()).frames > before, 'native state must produce a rendered frame');
    }
    await publish({ pose: 'IDLE', x: .5, blink: 0 });
    assert.equal((await inspect()).clip, 'Idle_Breathe');
    await page.screenshot({ path: path.join(output, '01-idle.png') });
    const mappings = { WALKING: 'Walk_InPlace', SEATED: 'Sit_Down', KNEELING: 'Kneel', CROUCHED: 'Crouch', DANCE_POP: 'Gentle_Dance', DANCE_URBAN: 'Gentle_Dance' };
    for (const [pose, clip] of Object.entries(mappings)) {
      await publish({ pose, gesture: 'WAVE', speaking: true, mouth: .7, x: .5 });
      assert.equal((await inspect()).clip, clip, `${pose} must survive speech and wave`);
      assert.equal((await inspect()).mouth, .7);
    }
    await publish({ pose: 'SEATED', x: .5, headYaw: 8 });
    for (let i=0; i<100 && (await inspect()).time < 3; i++) await page.waitForTimeout(200);
    const seated = await inspect();
    assert.equal(seated.clip, 'Sit_Down');
    assert.ok(seated.time >= 3);
    await page.screenshot({ path: path.join(output, '02-seated.png') });
    await publish({ pose: 'SEATED', x: .5, speaking: true, mouth: .8, headYaw: 8 });
    assert.equal((await inspect()).time, seated.time, 'speech must not restart a held pose');
    assert.ok((await inspect()).head.every((v, i) => Math.abs(v - seated.head[i]) < 1e-5), 'held head must not accumulate rotations');
    await publish({ pose: 'SEATED', x: .5, speaking: false, mouth: .8 });
    assert.equal((await inspect()).mouth, 0, 'mouth closes at TTS end even if last amplitude is nonzero');
    await publish({ pose: 'KNEELING', x: .5 });
    for (let i=0; i<100 && (await inspect()).time < 3; i++) await page.waitForTimeout(200);
    await page.screenshot({ path: path.join(output, '03-kneel.png') });
    await publish({ pose: 'CROUCHED', x: .5 });
    for (let i=0; i<100 && (await inspect()).time < 3; i++) await page.waitForTimeout(200);
    await page.screenshot({ path: path.join(output, '04-crouch.png') });
    await publish({ pose: 'IDLE', gesture: 'WAVE', expression: 'WARM', x: .5 }, 1000);
    assert.equal((await inspect()).clip, 'Wave');
    await page.screenshot({ path: path.join(output, '05-wave.png') });
    await publish({ pose: 'IDLE', listening: true });
    assert.equal((await inspect()).clip, 'Look_Around');
    await publish({ pose: 'IDLE', expression: 'ANGRY', speaking: true, mouth: .5, x: .5 });
    await page.screenshot({ path: path.join(output, '06-speaking.png') });
    await page.setViewportSize({ width: 190, height: 178 });
    await publish({ pose: 'IDLE', overlay: true, x: 1 });
    assert.equal((await inspect()).x, 0, 'overlay window owns displacement');
    await page.screenshot({ path: path.join(output, '07-overlay.png') });
    await page.evaluate(() => { clearInterval(window.pump); window.Salve3D.pause(); });
    const paused = await inspect();
    await page.waitForTimeout(250);
    assert.equal((await inspect()).frames, paused.frames);
    await publish({ pose: 'IDLE', overlay: true });
    assert.ok((await inspect()).frames > paused.frames, 'resume must restart the renderer');
    await page.evaluate(() => clearInterval(window.pump));
    await page.waitForTimeout(1300);
    assert.equal((await inspect()).running, false, 'lost native owner must stop GPU loop');
    assert.deepEqual(external, [], 'renderer must work entirely offline');
    assert.deepEqual(errors, []);
    fs.writeFileSync(path.join(output, 'result.json'), JSON.stringify({ passed: true, bones: 81, clips: 10, offline: true, checked: ['posture with speech', 'hold endpoints', 'no head drift', 'TTS stop', 'pause/resume', 'lost owner', 'compact overlay'], diagnostics: await inspect() }, null, 2));
    console.log('PASS: real GLB, 81 bones, 10 clips, posture/TTS, held head, pause/resume, offline, overlay.');
  } finally { await browser.close(); server.close(); }
})().catch(error => { console.error(error); server.close(); process.exitCode = 1; });
