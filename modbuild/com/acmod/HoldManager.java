package com.acmod;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class HoldManager {
    private static final Object LOCK = new Object();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long SEGMENT_MS = 59000L;
    private static final long RENEW_MS = 58000L;
    private static final ArrayList<GestureDescription.StrokeDescription> strokes = new ArrayList<>();
    private static final ArrayList<Path> paths = new ArrayList<>();
    private static Object callback;
    private static boolean active;
    private HoldManager() {}

    private static Object get(Object o, String field) throws Exception {
        Field f = o.getClass().getDeclaredField(field); f.setAccessible(true); return f.get(o);
    }
    private static float[] transform(int x, int y) {
        try {
            Class<?> q = Class.forName("C4.q");
            Method m = q.getDeclaredMethod("p", float.class, float.class); m.setAccessible(true);
            Object pair = m.invoke(null, (float)x, (float)y);
            Field fa = pair.getClass().getDeclaredField("a"); fa.setAccessible(true);
            Field fb = pair.getClass().getDeclaredField("b"); fb.setAccessible(true);
            return new float[]{((Number)fa.get(pair)).floatValue(), ((Number)fb.get(pair)).floatValue()};
        } catch (Throwable ignored) { return new float[]{x,y}; }
    }
    private static void invokeCallback(final Object cb, final GestureDescription gesture) {
        if (cb == null || gesture == null) return;
        MAIN.post(() -> {
            try {
                Method selected = null;
                for (Method m : cb.getClass().getMethods())
                    if (m.getName().equals("invoke") && m.getParameterTypes().length == 1) { selected=m; break; }
                if (selected != null) selected.invoke(cb, gesture);
            } catch (Throwable ignored) {}
        });
    }
    private static final Runnable renew = new Runnable() {
        @Override public void run() {
            synchronized (LOCK) {
                if (!active || callback == null || strokes.isEmpty()) return;
                try {
                    GestureDescription.Builder b = new GestureDescription.Builder();
                    ArrayList<GestureDescription.StrokeDescription> next = new ArrayList<>();
                    for (int i=0;i<strokes.size();i++) {
                        GestureDescription.StrokeDescription n = strokes.get(i).continueStroke(paths.get(i),0L,SEGMENT_MS,true);
                        b.addStroke(n); next.add(n);
                    }
                    strokes.clear(); strokes.addAll(next);
                    invokeCallback(callback,b.build());
                    MAIN.removeCallbacks(this); MAIN.postDelayed(this,RENEW_MS);
                } catch (Throwable ignored) {}
            }
        }
    };

    public static void start(List<?> actions, Object cb) {
        if (actions == null || cb == null) return;
        synchronized (LOCK) {
            if (active) return;
            try {
                GestureDescription.Builder b = new GestureDescription.Builder();
                ArrayList<GestureDescription.StrokeDescription> ns = new ArrayList<>();
                ArrayList<Path> np = new ArrayList<>();
                for (Object a: actions) {
                    if (!Customization.isHold(a)) continue;
                    Object xo=get(a,"e"), yo=get(a,"f");
                    if (!(xo instanceof Number) || !(yo instanceof Number)) continue;
                    float[] pt=transform(((Number)xo).intValue(),((Number)yo).intValue());
                    Path p=new Path(); p.moveTo(pt[0],pt[1]); p.lineTo(pt[0]+0.01f,pt[1]);
                    GestureDescription.StrokeDescription s=new GestureDescription.StrokeDescription(p,0L,SEGMENT_MS,true);
                    b.addStroke(s); ns.add(s); np.add(p);
                    if (ns.size()>=10) break;
                }
                if (ns.isEmpty()) return;
                strokes.clear(); strokes.addAll(ns); paths.clear(); paths.addAll(np);
                callback=cb; active=true;
                invokeCallback(callback,b.build());
                MAIN.removeCallbacks(renew); MAIN.postDelayed(renew,RENEW_MS);
            } catch (Throwable ignored) {
                active=false; strokes.clear(); paths.clear(); callback=null;
            }
        }
    }

    public static void augment(GestureDescription.Builder builder) {
        if (builder==null) return;
        synchronized (LOCK) {
            if (!active || strokes.isEmpty()) return;
            try {
                ArrayList<GestureDescription.StrokeDescription> next=new ArrayList<>();
                for (int i=0;i<strokes.size();i++) {
                    GestureDescription.StrokeDescription n=strokes.get(i).continueStroke(paths.get(i),0L,SEGMENT_MS,true);
                    builder.addStroke(n); next.add(n);
                }
                strokes.clear(); strokes.addAll(next);
                MAIN.removeCallbacks(renew); MAIN.postDelayed(renew,RENEW_MS);
            } catch (Throwable ignored) {}
        }
    }

    public static void stop(Object cb) {
        synchronized (LOCK) {
            if (!active) return;
            MAIN.removeCallbacks(renew);
            Object target=cb!=null?cb:callback;
            try {
                GestureDescription.Builder b=new GestureDescription.Builder();
                for (int i=0;i<strokes.size();i++)
                    b.addStroke(strokes.get(i).continueStroke(paths.get(i),0L,1L,false));
                invokeCallback(target,b.build());
            } catch (Throwable ignored) {}
            active=false; strokes.clear(); paths.clear(); callback=null;
        }
    }
}
