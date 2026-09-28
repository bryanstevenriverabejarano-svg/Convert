package salve.avatar;

/** Pixel geometry for the full-screen stage; keeps the entire 2:3 artwork inside its viewport. */
public final class AvatarStageLayout {
    public final float height, centerX, floor, bedScale, bedCenterX;

    private AvatarStageLayout(float height, float centerX, float floor, float bedScale, float bedCenterX) {
        this.height = height; this.centerX = centerX; this.floor = floor;
        this.bedScale = bedScale; this.bedCenterX = bedCenterX;
    }

    public static AvatarStageLayout fit(float width, float availableHeight, float position) {
        width = Math.max(0, width); availableHeight = Math.max(0, availableHeight);
        float height = Math.min(availableHeight * .94f, width * 1.38f);
        float halfWidth = height / 3f;
        float margin = width * .025f;
        float travel = Math.max(0, width - 2 * (halfWidth + margin));
        float x = halfWidth + margin + travel * Math.max(0, Math.min(1, position));
        float floor = (availableHeight + height) / 2f;
        // The bed and sleeping figure occupy 210 x 116 local units, including the Z marks.
        float bedScale = Math.min(width / 240f, availableHeight / 160f);
        return new AvatarStageLayout(height, x, floor, bedScale, width / 2f);
    }
}
