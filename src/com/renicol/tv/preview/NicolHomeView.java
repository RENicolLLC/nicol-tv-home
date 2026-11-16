package com.renicol.tv.preview;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public final class NicolHomeView extends View {
    private static final String[] BACKGROUNDS = {
            "Sonoran Desert Sunset", "Desert Night Sky", "Mountain Sunrise",
            "Calm Ocean", "Evergreen Forest", "Northern Lights", "City Lights",
            "Deep Space", "Rain on Glass", "Warm Living Room", "Blue Gradient",
            "Purple Gradient", "Dark Minimal", "Geometric Color", "Red Rock Canyon"
    };
    private static final int[][] COLORS = {
            {0xFF27184A,0xFFE76D3B,0xFFFFC268}, {0xFF030914,0xFF102A54,0xFF3C5E8A},
            {0xFF18243B,0xFFD47751,0xFFF5D7A4}, {0xFF003C5B,0xFF087E8B,0xFFB7E3E4},
            {0xFF061B13,0xFF185C3E,0xFF76A96B}, {0xFF07152E,0xFF21546A,0xFF35D09B},
            {0xFF090A18,0xFF30275F,0xFFD75A7E}, {0xFF02030B,0xFF11183A,0xFF332B63},
            {0xFF101820,0xFF314452,0xFF718A98}, {0xFF2C1E1B,0xFF845943,0xFFD6A875},
            {0xFF061C35,0xFF086BA8,0xFF56C7E8}, {0xFF160B2C,0xFF642B83,0xFFC45BD9},
            {0xFF050607,0xFF15191D,0xFF252C32}, {0xFF14152B,0xFF006D77,0xFFFFB703},
            {0xFF35110B,0xFF9E3824,0xFFF09455}
    };
    private static final String[] SCENERY_NAMES = {
            "Sonoran Desert Sunset", "Snowy Alpine Dawn", "Winter Forest", "Tropical Beach",
            "Dramatic Ocean Cliffs", "Emerald Waterfall", "Mountain Lake Reflection", "Autumn Forest",
            "Red Rock Canyon", "Rolling Green Highlands", "Lavender Fields", "Japanese Garden",
            "Northern Lights Fjord", "Ha Long Bay — Vietnam", "Bali Rice Terraces — Indonesia",
            "Swiss Alpine Valley", "Black Sand Coast — Iceland", "The Dolomites — Italy",
            "Aegean Cliffs — Greece", "Canadian Rockies", "Fiordland — New Zealand",
            "Sahara Dunes — Morocco", "Patagonia — Argentina", "Great Rift Valley — Kenya",
            "Night Garden — Singapore"
    };
    private static final String[] SCENERY_FILES = {
            "01-sonoran-desert-sunset", "02-snowy-alpine-dawn", "03-winter-forest", "04-tropical-beach",
            "05-ocean-cliffs", "06-emerald-waterfall", "07-mountain-lake", "08-autumn-forest",
            "09-red-rock-canyon", "10-green-highlands", "11-lavender-fields", "12-japanese-garden",
            "13-northern-lights-fjord", "14-vietnam-ha-long-bay", "15-bali-rice-terraces",
            "16-switzerland-alpine-valley", "17-iceland-black-sand", "18-italy-dolomites",
            "19-greece-aegean-cliffs", "20-canada-rockies", "21-new-zealand-fiordland",
            "22-morocco-sahara", "23-argentina-patagonia", "24-kenya-rift-valley",
            "25-singapore-night-garden"
    };
    private static final String[] SETTINGS = {
            "Choose background", "Rotate now", "Background rotation", "Weather", "Clock", "Date", "Animations"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private boolean settingsOpen;
    private boolean backgroundPickerOpen;
    private int settingsFocus;
    private int backgroundPickerFocus;
    private int background;
    private boolean sceneryMode;
    private int sceneryIndex;
    private Bitmap sceneryBitmap;
    private final Map<Integer, Bitmap> thumbnailCache = new HashMap<>();
    private boolean rotation;
    private boolean weatherOn;
    private boolean clockOn;
    private boolean dateOn;
    private boolean animationsOn;
    private final List<AppEntry> apps = new ArrayList<>();
    private int appFocus;
    private boolean editFavoriteOrder;
    private boolean selectLongHandled;
    private String temperature = "--°";
    private String condition = "Loading Phoenix weather…";
    private String highLow = "";
    private String updated = "";
    private boolean weatherLoading;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            invalidate();
            handler.postDelayed(this, 30000L);
        }
    };
    private final Runnable rotateTick = new Runnable() {
        @Override public void run() {
            if (rotation) {
                advanceBackground();
                save();
                invalidate();
            }
            handler.postDelayed(this, 5 * 60 * 1000L);
        }
    };
    private final Runnable weatherRefresh = new Runnable() {
        @Override public void run() {
            if (weatherOn) loadWeather();
            handler.postDelayed(this, 30 * 60 * 1000L);
        }
    };

    public NicolHomeView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        prefs = context.getSharedPreferences("nicol_home", Context.MODE_PRIVATE);
        background = prefs.getInt("background", 0);
        sceneryMode = prefs.getBoolean("scenery_mode", true);
        sceneryIndex = prefs.getInt("scenery_index", 0);
        rotation = prefs.getBoolean("rotation", true);
        weatherOn = prefs.getBoolean("weather", true);
        clockOn = prefs.getBoolean("clock", true);
        dateOn = prefs.getBoolean("date", true);
        animationsOn = prefs.getBoolean("animations", true);
        temperature = prefs.getString("weather_temperature", "--°");
        condition = prefs.getString("weather_condition", "Loading Phoenix weather…");
        highLow = prefs.getString("weather_high_low", "");
        updated = prefs.getString("weather_updated", "");
        if (sceneryMode) loadScenery(sceneryIndex);
        loadApps();
        handler.post(clockTick);
        handler.postDelayed(rotateTick, 5 * 60 * 1000L);
        handler.postDelayed(weatherRefresh, 30 * 60 * 1000L);
        loadWeather();
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        if (sceneryBitmap != null) sceneryBitmap.recycle();
        for (Bitmap bitmap : thumbnailCache.values()) if (bitmap != null) bitmap.recycle();
        thumbnailCache.clear();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (sceneryMode && sceneryBitmap != null) {
            c.drawBitmap(sceneryBitmap, null, new Rect(0, 0, w, h), paint);
        } else {
            int[] colors = COLORS[background];
            paint.setShader(new LinearGradient(0, 0, w, h, colors, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }
        paint.setColor(0x55000000);
        c.drawRect(0, 0, w, h, paint);

        float margin = w * 0.055f;
        drawText(c, "NICOL TV", margin, h * 0.105f, h * 0.045f, Color.WHITE, true);
        drawText(c, "HOME PREVIEW", margin, h * 0.15f, h * 0.024f, 0xFF9DE5FF, true);

        Date now = new Date();
        if (clockOn) drawText(c, new SimpleDateFormat("h:mm", Locale.getDefault()).format(now),
                margin, h * 0.38f, h * 0.17f, Color.WHITE, false);
        if (dateOn) drawText(c, new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now),
                margin, h * 0.46f, h * 0.042f, 0xFFE9F6FA, false);

        if (weatherOn) drawWeather(c, w, h);
        drawApps(c, w, h);
        drawBottomBar(c, w, h);
        if (settingsOpen) drawSettings(c, w, h);
        if (backgroundPickerOpen) drawBackgroundPicker(c, w, h);
    }

    private void drawWeather(Canvas c, int w, int h) {
        float left = w * 0.61f, top = h * 0.13f, right = w * 0.94f, bottom = h * 0.48f;
        paint.setColor(0x66000000);
        c.drawRoundRect(new RectF(left, top, right, bottom), 28, 28, paint);
        drawText(c, "PHOENIX", left + 34, top + 52, h * 0.027f, 0xFF9DE5FF, true);
        drawText(c, temperature, left + 34, top + 155, h * 0.105f, Color.WHITE, false);
        drawText(c, condition, left + 34, top + 205, h * 0.031f, Color.WHITE, false);
        drawText(c, highLow, left + 34, top + 252, h * 0.027f, 0xFFDCEBF0, false);
        drawText(c, updated, left + 34, bottom - 30, h * 0.019f, 0xFFB9CBD1, false);
    }

    private void drawApps(Canvas c, int w, int h) {
        float margin = w * .055f;
        drawText(c, "YOUR APPS", margin, h * .555f, h * .024f, 0xFFD7EFF7, true);
        if (apps.isEmpty()) {
            drawText(c, "No television apps found", margin, h * .69f, h * .03f, Color.WHITE, false);
            return;
        }
        int columns = 5;
        int page = pageForIndex(appFocus);
        int pages = pageCount();
        int start = pageStart(page);
        int count = pageSize(page);
        float gap = w * .012f;
        float rowGap = h * .012f;
        float cardW = (w * .89f - gap * (columns - 1)) / columns;
        float cardH = h * .075f;
        float gridTop = h * .60f;
        for (int i = 0; i < count; i++) {
            int index = start + i;
            int row = i / columns;
            int column = i % columns;
            float left = margin + column * (cardW + gap);
            float top = gridTop + row * (cardH + rowGap);
            float bottom = top + cardH;
            paint.setColor(index == appFocus ? (editFavoriteOrder ? 0xFF8A5823 : 0xFF31516E) : 0xB518273A);
            c.drawRoundRect(new RectF(left, top, left + cardW, bottom), 20, 20, paint);
            if (index == appFocus) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4);
                paint.setColor(0xFFF4DDA8);
                c.drawRoundRect(new RectF(left, top, left + cardW, bottom), 20, 20, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            String label = apps.get(index).label;
            if (label.length() > 16) label = label.substring(0, 15) + "…";
            float textWidth;
            paint.setTextSize(h * .027f);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            textWidth = paint.measureText(label);
            drawText(c, label, left + Math.max(15, (cardW - textWidth) / 2), top + cardH * .62f,
                    h * .027f, Color.WHITE, true);
        }
        String groupTitle = page == 0 ? "FAVORITES  •  " + Math.min(10, apps.size()) + " APPS"
                : "MORE APPS  " + page + " OF " + (pages - 1) + "  •  " + apps.size() + " TOTAL";
        drawText(c, groupTitle,
                w * .72f, h * .585f, h * .019f, 0xFF9DE5FF, true);
        if (page == 0) {
            drawText(c, editFavoriteOrder ? "ARRANGE MODE — SELECT TO SAVE" : "HOLD SELECT TO ARRANGE FAVORITES",
                    margin, h * .585f, h * .017f, editFavoriteOrder ? 0xFFFFD28A : 0xFFB9CBD1, true);
        }
    }

    private void drawBottomBar(Canvas c, int w, int h) {
        paint.setColor(0x77000000);
        c.drawRoundRect(new RectF(w * .055f, h * .79f, w * .945f, h * .925f), 24, 24, paint);
        drawText(c, currentBackgroundName(), w * .08f, h * .852f, h * .031f, Color.WHITE, true);
        String state = rotation ? "ROTATION ON" : "FIXED";
        drawText(c, state, w * .08f, h * .895f, h * .022f, 0xFF9DE5FF, true);
        drawText(c, "◀ ▶  CHOOSE     ▲ ▼  MORE APPS     SELECT  OPEN", w * .39f, h * .852f, h * .021f, Color.WHITE, true);
        drawText(c, "▼  SETTINGS", w * .74f, h * .895f, h * .022f, 0xFF9DE5FF, true);
    }

    private void drawSettings(Canvas c, int w, int h) {
        paint.setColor(0xF20A121B);
        c.drawRoundRect(new RectF(w * .53f, h * .08f, w * .96f, h * .94f), 28, 28, paint);
        drawText(c, "QUICK SETTINGS", w * .57f, h * .16f, h * .038f, Color.WHITE, true);
        for (int i = 0; i < SETTINGS.length; i++) {
            float top = h * (.20f + i * .09f);
            if (i == settingsFocus) {
                paint.setColor(0xFF167AA8);
                c.drawRoundRect(new RectF(w * .555f, top, w * .935f, top + h * .07f), 18, 18, paint);
            }
            boolean enabled = settingValue(i);
            drawText(c, SETTINGS[i], w * .58f, top + h * .048f, h * .027f, Color.WHITE, false);
            String value = i == 0 ? "OPEN" : (i == 1 ? "GO" : (enabled ? "ON" : "OFF"));
            drawText(c, value, w * .875f, top + h * .048f, h * .023f,
                    i < 2 || enabled ? 0xFF9DE5FF : 0xFF9BA5AA, true);
        }
        drawText(c, "↑↓ MOVE   SELECT TOGGLE   MENU CLOSE", w * .57f, h * .88f,
                h * .019f, 0xFFC5D6DC, true);
    }

    private boolean settingValue(int i) {
        switch (i) {
            case 2: return rotation;
            case 3: return weatherOn;
            case 4: return clockOn;
            case 5: return dateOn;
            default: return animationsOn;
        }
    }

    private void toggleSetting(int i) {
        switch (i) {
            case 0:
                backgroundPickerFocus = sceneryMode ? sceneryIndex : SCENERY_NAMES.length + background;
                backgroundPickerOpen = true;
                settingsOpen = false;
                selectLongHandled = true;
                break;
            case 1:
                advanceBackground();
                break;
            case 2: rotation = !rotation; break;
            case 3: weatherOn = !weatherOn; if (weatherOn) loadWeather(); break;
            case 4: clockOn = !clockOn; break;
            case 5: dateOn = !dateOn; break;
            case 6: animationsOn = !animationsOn; break;
        }
        save();
        invalidate();
    }

    private void drawBackgroundPicker(Canvas c, int w, int h) {
        paint.setColor(0xF5081018);
        c.drawRect(0, 0, w, h, paint);
        drawText(c, "CHOOSE BACKGROUND", w * .06f, h * .105f, h * .045f, Color.WHITE, true);
        int page = pickerPage(backgroundPickerFocus);
        int start = pickerPageStart(page);
        int count = pickerPageSize(page);
        String collection = page < 2 ? "SCENERY " + (page + 1) + " OF 2" : "15 GRADIENTS";
        drawText(c, collection + "  •  25 SCENES + 15 GRADIENTS  •  SELECT TO APPLY", w * .06f, h * .15f,
                h * .021f, 0xFF9DE5FF, true);
        int columns = 3;
        float leftMargin = w * .06f, gapX = w * .018f, gapY = h * .018f;
        float cardW = (w * .88f - gapX * 2) / 3f;
        float cardH = h * .125f;
        for (int local = 0; local < count; local++) {
            int i = start + local;
            int row = local / columns, column = local % columns;
            float left = leftMargin + column * (cardW + gapX);
            float top = h * .19f + row * (cardH + gapY);
            RectF card = new RectF(left, top, left + cardW, top + cardH);
            if (i < SCENERY_NAMES.length) {
                Bitmap thumb = loadThumbnail(i);
                if (thumb != null) c.drawBitmap(thumb, null, card, paint);
            } else {
                int gradient = i - SCENERY_NAMES.length;
                int[] colors = COLORS[gradient];
                paint.setShader(new LinearGradient(left, top, left + cardW, top + cardH,
                        colors, null, Shader.TileMode.CLAMP));
                c.drawRoundRect(card, 22, 22, paint);
                paint.setShader(null);
            }
            paint.setColor(0x77000000);
            c.drawRoundRect(card, 22, 22, paint);
            if (i == backgroundPickerFocus) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6);
                paint.setColor(0xFFF4DDA8);
                c.drawRoundRect(card, 22, 22, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            String label = i < SCENERY_NAMES.length ? SCENERY_NAMES[i] : BACKGROUNDS[i - SCENERY_NAMES.length];
            if (label.length() > 27) label = label.substring(0, 26) + "…";
            drawText(c, label, left + 24, top + cardH * .60f, h * .025f, Color.WHITE, true);
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean select = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
        if (backgroundPickerOpen) {
            int next = backgroundPickerFocus;
            int page = pickerPage(next);
            int start = pickerPageStart(page);
            int size = pickerPageSize(page);
            int local = next - start;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && local % 3 > 0) next--;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && local % 3 < 2 && local + 1 < size) next++;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && local >= 3) next -= 3;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && page > 0) {
                int previousPage = page - 1;
                int previousSize = pickerPageSize(previousPage);
                int column = local % 3;
                next = pickerPageStart(previousPage) + Math.min(previousSize - 1, ((previousSize - 1) / 3) * 3 + column);
            }
            else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && local + 3 < size) next += 3;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && page < 2) {
                int column = local % 3;
                next = pickerPageStart(page + 1) + Math.min(column, pickerPageSize(page + 1) - 1);
            }
            else if (select) {
                if (backgroundPickerFocus < SCENERY_NAMES.length) {
                    sceneryMode = true;
                    sceneryIndex = backgroundPickerFocus;
                    loadScenery(sceneryIndex);
                } else {
                    sceneryMode = false;
                    background = backgroundPickerFocus - SCENERY_NAMES.length;
                }
                backgroundPickerOpen = false;
                selectLongHandled = true;
                save();
                invalidate();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                backgroundPickerOpen = false;
                settingsOpen = true;
                invalidate();
                return true;
            }
            backgroundPickerFocus = next;
            invalidate();
            return true;
        }
        if (!settingsOpen && select) {
            if (editFavoriteOrder && event.getRepeatCount() == 0) return true;
            if (event.getRepeatCount() == 0) {
                selectLongHandled = false;
                return true;
            }
            if (appFocus < Math.min(10, apps.size())) {
                editFavoriteOrder = true;
                selectLongHandled = true;
                invalidate();
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            settingsOpen = !settingsOpen;
            invalidate();
            return true;
        }
        if (settingsOpen) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) settingsFocus = (settingsFocus + SETTINGS.length - 1) % SETTINGS.length;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) settingsFocus = (settingsFocus + 1) % SETTINGS.length;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) toggleSetting(settingsFocus);
            else if (keyCode == KeyEvent.KEYCODE_BACK) settingsOpen = false;
            else return super.onKeyDown(keyCode, event);
            invalidate();
            return true;
        }
        if (editFavoriteOrder) {
            int target = appFocus;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && appFocus > 0) target = appFocus - 1;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && appFocus < Math.min(9, apps.size() - 1)) target = appFocus + 1;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && appFocus >= 5) target = appFocus - 5;
            else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && appFocus + 5 < Math.min(10, apps.size())) target = appFocus + 5;
            else return true;
            if (target != appFocus) {
                Collections.swap(apps, appFocus, target);
                appFocus = target;
                saveFavorites();
                invalidate();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !apps.isEmpty()) {
            appFocus = (appFocus + apps.size() - 1) % apps.size(); invalidate(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && !apps.isEmpty()) {
            appFocus = (appFocus + 1) % apps.size(); invalidate(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !apps.isEmpty()) {
            int page = pageForIndex(appFocus);
            int start = pageStart(page);
            int relative = appFocus - start;
            if (relative < 5 && relative + 5 < pageSize(page)) appFocus = start + relative + 5;
            else if (page + 1 < pageCount()) appFocus = pageStart(page + 1) + Math.min(relative % 5, pageSize(page + 1) - 1);
            else settingsOpen = true;
            invalidate(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !apps.isEmpty()) {
            int page = pageForIndex(appFocus);
            int start = pageStart(page);
            int relative = appFocus - start;
            if (relative >= 5) appFocus -= 5;
            else if (page > 0) {
                int priorStart = pageStart(page - 1);
                int priorSize = pageSize(page - 1);
                int priorBottom = priorSize > 5 ? 5 : 0;
                appFocus = priorStart + Math.min(priorBottom + relative, priorSize - 1);
            }
            invalidate(); return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean select = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
        if (select && !settingsOpen) {
            if (selectLongHandled) {
                selectLongHandled = false;
                return true;
            }
            if (editFavoriteOrder) {
                editFavoriteOrder = false;
                saveFavorites();
                invalidate();
                return true;
            }
            if (!apps.isEmpty()) launchApp(apps.get(appFocus));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void loadApps() {
        PackageManager pm = getContext().getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : found) {
            if (info.activityInfo == null || getContext().getPackageName().equals(info.activityInfo.packageName)) continue;
            CharSequence name = info.loadLabel(pm);
            apps.add(new AppEntry(name == null ? info.activityInfo.packageName : name.toString(),
                    info.activityInfo.packageName, info.activityInfo.name));
        }
        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase(Locale.US)));
        orderFavorites();
    }

    private void orderFavorites() {
        String saved = prefs.getString("favorite_order", "");
        String[] defaults = {"Netflix", "YouTube", "Hulu", "Disney+", "Apple TV", "Pluto TV", "ESPN", "FOX One", "CBS", "Paramount+"};
        String[] wanted = saved.isEmpty() ? defaults : saved.split("\\n");
        List<AppEntry> favorites = new ArrayList<>();
        for (String value : wanted) {
            for (AppEntry app : apps) {
                boolean match = saved.isEmpty() ? app.label.equalsIgnoreCase(value) : app.key().equals(value);
                if (match && !favorites.contains(app)) { favorites.add(app); break; }
            }
        }
        for (String label : defaults) {
            if (favorites.size() >= 10) break;
            for (AppEntry app : apps) {
                if (app.label.equalsIgnoreCase(label) && !favorites.contains(app)) { favorites.add(app); break; }
            }
        }
        for (AppEntry app : apps) {
            if (favorites.size() >= 10) break;
            if (!favorites.contains(app)) favorites.add(app);
        }
        apps.removeAll(favorites);
        apps.addAll(0, favorites);
        saveFavorites();
    }

    private void saveFavorites() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < Math.min(10, apps.size()); i++) {
            if (i > 0) value.append('\n');
            value.append(apps.get(i).key());
        }
        prefs.edit().putString("favorite_order", value.toString()).apply();
    }

    private int pageCount() {
        if (apps.size() <= 10) return 1;
        return 1 + (apps.size() - 10 + 9) / 10;
    }

    private int pageForIndex(int index) {
        return index < 10 ? 0 : 1 + (index - 10) / 10;
    }

    private int pageStart(int page) {
        return page == 0 ? 0 : 10 + (page - 1) * 10;
    }

    private int pageSize(int page) {
        int start = pageStart(page);
        return Math.max(0, Math.min(10, apps.size() - start));
    }

    private void launchApp(AppEntry app) {
        try {
            Intent intent = getContext().getPackageManager().getLeanbackLaunchIntentForPackage(app.packageName);
            if (intent == null) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                intent.setComponent(new ComponentName(app.packageName, app.activityName));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            getContext().startActivity(intent);
        } catch (Exception ignored) { }
    }

    private static final class AppEntry {
        final String label;
        final String packageName;
        final String activityName;
        AppEntry(String label, String packageName, String activityName) {
            this.label = label;
            this.packageName = packageName;
            this.activityName = activityName;
        }
        String key() { return packageName + "/" + activityName; }
    }

    private void save() {
        prefs.edit().putInt("background", background).putBoolean("rotation", rotation)
                .putBoolean("scenery_mode", sceneryMode).putInt("scenery_index", sceneryIndex)
                .putBoolean("weather", weatherOn).putBoolean("clock", clockOn)
                .putBoolean("date", dateOn).putBoolean("animations", animationsOn).apply();
    }

    private String currentBackgroundName() {
        return sceneryMode ? SCENERY_NAMES[sceneryIndex] : BACKGROUNDS[background];
    }

    private void advanceBackground() {
        if (sceneryMode) {
            sceneryIndex = (sceneryIndex + 1) % SCENERY_NAMES.length;
            loadScenery(sceneryIndex);
        } else {
            background = (background + 1) % BACKGROUNDS.length;
        }
    }

    private void loadScenery(int index) {
        try (InputStream input = getContext().getAssets().open("scenery/" + SCENERY_FILES[index] + ".webp")) {
            Bitmap next = BitmapFactory.decodeStream(input);
            if (sceneryBitmap != null && sceneryBitmap != next) sceneryBitmap.recycle();
            sceneryBitmap = next;
        } catch (Exception ignored) { sceneryBitmap = null; }
    }

    private Bitmap loadThumbnail(int index) {
        Bitmap cached = thumbnailCache.get(index);
        if (cached != null) return cached;
        try (InputStream input = getContext().getAssets().open("scenery-thumbs/" + SCENERY_FILES[index] + ".webp")) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            thumbnailCache.put(index, bitmap);
            return bitmap;
        } catch (Exception ignored) { return null; }
    }

    private int pickerPage(int index) { return index < 15 ? 0 : (index < 25 ? 1 : 2); }
    private int pickerPageStart(int page) { return page == 0 ? 0 : (page == 1 ? 15 : 25); }
    private int pickerPageSize(int page) { return page == 0 ? 15 : (page == 1 ? 10 : 15); }

    private void loadWeather() {
        if (weatherLoading) return;
        weatherLoading = true;
        new Thread(() -> {
            Exception lastError = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                HttpURLConnection connection = null;
                try {
                    String endpoint = "https://api.open-meteo.com/v1/forecast?latitude=33.4484&longitude=-112.0740"
                            + "&current=temperature_2m,apparent_temperature,weather_code&daily=temperature_2m_max,temperature_2m_min"
                            + "&temperature_unit=fahrenheit&timezone=America%2FPhoenix&forecast_days=1";
                    connection = (HttpURLConnection) new URL(endpoint).openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "Nicol-TV-Home-Preview/0.5");
                    int status = connection.getResponseCode();
                    if (status < 200 || status >= 300) throw new Exception("Weather HTTP status " + status);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder json = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                    reader.close();
                    JSONObject root = new JSONObject(json.toString());
                    JSONObject current = root.getJSONObject("current");
                    JSONObject daily = root.getJSONObject("daily");
                    int temp = (int)Math.round(current.getDouble("temperature_2m"));
                    int feels = (int)Math.round(current.getDouble("apparent_temperature"));
                    int code = current.getInt("weather_code");
                    JSONArray highs = daily.getJSONArray("temperature_2m_max");
                    JSONArray lows = daily.getJSONArray("temperature_2m_min");
                    String nextTemp = temp + "°";
                    String nextCondition = weatherName(code) + "  •  Feels " + feels + "°";
                    String nextHighLow = "High " + Math.round(highs.getDouble(0)) + "°   Low " + Math.round(lows.getDouble(0)) + "°";
                    String nextUpdated = "Updated " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                    prefs.edit().putString("weather_temperature", nextTemp)
                            .putString("weather_condition", nextCondition)
                            .putString("weather_high_low", nextHighLow)
                            .putString("weather_updated", nextUpdated).apply();
                    handler.post(() -> {
                        weatherLoading = false;
                        temperature = nextTemp; condition = nextCondition; highLow = nextHighLow; updated = nextUpdated; invalidate();
                    });
                    return;
                } catch (Exception e) {
                    lastError = e;
                    Log.e("NicolWeather", "Weather request attempt " + attempt + " failed", e);
                    try { Thread.sleep(attempt * 2000L); } catch (InterruptedException ignored) { break; }
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            if (lastError != null) Log.e("NicolWeather", "All weather attempts failed", lastError);
            handler.post(() -> {
                weatherLoading = false;
                if (prefs.contains("weather_temperature")) {
                    updated = "Saved forecast • retrying";
                } else {
                    condition = "Weather temporarily unavailable";
                    updated = "Retrying automatically";
                }
                invalidate();
                handler.postDelayed(() -> loadWeather(), 60 * 1000L);
            });
        }).start();
    }

    private static String weatherName(int code) {
        if (code == 0) return "Clear";
        if (code <= 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Rain showers";
        if (code <= 86) return "Snow showers";
        return "Thunderstorms";
    }

    private void drawText(Canvas c, String text, float x, float y, float size, int color, boolean bold) {
        paint.setShader(null);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        c.drawText(text, x, y, paint);
    }
}
