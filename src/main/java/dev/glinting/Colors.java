package dev.glinting;

/** Small colour helpers (HSV <-> RGB, hex parsing). */
public final class Colors {
    private Colors() {}

    /** h, s, v all in 0..1. Returns 0xRRGGBB. */
    public static int hsvToRgb(double h, double s, double v) {
        h = h - Math.floor(h);
        s = Math.max(0, Math.min(1, s));
        v = Math.max(0, Math.min(1, v));
        double c = v * s;
        double hp = h * 6.0;
        double x = c * (1 - Math.abs(hp % 2 - 1));
        double m = v - c;
        double r, g, b;
        switch ((int) Math.floor(hp) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        int ri = (int) Math.round((r + m) * 255);
        int gi = (int) Math.round((g + m) * 255);
        int bi = (int) Math.round((b + m) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }

    /** Returns {h, s, v}, each in 0..1. */
    public static double[] rgbToHsv(int rgb) {
        double r = ((rgb >> 16) & 255) / 255.0;
        double g = ((rgb >> 8) & 255) / 255.0;
        double b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double d = max - min;
        double h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h /= 6.0;
        if (h < 0) h += 1.0;
        double s = max == 0 ? 0 : d / max;
        return new double[] {h, s, max};
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** Parses "#RRGGBB" or "RRGGBB". Returns -1 if not a complete 6-digit hex code. */
    public static int parseHex(String text) {
        if (text == null) return -1;
        String t = text.startsWith("#") ? text.substring(1) : text;
        if (t.length() != 6) return -1;
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
