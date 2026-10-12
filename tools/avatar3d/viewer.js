import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';

// This page receives no prompts, transcripts, credentials or commands. It only renders visual state.
const clamp = (n, low, high, fallback = 0) => Number.isFinite(n) ? Math.max(low, Math.min(high, n)) : fallback;
const POSES = new Set(['IDLE', 'WALKING', 'SEATED', 'KNEELING', 'CROUCHED', 'DANCE_POP', 'DANCE_URBAN']);
const EXPRESSIONS = new Set(['NEUTRAL', 'WARM', 'CURIOUS', 'CONCERNED', 'SAD', 'ANGRY', 'SURPRISED', 'SHY']);
const GESTURES = new Set(['NONE', 'WAVE', 'NOD', 'SHAKE', 'EXPLAIN', 'THINK', 'CELEBRATE', 'LAUGH', 'CRY', 'STARTLE']);
// The first render happens before Android publishes state. Use the same finite defaults.
let state = clean();
let status = 'loading', running = false, raf = 0, lastFrame = 0, lastInput = 0;
let renderer, scene, camera, rig, model, mixer, head, headBase;
let current, currentName = '', actions = {}, faces = [], bed;
let modelHeight = 1.7, modelWidth = 1, worldHeight = 2.1;
let frames = 0;

function clean(value = {}) {
    return {
        pose: POSES.has(value.pose) ? value.pose : 'IDLE',
        gesture: GESTURES.has(value.gesture) ? value.gesture : 'NONE',
        expression: EXPRESSIONS.has(value.expression) ? value.expression : 'NEUTRAL',
        x: clamp(value.x, 0, 1, .3), mouth: clamp(value.mouth, 0, 1), blink: clamp(value.blink, 0, 1),
        headYaw: clamp(value.headYaw, -14, 14), headPitch: clamp(value.headPitch, -10, 10),
        headTilt: clamp(value.headTilt, -12, 12), speaking: value.speaking === true,
        thinking: value.thinking === true, listening: value.listening === true,
        overlay: value.overlay === true, bed: value.bed === true,
    };
}
function chooseClip(s) {
    // Body posture always wins over conversational gestures and speech.
    switch (s.pose) {
        case 'WALKING': return 'Walk_InPlace';
        case 'SEATED': return 'Sit_Down';
        case 'KNEELING': return 'Kneel';
        case 'CROUCHED': return 'Crouch';
        case 'DANCE_POP': case 'DANCE_URBAN': return 'Gentle_Dance';
    }
    if (s.gesture === 'WAVE') return 'Wave';
    if (s.gesture === 'CELEBRATE') return 'Gentle_Dance';
    if (s.thinking || s.listening) return 'Look_Around';
    return 'Idle_Breathe';
}
function selectClip() {
    const name = chooseClip(state);
    if (name === currentName) return;
    const next = actions[name];
    if (!next) throw new Error(`Missing reviewed clip: ${name}`);
    next.reset().setEffectiveWeight(1).setEffectiveTimeScale(1);
    const hold = ['Sit_Down', 'Kneel', 'Crouch', 'Wave'].includes(name);
    next.setLoop(hold ? THREE.LoopOnce : THREE.LoopRepeat, hold ? 1 : Infinity);
    next.clampWhenFinished = hold;
    next.play();
    if (current) { current.fadeOut(.25); next.fadeIn(.25); }
    current = next; currentName = name;
}
function applyFace() {
    const values = {
        JawOpen: state.speaking || state.gesture === 'LAUGH' ? state.mouth : 0,
        // Leave the upper lid slightly relaxed instead of the export's fully open stare.
        // A complete native blink must still close the eyes fully.
        Blink: .08 + .92 * state.blink,
        Smile: ['WARM', 'SHY'].includes(state.expression) || state.gesture === 'LAUGH' ? .5 : 0,
        MouthFrown: ['SAD', 'CONCERNED'].includes(state.expression) ? .45 : 0,
        BrowUp: ['CURIOUS', 'SURPRISED'].includes(state.expression) ? .45 : 0,
        BrowDown: state.expression === 'ANGRY' ? .6 : state.expression === 'SAD' ? .2 : 0,
    };
    for (const mesh of faces) {
        mesh.morphTargetInfluences.fill(0);
        for (const [name, value] of Object.entries(values)) {
            const index = mesh.morphTargetDictionary[name];
            if (index !== undefined) mesh.morphTargetInfluences[index] = value;
        }
    }
}

function prepareAppearance() {
    const matte = new Set(['Salve_Suit', 'Salve_Reconstructed_Pearl', 'Salve_Hair',
        'Salve_Hair_Interior', 'Salve_Glove_White', 'Salve_Face', 'SALVE_FacePatch_Skin',
        'Salve_Skull_Skin', 'SALVE_Sclera']);
    model.traverse(object => {
        if (!object.isMesh) return;
        for (const material of Array.isArray(object.material) ? object.material : [object.material]) {
            // Keep embedded maps, but restore the pearl shell's specular finish for a studio-like render.
            if (matte.has(material.name)) {
                const reflective = material.name === 'Salve_Suit' || material.name === 'Salve_Reconstructed_Pearl';
                const hair = material.name.includes('Hair');
                material.metalness = reflective ? .12 : 0;
                material.roughness = reflective ? .42 : hair ? .52
                    : material.name === 'SALVE_Sclera' ? .28
                    : material.name === 'Salve_Glove_White' ? .46 : .72;
                if ('clearcoat' in material) material.clearcoat = reflective ? .22 : hair ? .12
                    : material.name === 'Salve_Glove_White' ? .1 : 0;
            }
            if (material.name === 'SALVE_Iris_Azure_Cyan_v2') material.emissiveIntensity = .55;
        }
        // Relax only the lower back-hair flare. Keep the fringe, face, body, skin weights
        // and original GLB untouched; all reviewed clips continue to drive this mesh.
        if (object.name.startsWith('Salve_Hair') && !object.morphTargetDictionary) {
            object.geometry = object.geometry.clone();
            const position = object.geometry.attributes.position;
            for (let i = 0; i < position.count; i++) {
                const lowerHair = THREE.MathUtils.smoothstep(1.3 - position.getY(i), 0, .8);
                position.setX(i, position.getX(i) * (1 - .18 * lowerHair));
            }
            position.needsUpdate = true;
            object.geometry.computeVertexNormals();
            object.geometry.computeBoundingBox();
            object.geometry.computeBoundingSphere();
        }
    });
}
function resize() {
    if (!renderer || !camera) return;
    const w = Math.max(1, innerWidth), h = Math.max(1, innerHeight), aspect = w / h;
    // Fit the full hair silhouette, with room for hands and lateral travel even in portrait/overlay.
    worldHeight = Math.max(modelHeight * 1.08, modelWidth * 1.22 / aspect);
    camera.left = -worldHeight * aspect / 2;
    camera.right = worldHeight * aspect / 2;
    camera.top = worldHeight / 2; camera.bottom = -worldHeight / 2;
    camera.position.set(0, modelHeight * .48, 4);
    camera.lookAt(0, modelHeight * .48, 0);
    camera.updateProjectionMatrix();
    renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 2));
    renderer.setSize(w, h, false);
}
function position() {
    const span = Math.max(0, camera.right - camera.left - modelWidth * 1.2);
    rig.position.x = state.overlay ? 0 : (state.x - .5) * span * .8;
    bed.visible = state.bed && !state.overlay;
    bed.position.x = Math.min(camera.right - modelWidth * .45, modelWidth * .7);
}
function step(dt) {
    selectClip();
    if (head && headBase) head.quaternion.copy(headBase);
    mixer.update(Math.min(dt, .1));
    if (head) {
        headBase = head.quaternion.clone();
        head.rotateX(THREE.MathUtils.degToRad(state.headPitch));
        head.rotateY(THREE.MathUtils.degToRad(state.headYaw));
        head.rotateZ(THREE.MathUtils.degToRad(-state.headTilt));
    }
    applyFace(); position();
    renderer.render(scene, camera); frames++;
}
function tick(now) {
    raf = 0;
    if (!running || status !== 'ready') return;
    // If the native owner stops publishing, stop GPU work even if its pause callback was lost.
    if (document.hidden || now - lastInput > 1000) { pause(); return; }
    if (now - lastFrame >= 1000 / 30) {
        step(lastFrame ? (now - lastFrame) / 1000 : 0);
        lastFrame = now;
    }
    raf = requestAnimationFrame(tick);
}
function pause() { running = false; if (raf) cancelAnimationFrame(raf); raf = 0; lastFrame = 0; }
function resume() {
    if (running || status !== 'ready' || document.hidden) return;
    running = true; lastFrame = 0; raf = requestAnimationFrame(tick);
}
function fail(error) {
    status = 'error'; pause();
    document.documentElement.dataset.status = status;
    console.error('Salve 3D unavailable:', error);
}
window.Salve3D = Object.freeze({
    update(value) {
        state = clean(value); lastInput = performance.now();
        if (status === 'ready') { applyFace(); resume(); }
        return status;
    },
    pause,
    // Read-only visual diagnostics used by the offline regression harness.
    inspect() {
        const body = faces.find(m => m.morphTargetDictionary.JawOpen !== undefined);
        return { status, clip: currentName, time: current?.time || 0, running, frames,
            pose: state.pose, mouth: body ? body.morphTargetInfluences[body.morphTargetDictionary.JawOpen] : 0,
            bones: model ? (() => { let n = 0; model.traverse(o => { if (o.isBone) n++; }); return n; })() : 0,
            meshes: faces.length, head: head?.quaternion.toArray(), clips: Object.keys(actions),
            x: rig?.position.x, calls: renderer?.info.render.calls };
    },
});
window.addEventListener('error', event => fail(event.message));
window.addEventListener('unhandledrejection', event => fail(event.reason));
document.addEventListener('visibilitychange', () => { if (document.hidden) pause(); });
window.addEventListener('resize', resize);

async function init() {
    renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'low-power' });
    renderer.setClearColor(0, 0);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.NeutralToneMapping;
    renderer.toneMappingExposure = 1;
    document.body.appendChild(renderer.domElement);
    renderer.domElement.addEventListener('webglcontextlost', event => { event.preventDefault(); fail('context lost'); });
    scene = new THREE.Scene();
    camera = new THREE.OrthographicCamera(-1, 1, 1, -1, .01, 20);
    // Broad neutral fill keeps the painted face readable on the dark Android stage.
    scene.add(new THREE.AmbientLight(0xffffff, 1.35));
    const key = new THREE.DirectionalLight(0xfff6f0, 1.15); key.position.set(-1, 2, 5); scene.add(key);
    const fill = new THREE.DirectionalLight(0xeaf3ff, .55); fill.position.set(2, 1, 3); scene.add(fill);
    const rim = new THREE.DirectionalLight(0x4b8dff, .6); rim.position.set(-2, 1, -4); scene.add(rim);
    let resourceError = null;
    const manager = new THREE.LoadingManager();
    manager.onError = url => { resourceError = url; };
    const gltf = await new GLTFLoader(manager).loadAsync('./SALVE_Animada.glb');
    if (resourceError) throw new Error('Missing embedded texture: ' + resourceError);
    model = gltf.scene;
    prepareAppearance();
    rig = new THREE.Group(); rig.add(model); scene.add(rig);
    model.updateMatrixWorld(true);
    const bounds = new THREE.Box3().setFromObject(model);
    const size = bounds.getSize(new THREE.Vector3()), center = bounds.getCenter(new THREE.Vector3());
    modelHeight = size.y; modelWidth = size.x;
    model.position.x -= center.x; model.position.y -= bounds.min.y; model.position.z -= center.z;
    model.traverse(object => {
        if (object.isMesh) {
            // Animated skin/morph bounds may exceed the rest-pose sphere.
            object.frustumCulled = false;
            if (object.morphTargetDictionary) faces.push(object);
        }
        if (object.isBone && object.name === 'head') head = object;
    });
    mixer = new THREE.AnimationMixer(model);
    for (const source of gltf.animations) {
        // The export contains face keys in every clip. Native TTS/expressions own those channels.
        const clip = new THREE.AnimationClip(source.name, source.duration,
            source.tracks.filter(track => !track.name.includes('morphTargetInfluences')));
        actions[source.name] = mixer.clipAction(clip);
    }
    for (const name of ['Idle_Breathe', 'Look_Around', 'Walk_InPlace', 'Sit_Down', 'Kneel', 'Gentle_Dance', 'Wave', 'Crouch']) {
        if (!actions[name]) throw new Error(`Missing clip ${name}`);
    }
    bed = new THREE.Group();
    const mattress = new THREE.Mesh(new THREE.BoxGeometry(modelWidth * .85, .12, .7),
        new THREE.MeshStandardMaterial({ color: 0xdde8ed, roughness: .65 }));
    mattress.position.y = .25; bed.add(mattress);
    const base = new THREE.Mesh(new THREE.BoxGeometry(modelWidth * .9, .18, .75),
        new THREE.MeshStandardMaterial({ color: 0x173044, emissive: 0x064953, roughness: .4 }));
    base.position.y = .12; bed.add(base); bed.position.z = -.45; bed.visible = false; scene.add(bed);
    resize(); step(0);
    status = 'ready'; document.documentElement.dataset.status = status;
    // Native update starts animation only while a visible host owns the frame pump.
}
init().catch(fail);
