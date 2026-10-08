package com.acmod;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.CompoundButton;
import java.lang.reflect.Field;

public final class Customization {
    public static final long HOLD_SENTINEL = -777777L;
    private static final String PREFS = "ac_mod_point_settings_v1";
    private Customization() {}

    static Field field(Class<?> c, String name) throws Exception {
        Class<?> x = c;
        while (x != null) {
            try { Field f = x.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException e) { x = x.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    static Object get(Object o, String name) throws Exception { return field(o.getClass(), name).get(o); }
    static long getLong(Object o, String name, long d) {
        try { Object v = get(o, name); if (v instanceof Number) return ((Number)v).longValue(); } catch (Throwable ignored) {}
        return d;
    }
    static int getInt(Object o, String name, int d) {
        try { Object v = get(o, name); if (v instanceof Number) return ((Number)v).intValue(); } catch (Throwable ignored) {}
        return d;
    }
    public static boolean isHold(Object action) {
        if (action == null || !"g6.a$a".equals(action.getClass().getName())) return false;
        try { Object v = get(action, "i"); return v instanceof Long && ((Long)v).longValue() == HOLD_SENTINEL; }
        catch (Throwable ignored) { return false; }
    }
    static String pointKey(Object action, int index) {
        if (action != null) {
            long id = getLong(action, "b", 0L);
            long scenario = getLong(action, "c", 0L);
            if (id != 0L || scenario != 0L) return "p_" + scenario + "_" + id;
            int x = getInt(action, "e", Integer.MIN_VALUE), y = getInt(action, "f", Integer.MIN_VALUE);
            if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) return "xy_" + x + "_" + y + "_" + index;
        }
        return "idx_" + index;
    }
    static int id(Context c, String name) { return c.getResources().getIdentifier(name, "id", c.getPackageName()); }

    public static void setupDialog(Object editDialog, Dialog dialog) {
        if (editDialog == null || dialog == null) return;
        try {
            Object clickAction = get(editDialog, "s");
            View section = dialog.findViewById(id(dialog.getContext(), "mod_click_settings"));
            if (section == null) return;
            if (clickAction == null) { section.setVisibility(View.GONE); return; }
            section.setVisibility(View.VISIBLE);
            final CompoundButton hold = (CompoundButton) dialog.findViewById(id(dialog.getContext(), "mod_hold"));
            final EditText size = (EditText) dialog.findViewById(id(dialog.getContext(), "mod_point_size"));
            final CompoundButton invisible = (CompoundButton) dialog.findViewById(id(dialog.getContext(), "mod_point_invisible"));
            final EditText duration = (EditText) get(editDialog, "y");
            int index = getInt(editDialog, "p", 0);
            String key = pointKey(clickAction, index);
            SharedPreferences p = dialog.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (size != null) size.setText(String.valueOf(p.getInt(key + "_size", 100)));
            if (invisible != null) invisible.setChecked(p.getBoolean(key + "_invisible", false));
            boolean isHold = isHold(clickAction);
            if (hold != null) {
                hold.setChecked(isHold);
                if (duration != null) {
                    if (isHold) duration.setText(String.valueOf(p.getLong(key + "_last_duration", 25L)));
                    duration.setEnabled(!isHold); duration.setAlpha(isHold ? 0.5f : 1.0f);
                }
                hold.setOnCheckedChangeListener((buttonView, checked) -> {
                    if (duration != null) { duration.setEnabled(!checked); duration.setAlpha(checked ? 0.5f : 1.0f); }
                });
            }
        } catch (Throwable ignored) {}
    }

    public static long resolveClickDuration(Object editDialog, long parsedDuration) {
        if (editDialog == null) return parsedDuration;
        try {
            Dialog dialog = (Dialog)get(editDialog, "o");
            if (dialog == null) return parsedDuration;
            View v = dialog.findViewById(id(dialog.getContext(), "mod_hold"));
            if (v instanceof CompoundButton && ((CompoundButton)v).isChecked()) return HOLD_SENTINEL;
        } catch (Throwable ignored) {}
        return parsedDuration;
    }

    public static void savePointPrefs(Object editDialog) {
        if (editDialog == null) return;
        try {
            Object clickAction = get(editDialog, "s");
            if (clickAction == null) return;
            Dialog dialog = (Dialog)get(editDialog, "o");
            if (dialog == null) return;
            int index = getInt(editDialog, "p", 0);
            String key = pointKey(clickAction, index);
            EditText size = (EditText)dialog.findViewById(id(dialog.getContext(), "mod_point_size"));
            CompoundButton invisible = (CompoundButton)dialog.findViewById(id(dialog.getContext(), "mod_point_invisible"));
            CompoundButton hold = (CompoundButton)dialog.findViewById(id(dialog.getContext(), "mod_hold"));
            int s = 100;
            if (size != null) try { s = Integer.parseInt(size.getText().toString().trim()); } catch (Throwable ignored) {}
            if (s < 25) s = 25; if (s > 300) s = 300;
            SharedPreferences.Editor e = dialog.getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(key + "_size", s).putBoolean(key + "_invisible", invisible != null && invisible.isChecked());
            if (hold == null || !hold.isChecked()) {
                try {
                    EditText duration = (EditText)get(editDialog, "y");
                    if (duration != null) e.putLong(key + "_last_duration", Long.parseLong(duration.getText().toString().trim()));
                } catch (Throwable ignored) {}
            }
            e.apply();
        } catch (Throwable ignored) {}
    }

    public static void applyPoint(Object target, View view, WindowManager.LayoutParams lp) {
        if (target == null || view == null || lp == null) return;
        try {
            Object clickAction = get(target, "h"); if (clickAction == null) return;
            Context c = (Context)get(target, "a");
            int index = getInt(target, "b", 0);
            String key = pointKey(clickAction, index);
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int size = p.getInt(key + "_size", 100); if (size < 25) size = 25; if (size > 300) size = 300;
            float scale = size / 100.0f; view.setScaleX(scale); view.setScaleY(scale);
            if (p.getBoolean(key + "_invisible", false)) {
                view.setAlpha(0.0f);
                lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
        } catch (Throwable ignored) {}
    }
}
