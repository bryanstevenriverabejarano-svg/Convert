package salve.avatar;

/** Narrow presentation protocol: only fixed enum names and finite numbers reach the local renderer. */
public final class Avatar3dState {
    private Avatar3dState() { }
    public static boolean allowedAsset(String path) {
        return "/avatar3d/index.html".equals(path) || "/avatar3d/viewer.js".equals(path)
                || "/avatar3d/SALVE_Animada.glb".equals(path);
    }
    public static String encode(AvatarState state, AvatarMotion.Snapshot motion, boolean overlay) {
        return "{\"pose\":\"" + state.getPose().name() + "\",\"gesture\":\"" + motion.gesture.name()
                + "\",\"expression\":\"" + motion.expression.name() + "\",\"x\":" + state.getX()
                + ",\"overlay\":" + overlay + ",\"speaking\":" + motion.speaking
                + ",\"thinking\":" + motion.thinking + ",\"listening\":" + motion.listening
                + ",\"mouth\":" + motion.mouthOpen + ",\"blink\":" + motion.blink
                + ",\"headYaw\":" + motion.headYaw + ",\"headPitch\":" + motion.headPitch
                + ",\"headTilt\":" + motion.headTilt + ",\"bed\":" + state.hasBed() + "}";
    }
}
