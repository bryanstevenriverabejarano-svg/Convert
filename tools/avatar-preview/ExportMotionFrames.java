import java.lang.reflect.Field;
import salve.avatar.AvatarMotion;
import salve.avatar.AvatarMotionProtocol;

/** Development preview fixtures from the production director; this does not synthesize audio. */
public final class ExportMotionFrames {
    public static void main(String[] args) throws Exception {
        AvatarMotion motion = new AvatarMotion();
        motion.activate(1);
        motion.beginTurn(1, 1, "Hola Salve");
        motion.response(1, 1, AvatarMotionProtocol.parse("Hola. [[salve_motion:WAVE:WARM]]"));
        StringBuilder json = new StringBuilder("[\n");
        for (int frame = 0; frame < 270; frame++) {
            if (frame == 90) motion.listening(1, true);
            if (frame == 145) {
                motion.listening(1, false);
                motion.beginTurn(1, 2, "Explícame esta idea");
                motion.response(1, 2, AvatarMotionProtocol.parse("Una explicación. [[salve_motion:EXPLAIN:NEUTRAL]]"));
                motion.speechPending(1, 2, "preview");
                motion.speechStart(1, "preview");
            }
            if (frame > 145 && frame < 235 && frame % 15 == 0) motion.speechRange(1, "preview", frame, frame + 4);
            if (frame == 235) motion.speechEnd(1, "preview");
            motion.advance(1f / 30f);
            AvatarMotion.Snapshot snapshot = motion.snapshot();
            if (frame > 0) json.append(",\n");
            json.append("{\"scene\":\"").append(frame < 90 ? "Saludo" : frame < 145 ? "Escucha" : frame < 235 ? "Explicación · muestra sin audio" : "Reposo").append("\"");
            for (Field field : AvatarMotion.Snapshot.class.getFields()) {
                Object value = field.get(snapshot);
                json.append(",\"").append(field.getName()).append("\":");
                if (value instanceof Enum<?>) json.append('"').append(value).append('"');
                else json.append(value);
            }
            json.append('}');
        }
        System.out.println(json.append("\n]"));
    }
}
