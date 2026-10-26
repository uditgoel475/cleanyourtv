package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Displays and rotates a theme's local wallpaper images while Home is visible. */
final class WallpaperPlayer {
    private final FrameLayout root;
    private final Context context;
    private ImageView image;
    private final int backgroundColor;
    private final List<File> files;
    private final long interval;
    private final String order;
    private final boolean sliderEnabled;
    private final SharedPreferences preferences;
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final int width;
    /** config wallpaper_true_color: ARGB_8888 instead of RGB_565. */
    private final Bitmap.Config bitmapConfig;
    private final int height;
    /** wallpaper_dim as an ARGB black, painted into each picture once instead of a full-screen shade view. */
    private final int dimColor;
    /** The picture on screen, released once the next one has been drawn. */
    private Bitmap current;
    private boolean active;
    private boolean closed;
    private boolean loading;
    private String displayed = "";
    private ImageButton previousButton;
    private ImageButton nextButton;
    private AnimatorSet transition;
    private boolean slideFromRight = true;
    /** The picture being decoded while loading, so a press during a change counts from it, not the old one. */
    private File loadingFile;
    /** The latest arrow press made while a change was still loading; shown once it finishes. */
    private File pendingFile;
    private int pendingDirection;
    /** The tile focus came from, given back by the arrow's inward key or Down. */
    private View returnFocus;
    /** Focus came sideways, from a row's edge: the inward key goes back to that tile instead of the other arrow. */
    private boolean enteredFromSide;
    /** The arrow saw a fresh press of its outward key, so that key's repeats (a held key) keep changing the picture. */
    private boolean armed;
    /** The change was started by a held key: it slides faster so the pictures keep coming. */
    private boolean holding;
    /**
     * wallpaper_slider: the pictures either side of the shown one, decoded ahead so an arrow press slides at once.
     * Main thread only; at most two entries, released when they stop being neighbours or memory is trimmed.
     */
    private final Map<String, Bitmap> ready = new HashMap<>();
    /** wallpaper_captions: the shown picture's name from wallpapers/captions.txt; null when off or there are none. */
    private final TextView caption;
    private final Map<String, String> names;
    /** wallpaper_moods: the collection's groups by time of day; empty rotates through all of it. */
    private final Map<String, Set<String>> moods;
    /** config.xml latitude and longitude for the sun's times, and today's {sunrise, sunset} once worked out. */
    private final String latitude, longitude;
    private LocalDate sunDay;
    private int[] sun;

    WallpaperPlayer(Context context, FrameLayout root, Config config) {
        this.context = context;
        this.root = root;
        backgroundColor = config.backgroundColor;
        files = new ArrayList<>(config.wallpapers);
        interval = config.wallpaperRotationMinutes * 60_000L;
        sliderEnabled = config.wallpaperSlider;
        bitmapConfig = config.wallpaperTrueColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        // The dim is painted into the picture (decode), so no full-screen shade layer is drawn on every frame.
        dimColor = config.wallpaperDim > 0 ? (config.wallpaperDim * 255 / 100) << 24 : 0;
        order = config.wallpaperOrder;
        preferences = context.getSharedPreferences("wallpaper_rotation", Context.MODE_PRIVATE);
        moods = config.wallpaperMoods;
        latitude = moods.isEmpty() ? "" : context.getString(R.string.latitude);
        longitude = moods.isEmpty() ? "" : context.getString(R.string.longitude);
        // Playback state belongs to one collection. Changing the collection starts from its first
        // picture instead of resuming a filename that belonged to the previous one.
        String collection = collectionKey(files);
        if (!collection.equals(preferences.getString("collection", ""))) {
            preferences.edit().putString("collection", collection)
                    .remove("image").remove("changed").apply();
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        names = config.wallpaperCaptions && config.wallpaperEnabled && !files.isEmpty()
                ? WallpaperCaptions.read(files.get(0).getParentFile()) : Collections.emptyMap();
        caption = names.isEmpty() ? null : makeCaption(context, root);
        if (config.wallpaperEnabled && !files.isEmpty()) {
            root.addView(image, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (sliderEnabled && files.size() > 1) createArrowControls();
            showFirst();
        }
    }

    /**
     * The picture Home opens with is decoded right here, on the main thread, as the single wallpaper always was:
     * Home's first frame already has it instead of drawing the plain colour and the picture popping in later.
     * Only the later changes of a rotation decode in the background.
     */
    private void showFirst() {
        String previous = preferences.getString("image", "");
        List<File> playable = playable();
        File first = WallpaperRotation.find(playable, previous);
        if (first == null) first = WallpaperRotation.pick(playable, previous, order, random);
        while (first != null) {
            Bitmap bitmap = decode(first);
            if (bitmap != null) {
                showImmediately(bitmap);
                // The picture covers the whole screen: the root's plain colour under it is overdraw.
                root.setBackground(null);
                displayed = first.getName();
                showCaption(false);
                if (!displayed.equals(previous)) {
                    preferences.edit().putString("image", displayed)
                            .putLong("changed", System.currentTimeMillis()).apply();
                }
                prefetchNeighbours();
                return;
            }
            files.remove(first);   // unreadable or over 4K: try another, or keep the plain colour
            playable = playable();
            first = WallpaperRotation.pick(playable, previous, order, random);
        }
    }

    void start() {
        active = true;
        update();
    }

    void stop() {
        active = false;
    }

    /** Drops the wallpaper texture when Home is hidden and Android asks the process to free memory. */
    void releaseForMemoryPressure() {
        if (transition != null) transition.cancel();
        clearReady();
        if (current == null) return;
        image.setImageDrawable(null);
        // The renderer may still have a queued frame that references this hardware bitmap. Drop our references and
        // let Android reclaim its native buffer after rendering releases it instead of recycling it mid-frame.
        current = null;
        displayed = "";
        root.setBackgroundColor(backgroundColor);
    }

    /** The pictures the rotation picks from now: wallpaper_moods' group for the time of day, or the whole collection. */
    private List<File> playable() {
        if (moods.isEmpty()) return files;
        LocalDateTime now = LocalDateTime.now();
        if (!now.toLocalDate().equals(sunDay)) {
            sunDay = now.toLocalDate();
            sun = WallpaperMood.sun(sunDay, latitude, longitude, ZoneId.systemDefault());
        }
        return WallpaperMood.select(files, moods, WallpaperMood.timeOfDay(now.getHour() * 60 + now.getMinute(), sun[0], sun[1]));
    }

    void close() {
        active = false;
        closed = true;
        if (transition != null) transition.cancel();
        decoder.shutdownNow();
        clearReady();
    }

    void update() {
        if (!active || closed || loading || files.isEmpty()) return;
        String previous = preferences.getString("image", "");
        // A picture outside the time of day's group (the group changed) is replaced at once, not after the interval.
        List<File> playable = playable();
        File selected = WallpaperRotation.find(playable, previous);
        long changed = preferences.getLong("changed", 0);
        if (selected == null || WallpaperRotation.due(changed, System.currentTimeMillis(), interval)) {
            selected = WallpaperRotation.pick(playable, previous, order, random);
        }
        if (selected == null || selected.getName().equals(displayed)) return;
        int direction = slideFromRight ? 1 : -1;
        slideFromRight = !slideFromRight;
        load(selected, direction, !selected.getName().equals(previous) || changed == 0);
    }

    private void load(File next, int direction, boolean saveChangeTime) {
        if (loading || closed) return;
        loading = true;
        loadingFile = next;
        // A neighbour decoded ahead slides in at once; anything else is decoded first.
        Bitmap cached = ready.remove(next.getName());
        if (cached != null) {
            present(next, cached, direction, saveChangeTime);
            return;
        }
        decoder.execute(() -> {
            Bitmap bitmap = decode(next);
            root.post(() -> present(next, bitmap, direction, saveChangeTime));
        });
    }

    private void present(File next, Bitmap bitmap, int direction, boolean saveChangeTime) {
        // A change stays loading until its slide has finished: a press during the slide then counts
        // from the incoming picture and is queued, instead of loading the picture already on its way in.
        if (closed || !active) {
            loading = false;
            loadingFile = null;
            pendingFile = null;
            if (bitmap != null) bitmap.recycle();
            return;
        }
        if (bitmap == null) {
            loading = false;
            loadingFile = null;
            files.remove(next);
            Log.w("TvHome.Wallpaper", "Skipping unreadable image: " + next.getName());
            if (!runPending()) update();
            return;
        }
        String outgoing = displayed;
        show(bitmap, direction, outgoing, () -> {
            displayed = next.getName();
            showCaption(true);
            if (saveChangeTime) {
                preferences.edit().putString("image", displayed)
                        .putLong("changed", System.currentTimeMillis()).apply();
            }
            loading = false;
            loadingFile = null;
            // Posted: the slide hands the old picture to retire() first, so the way back is not decoded again.
            if (!runPending()) root.post(this::prefetchNeighbours);
        });
    }

    /** The pictures either side of the given one in the arrows' order; empty without the slider. */
    private List<String> neighbours(String name) {
        List<String> around = new ArrayList<>(2);
        int size = files.size();
        int index = WallpaperRotation.indexOf(files, name);
        if (!sliderEnabled || size < 2 || index < 0) return around;
        around.add(files.get((index + 1) % size).getName());
        String previous = files.get((index - 1 + size) % size).getName();
        if (!around.contains(previous)) around.add(previous);
        return around;
    }

    /** Decodes the shown picture's two neighbours ahead, and lets go of any that no longer are. */
    private void prefetchNeighbours() {
        if (closed || !active && current == null) return;
        List<String> wanted = neighbours(displayed);
        for (Iterator<Map.Entry<String, Bitmap>> it = ready.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bitmap> entry = it.next();
            if (wanted.contains(entry.getKey())) continue;
            Bitmap stale = entry.getValue();
            it.remove();
            root.postDelayed(stale::recycle, 500);
        }
        for (String name : wanted) {
            if (ready.containsKey(name)) continue;
            File file = files.get(WallpaperRotation.indexOf(files, name));
            decoder.execute(() -> {
                Bitmap bitmap = decode(file);
                if (bitmap == null) return;
                root.post(() -> {
                    // Still a neighbour and not already there: keep it; otherwise it is not needed any more.
                    if (!closed && !ready.containsKey(name) && neighbours(displayed).contains(name)
                            && !name.equals(loadingFile == null ? null : loadingFile.getName())) {
                        ready.put(name, bitmap);
                    } else {
                        bitmap.recycle();
                    }
                });
            });
        }
    }

    /** The picture that just left the screen: kept when it is a neighbour (the way back), else released. */
    private void retire(String name, Bitmap bitmap) {
        if (bitmap == null) return;
        if (!closed && !ready.containsKey(name) && neighbours(displayed).contains(name)) {
            ready.put(name, bitmap);
        } else {
            root.postDelayed(bitmap::recycle, 500);
        }
    }

    private void clearReady() {
        for (Bitmap bitmap : ready.values()) root.postDelayed(bitmap::recycle, 500);
        ready.clear();
    }

    /** Shows the press queued during a change; false when there was none. */
    private boolean runPending() {
        File pending = pendingFile;
        pendingFile = null;
        if (pending == null || !files.contains(pending)) return false;
        selectFile(pending, pendingDirection);
        return true;
    }

    /**
     * Moves focus from a row's edge tile to the arrow on that side; false when there are no arrows. A second
     * press in the same direction then changes the picture.
     */
    /** wallpaper_slider is on and there is more than one picture: the two arrows exist. */
    boolean hasArrows() {
        return previousButton != null;
    }

    boolean focusArrow(boolean left, View from) {
        return focusArrow(left, from, true);
    }

    /**
     * Up from the dock: the arrows sit one level above it, so focus goes to the one on that side of the dock. From
     * there the inward key moves to the other arrow, and Down comes back to the tile.
     */
    boolean focusArrowFromBelow(boolean left, View from) {
        return focusArrow(left, from, false);
    }

    private boolean focusArrow(boolean left, View from, boolean fromSide) {
        ImageButton arrow = left ? previousButton : nextButton;
        if (arrow == null || arrow.getVisibility() != View.VISIBLE) return false;
        returnFocus = from;
        enteredFromSide = fromSide;
        armed = false;
        arrow.setFocusable(true);
        if (arrow.requestFocus()) return true;
        arrow.setFocusable(false);
        return false;
    }

    /** Back on an arrow: focus goes back to the tile it came from. */
    void leaveArrow() {
        if ((previousButton != null && previousButton.isFocused()) || (nextButton != null && nextButton.isFocused())) {
            giveFocusBack();
        }
    }

    private void giveFocusBack() {
        View back = returnFocus;
        if (back != null && back.isAttachedToWindow() && back.isShown() && back.requestFocus()) return;
        View grid = root.findViewById(R.id.grid);
        if (grid != null) grid.requestFocus();
    }

    private void createArrowControls() {
        previousButton = makeArrow(R.drawable.ic_wallpaper_left, R.string.wallpaper_previous, -1);
        nextButton = makeArrow(R.drawable.ic_wallpaper_right, R.string.wallpaper_next, 1);
        previousButton.setId(View.generateViewId());
        nextButton.setId(View.generateViewId());
        previousButton.setNextFocusUpId(R.id.btnInputs);
        nextButton.setNextFocusUpId(R.id.btnInputs);

        // Gravity LEFT and RIGHT, not START and END: the Left key always reaches the arrow drawn on the left.
        int top = Math.round(height * 0.55f);
        FrameLayout.LayoutParams previous = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.LEFT);
        previous.leftMargin = dp(20);
        previous.topMargin = top;
        root.addView(previousButton, previous);

        FrameLayout.LayoutParams next = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.RIGHT);
        next.rightMargin = dp(20);
        next.topMargin = top;
        root.addView(nextButton, next);
    }

    private ImageButton makeArrow(int icon, int description, int direction) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(icon);
        button.setColorFilter(Color.WHITE);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(arrowBackground(false));
        button.setContentDescription(context.getString(description));
        // Focusable only while reached from a row's edge (focusArrow): otherwise Home's first focus, or a focus
        // search from the dock or the top bar, could land on an arrow instead of an app.
        button.setFocusable(false);
        button.setOnFocusChangeListener((view, focused) -> {
            if (!focused) button.setFocusable(false);
            button.animate().cancel();
            button.animate().scaleX(focused ? 1.14f : 1f).scaleY(focused ? 1.14f : 1f)
                    .setDuration(150).start();
            button.setColorFilter(focused ? 0xFF111111 : Color.WHITE);
            button.setBackground(arrowBackground(focused));
        });
        // Outward (the arrow's own direction) changes the picture, and keeps changing it while held: each repeat
        // after a slide has finished brings the next one, round and round the collection. A held key counts only
        // after a fresh press on the arrow, so the press that brought focus here does not also change the picture.
        // Inward goes back to the tile when focus came from a row's edge, or to the other arrow when it came up from
        // the dock; Down always goes back to the tile, and Back is the activity's (MainActivity.onBackPressed).
        // Other repeats are swallowed.
        int outward = direction < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
        int inward = direction < 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
        button.setOnKeyListener((view, keyCode, event) -> {
            if (keyCode != outward && keyCode != inward && keyCode != KeyEvent.KEYCODE_DPAD_DOWN) return false;
            if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
            if (keyCode == outward && event.getRepeatCount() > 0) {
                if (armed && !loading) {
                    holding = true;
                    moveWallpaper(direction);
                }
                return true;
            }
            if (event.getRepeatCount() > 0) return true;
            if (keyCode == outward) {
                armed = true;
                holding = false;
                view.playSoundEffect(SoundEffectConstants.CLICK);
                moveWallpaper(direction);
            } else if (keyCode == inward && !enteredFromSide) {
                ImageButton other = direction < 0 ? nextButton : previousButton;
                armed = false;
                other.setFocusable(true);
                if (!other.requestFocus()) other.setFocusable(false);
            } else {
                giveFocusBack();
            }
            return true;
        });
        button.setOnClickListener(v -> {
            holding = false;
            moveWallpaper(direction);
        });
        return button;
    }

    private GradientDrawable arrowBackground(boolean focused) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(focused ? 0xF5FFFFFF : 0x2EFFFFFF);
        if (!focused) background.setStroke(dp(1), 0x38FFFFFF);
        return background;
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private void moveWallpaper(int offset) {
        if (files.isEmpty()) return;
        // Count from where the latest press is headed: a queued press, else the picture loading, else the shown one.
        String from = pendingFile != null ? pendingFile.getName()
                : loadingFile != null ? loadingFile.getName() : displayed;
        // After a memory trim nothing is displayed: count from the saved picture instead of jumping to the first.
        if (from.isEmpty()) from = preferences.getString("image", "");
        int currentIndex = WallpaperRotation.indexOf(files, from);
        if (currentIndex < 0) currentIndex = 0;
        selectFile(files.get((currentIndex + offset + files.size()) % files.size()), offset);
    }

    private void selectFile(File selected, int direction) {
        if (loading) {
            pendingFile = selected;
            pendingDirection = direction;
            return;
        }
        if (selected.getName().equals(displayed)) return;
        load(selected, direction, true);
    }

    /**
     * Small light text under the top-right buttons, above everything else and never focusable. It sits where the
     * top bar ends in every layout, so it does not cover the clock, the stats line or the tiles.
     */
    private static TextView makeCaption(Context context, FrameLayout root) {
        float dp = context.getResources().getDisplayMetrics().density;
        TextView text = new TextView(context);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        text.setTextColor(0xD9FFFFFF);
        text.setShadowLayer(6 * dp, 0, dp, 0xCC000000);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setMaxWidth(Math.round(420 * dp));
        text.setFocusable(false);
        text.setAlpha(0f);
        try {
            text.setTypeface(UiFont.get(context, R.string.font_regular));
        } catch (Exception ignored) {
            // the system font is fine
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        params.topMargin = Math.round(110 * dp);
        params.setMarginEnd(Math.round(62 * dp));
        root.addView(text, params);
        // Under the top-right buttons, wherever the theme puts them (a fixed offset ran into round icon buttons).
        View buttons = root.findViewById(R.id.topButtons);
        if (buttons != null) {
            View.OnLayoutChangeListener place = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (!buttons.isShown()) return;
                int[] at = new int[2], base = new int[2];
                buttons.getLocationInWindow(at);
                root.getLocationInWindow(base);
                int y = at[1] - base[1] + buttons.getHeight() + Math.round(6 * dp);
                int end = root.getWidth() - (at[0] - base[0] + buttons.getWidth());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) text.getLayoutParams();
                if (lp.topMargin == y && lp.getMarginEnd() == end) return;
                lp.topMargin = y;
                lp.setMarginEnd(Math.max(0, end));
                text.setLayoutParams(lp);
            };
            buttons.addOnLayoutChangeListener(place);
            text.addOnLayoutChangeListener(place);
        }
        return text;
    }

    /** The displayed picture's name, faded across a change; a picture without a name hides the caption. */
    private void showCaption(boolean fade) {
        if (caption == null) return;
        String name = names.get(displayed);
        caption.animate().cancel();
        if (name == null) {
            caption.setAlpha(0f);
            return;
        }
        caption.setContentDescription(name);
        if (!fade || caption.getAlpha() == 0f) {
            caption.setText(name);
            caption.setAlpha(1f);
            return;
        }
        caption.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            caption.setText(name);
            caption.animate().alpha(1f).setDuration(350).start();
        }).start();
    }

    /** Names plus sizes: enough to notice a different collection without hashing every image. */
    private static String collectionKey(List<File> images) {
        StringBuilder key = new StringBuilder();
        for (File file : images) key.append(file.getName()).append(':').append(file.length()).append(';');
        return key.toString();
    }

    /** Puts the first or a non-animated picture on screen and releases the previous one after a frame. */
    private void showImmediately(Bitmap bitmap) {
        Bitmap old = current;
        current = bitmap;
        // The wallpaper covers the whole root; avoid drawing the fallback colour under it.
        root.setBackground(null);
        image.setImageBitmap(bitmap);
        if (old != null && old != bitmap) image.postDelayed(old::recycle, 500);
    }

    /**
     * Slides the next picture over the current one, alternating sides, then hands the old one to retire(): kept as
     * a neighbour, or released.
     */
    private void show(Bitmap bitmap, int direction, String outgoing, Runnable completed) {
        if (!sliderEnabled || current == null || displayed.isEmpty()) {
            showImmediately(bitmap);
            completed.run();
            return;
        }

        ImageView oldImage = image;
        Bitmap oldBitmap = current;
        ImageView incoming = new ImageView(context);
        incoming.setScaleType(ImageView.ScaleType.CENTER_CROP);
        incoming.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        incoming.setImageBitmap(bitmap);
        incoming.setTranslationX(direction * width);
        int imageIndex = root.indexOfChild(oldImage);
        root.addView(incoming, imageIndex + 1,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackground(null);
        image = incoming;
        current = bitmap;

        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(
                ObjectAnimator.ofFloat(oldImage, View.TRANSLATION_X, -direction * width),
                ObjectAnimator.ofFloat(incoming, View.TRANSLATION_X, 0f));
        animator.setDuration(holding ? 260 : 380);
        animator.setInterpolator(new DecelerateInterpolator());
        final boolean[] finished = {false};
        Runnable finish = () -> {
            if (finished[0]) return;
            finished[0] = true;
            incoming.setTranslationX(0f);
            root.removeView(oldImage);
            oldImage.setImageDrawable(null);
            if (transition == animator) transition = null;
            completed.run();
            // After completed(): displayed is the new picture, so the old one is kept when it is now a neighbour.
            if (oldBitmap != null && oldBitmap != bitmap) retire(outgoing, oldBitmap);
        };
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { finish.run(); }
            @Override public void onAnimationCancel(Animator animation) { finish.run(); }
        });
        transition = animator;
        animator.start();
    }

    private Bitmap decode(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            int width = options.outWidth, height = options.outHeight;
            if (width <= 0 || height <= 0) return null;
            int sample = WallpaperSize.sampleSize(width, height, this.width, this.height);
            if (!WallpaperSize.accepted(width / sample, height / sample)) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            int[] scale = WallpaperSize.coverScale(width / sample, height / sample, this.width, this.height);
            if (scale != null) {
                options.inScaled = true;
                options.inDensity = scale[0];
                options.inTargetDensity = scale[1];
            }
            options.inPreferredConfig = bitmapConfig;
            options.inMutable = dimColor != 0;
            Bitmap decoded = BitmapFactory.decodeFile(file.getPath(), options);
            if (decoded == null) return null;
            if (dimColor != 0) new Canvas(decoded).drawColor(dimColor);
            // One copy in GPU memory instead of a software bitmap plus its texture; the decode stays off the UI
            // thread for rotations, and Home's return no longer uploads 4-8 MB to the GPU.
            Bitmap gpu = decoded.copy(Bitmap.Config.HARDWARE, false);
            if (gpu == null) return decoded;
            decoded.recycle();
            return gpu;
        } catch (Exception | OutOfMemoryError failure) {
            return null;
        }
    }
}
