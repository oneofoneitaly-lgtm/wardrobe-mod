package it.adryh.aura.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

/** One entry of a settings page. */
public abstract class Setting {
    public final String key;
    public final String label;
    private String hint = "";
    private BooleanSupplier visible = () -> true;

    protected Setting(String key, String label) {
        this.key = key;
        this.label = label;
    }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T hint(String text) {
        this.hint = text;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T onlyIf(BooleanSupplier condition) {
        this.visible = condition;
        return (T) this;
    }

    public String hint() {
        return hint;
    }

    public boolean visible() {
        return visible.getAsBoolean();
    }

    /** Value to store, or null when the entry has nothing to save. */
    public JsonElement save() {
        return null;
    }

    public void load(JsonElement json) {
    }

    public void reset() {
    }

    public static final class Header extends Setting {
        public Header(String label) {
            super("", label);
        }
    }

    public static final class Action extends Setting {
        public final String button;
        public final Runnable run;

        public Action(String label, String button, Runnable run) {
            super("", label);
            this.button = button;
            this.run = run;
        }
    }

    public static final class Toggle extends Setting {
        private final boolean initial;
        private boolean value;

        public Toggle(String key, String label, boolean initial) {
            super(key, label);
            this.initial = initial;
            this.value = initial;
        }

        public boolean on() {
            return value;
        }

        public void set(boolean v) {
            value = v;
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(value);
        }

        @Override
        public void load(JsonElement json) {
            value = json.getAsBoolean();
        }

        @Override
        public void reset() {
            value = initial;
        }
    }

    public static final class Slider extends Setting {
        public final double min;
        public final double max;
        public final double step;
        private final String suffix;
        private final double initial;
        private double value;

        public Slider(String key, String label, double initial, double min, double max, double step, String suffix) {
            super(key, label);
            this.initial = initial;
            this.value = initial;
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
        }

        public double get() {
            return value;
        }

        public float f() {
            return (float) value;
        }

        public int i() {
            return (int) Math.round(value);
        }

        public void set(double v) {
            double snapped = min + Math.round((v - min) / step) * step;
            value = Math.max(min, Math.min(max, snapped));
        }

        public String text() {
            if (step >= 1) {
                return i() + suffix;
            }
            return String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f%s" : "%.2f%s", value, suffix);
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(value);
        }

        @Override
        public void load(JsonElement json) {
            set(json.getAsDouble());
        }

        @Override
        public void reset() {
            value = initial;
        }
    }

    public static final class Choice extends Setting {
        public final String[] options;
        private final int initial;
        private int value;

        public Choice(String key, String label, int initial, String... options) {
            super(key, label);
            this.options = options;
            this.initial = initial;
            this.value = initial;
        }

        public int index() {
            return value;
        }

        public String text() {
            return options[value];
        }

        public void cycle(int direction) {
            value = Math.floorMod(value + direction, options.length);
        }

        public void set(int index) {
            value = Math.max(0, Math.min(options.length - 1, index));
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(value);
        }

        @Override
        public void load(JsonElement json) {
            set(json.getAsInt());
        }

        @Override
        public void reset() {
            value = initial;
        }
    }

    /** A colour; {@link #FOLLOW} means "use the pack accent colour". */
    public static final class Color extends Setting {
        public static final int FOLLOW = -1;
        public final boolean canFollow;
        private final int initial;
        private int value;

        public Color(String key, String label, int initial) {
            this(key, label, initial, initial == FOLLOW);
        }

        public Color(String key, String label, int initial, boolean canFollow) {
            super(key, label);
            this.initial = initial;
            this.value = initial;
            this.canFollow = canFollow;
        }

        public boolean following() {
            return value == FOLLOW;
        }

        /** Opaque ARGB colour. */
        public int get() {
            return value == FOLLOW ? Theme.accent() : 0xFF000000 | value;
        }

        public void set(int rgb) {
            value = rgb == FOLLOW ? FOLLOW : rgb & 0xFFFFFF;
        }

        @Override
        public JsonElement save() {
            return new JsonPrimitive(value);
        }

        @Override
        public void load(JsonElement json) {
            set(json.getAsInt());
        }

        @Override
        public void reset() {
            value = initial;
        }
    }
}
