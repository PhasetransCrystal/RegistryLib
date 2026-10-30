package net.ptcrys.registrylibtest;

import java.awt.Color;
import java.util.Random;

/** Stable colors for generated test textures, independent of provider execution order. */
public final class TestTextureColor {

    private TestTextureColor() {}

    public static Color vibrant(String name) {
        Random random = new Random(name.hashCode());
        float hue = random.nextFloat();
        float saturation = 0.8f + random.nextFloat() * 0.2f;
        float brightness = 0.5f + random.nextFloat() * 0.3f;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    public static Color muted(String name) {
        Random random = new Random(name.hashCode());
        float hue = random.nextFloat();
        float saturation = 0.4f + random.nextFloat() * 0.3f;
        float brightness = 0.4f + random.nextFloat() * 0.4f;
        return Color.getHSBColor(hue, saturation, brightness);
    }
}
