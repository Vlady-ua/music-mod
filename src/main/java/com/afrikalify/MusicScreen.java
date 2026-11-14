package com.afrikalify;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MusicScreen extends Screen {
    private enum Tab { LIBRARY, NOW, QUEUE }

    private static Tab lastTab = Tab.LIBRARY;

    private static final int W = 460;
    private static final int H = 250;

    private static final int COL_WINDOW = 0xF0141420;
    private static final int COL_SIDEBAR = 0xF00E0E18;
    private static final int COL_PANEL = 0xFF1B1B2A;
    private static final int COL_ACCENT = 0xFFC4B5FD;
    private static final int COL_TEXT = 0xFFEDEBFA;
    private static final int COL_DIM = 0xFF8B87A8;

    private final MusicPlayer player = MusicPlayer.INSTANCE;

    private Tab tab = lastTab;
    private List<Path> tracks = new ArrayList<>();
    private Path selected;
    private String filter = "";
    private int scroll;
    private String status = "";
    private boolean dirty;
    private int lastQueueSize = -1;

    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private ButtonWidget loopButton;
    private ButtonWidget pauseButton;

    private int px, py, mx, mw;

    public MusicScreen() {
        super(Text.literal("Afrikalify"));
        this.tracks = MusicLibrary.scan();
        this.selected = player.current();
    }

    // ---------- layout ----------

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        mx = px + 108;
        mw = W - 116;
        rowButtons.clear();

        // sidebar
        int sy = py + 40;
        addDrawableChild(tabButton("Library", Tab.LIBRARY, sy));
        addDrawableChild(tabButton("Now", Tab.NOW, sy + 24));
        addDrawableChild(tabButton("Queue", Tab.QUEUE, sy + 48));
        addDrawableChild(ButtonWidget.builder(Text.literal("Open folder"), b -> openFolder())
                .dimensions(px + 8, sy + 72, 84, 20).build());

        // bottom bar (always visible, like the screenshot)
        int by = py + H - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Play"), b -> onPlay())
                .dimensions(mx, by, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), b -> player.stop())
                .dimensions(mx + 54, by, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Queue"), b -> onEnqueue())
                .dimensions(mx + 108, by, 50, 20).build());
        loopButton = ButtonWidget.builder(loopLabel(), b -> {
            player.setLoop(!player.isLoop());
            b.setMessage(loopLabel());
        }).dimensions(mx + 162, by, 80, 20).build();
        addDrawableChild(loopButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(mx + mw - 50, by, 50, 20).build());

        // tab-specific widgets
        if (tab == Tab.LIBRARY) {
            TextFieldWidget search = new TextFieldWidget(textRenderer, mx, py + 22, mw - 60, 18, Text.literal("Search"));
            search.setPlaceholder(Text.literal("Search local tracks"));
            search.setText(filter);
            search.setChangedListener(s -> {
                filter = s;
                scroll = 0;
                dirty = true;
            });
            addDrawableChild(search);
            addDrawableChild(ButtonWidget.builder(Text.literal("Reload"), b -> {
                tracks = MusicLibrary.scan();
                status = "Found " + tracks.size() + " track(s)";
                dirty = true;
            }).dimensions(mx + mw - 56, py + 22, 56, 18).build());
        } else if (tab == Tab.QUEUE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Clear queue"), b -> {
                player.clearQueue();
                dirty = true;
            }).dimensions(mx, py + 22, 90, 18).build());
        } else {
            pauseButton = ButtonWidget.builder(Text.literal("Pause"), b -> player.togglePause())
                    .dimensions(mx, py + 84, 80, 20).build();
            addDrawableChild(pauseButton);
            addDrawableChild(ButtonWidget.builder(Text.literal("Next"), b -> player.next())
                    .dimensions(mx + 84, py + 84, 80, 20).build());
            addDrawableChild(new VolumeSlider(mx, py + 112, mw, 20));
        }

        rebuildRows();
    }

    private ButtonWidget tabButton(String label, Tab t, int y) {
        String text = (tab == t ? "> " : "") + label;
        return ButtonWidget.builder(Text.literal(text), b -> switchTab(t))
                .dimensions(px + 8, y, 84, 20).build();
    }

    private void switchTab(Tab t) {
        tab = t;
        lastTab = t;
        scroll = 0;
        clearChildren();
        init();
    }

    private Text loopLabel() {
        return Text.literal("Loop: " + (player.isLoop() ? "On" : "Off"));
    }

    private List<Path> filtered() {
        if (filter.isBlank()) return tracks;
        String f = filter.toLowerCase(Locale.ROOT);
        List<Path> out = new ArrayList<>();
        for (Path p : tracks) {
            if (MusicLibrary.displayName(p).toLowerCase(Locale.ROOT).contains(f)) out.add(p);
        }
        return out;
    }

    private void rebuildRows() {
        for (ButtonWidget b : rowButtons) remove(b);
        rowButtons.clear();
        if (tab == Tab.NOW) return;

        int top = py + 46;
        int rowH = 18;
        int visible = Math.max(1, (py + H - 56 - top) / rowH);

        List<Path> src = tab == Tab.LIBRARY ? filtered() : player.queueSnapshot();
        lastQueueSize = player.queueSize();
        int max = Math.max(0, src.size() - visible);
        scroll = Math.max(0, Math.min(scroll, max));

        for (int i = 0; i < visible && scroll + i < src.size(); i++) {
            final int idx = scroll + i;
            final Path p = src.get(idx);
            String name = MusicLibrary.displayName(p);
            boolean sel = tab == Tab.LIBRARY && p.equals(selected);
            String label = (tab == Tab.QUEUE ? (idx + 1) + ". " : sel ? "> " : "") + name;
            label = textRenderer.trimToWidth(label, mw - 12);
            Text t = Text.literal(label);
            if (sel) t = t.copy().formatted(Formatting.LIGHT_PURPLE);
            ButtonWidget b = ButtonWidget.builder(t, btn -> onRowClick(idx, p))
                    .dimensions(mx, top + i * rowH, mw, 16).build();
            rowButtons.add(b);
            addDrawableChild(b);
        }
    }

    // ---------- actions ----------

    private void onRowClick(int index, Path p) {
        if (tab == Tab.LIBRARY) {
            selected = p;
        } else if (tab == Tab.QUEUE) {
            player.removeFromQueue(index);
            status = "Removed from queue";
        }
        dirty = true;
    }

    private void onPlay() {
        boolean selectedIsCurrent = selected != null && selected.equals(player.current());
        if (tab == Tab.LIBRARY && selected != null && !(selectedIsCurrent && player.isPlaying())) {
            player.play(selected);
        } else if (player.isPaused()) {
            player.togglePause();
        } else if (!player.isPlaying() && player.current() != null) {
            player.play(player.current());
        } else if (!player.isPlaying() && selected != null) {
            player.play(selected);
        }
    }

    private void onEnqueue() {
        if (selected == null) {
            status = "Select a song first";
            return;
        }
        player.enqueue(selected);
        status = "Queued: " + MusicLibrary.displayName(selected);
        if (tab == Tab.QUEUE) dirty = true;
    }

    private void openFolder() {
        MusicLibrary.ensureFolder();
        Util.getOperatingSystem().open(MusicLibrary.folder());
        status = "Opened music folder";
    }

    // ---------- rendering ----------

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // intentionally empty: we draw our own dimmed backdrop in render()
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (tab == Tab.QUEUE && player.queueSize() != lastQueueSize) dirty = true;
        if (dirty) {
            dirty = false;
            rebuildRows();
        }
        if (pauseButton != null) pauseButton.setMessage(Text.literal(player.isPaused() ? "Resume" : "Pause"));

        drawChrome(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawChrome(DrawContext ctx) {
        ctx.fill(0, 0, width, height, 0x88000000);
        ctx.fill(px, py, px + W, py + H, COL_WINDOW);
        ctx.fill(px, py, px + 100, py + H, COL_SIDEBAR);
        ctx.fill(px, py, px + W, py + 1, COL_ACCENT);
        ctx.fill(mx - 4, py + 18, mx + mw + 4, py + H - 56, COL_PANEL);

        ctx.drawTextWithShadow(textRenderer, "Afrikalify", px + 10, py + 8, COL_ACCENT);
        ctx.drawTextWithShadow(textRenderer, "Lavender theme", px + 10, py + 20, COL_DIM);

        String header = switch (tab) {
            case LIBRARY -> "Library";
            case NOW -> "Now playing";
            case QUEUE -> "Queue (click a song to remove it)";
        };
        ctx.drawTextWithShadow(textRenderer, header, mx, py + 8, COL_TEXT);
        if (!status.isEmpty()) {
            int sw = textRenderer.getWidth(status);
            ctx.drawTextWithShadow(textRenderer, status, mx + mw - sw, py + 8, COL_DIM);
        }

        // bottom: current track + state
        Path cur = player.current();
        String name = cur == null ? "Nothing playing" : MusicLibrary.displayName(cur);
        String state = !player.isPlaying() ? "Stopped" : player.isPaused() ? "Paused" : "Playing";
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(name, mw), mx, py + H - 52, COL_TEXT);
        ctx.drawTextWithShadow(textRenderer, state, mx, py + H - 42, COL_DIM);

        if (tab == Tab.LIBRARY && filtered().isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "No mp3 files found.", mx + 6, py + 52, COL_DIM);
            ctx.drawTextWithShadow(textRenderer, "Click 'Open folder', drop mp3s in, then 'Reload'.", mx + 6, py + 64, COL_DIM);
        } else if (tab == Tab.QUEUE && player.queueSize() == 0) {
            ctx.drawTextWithShadow(textRenderer, "Queue is empty.", mx + 6, py + 52, COL_DIM);
        }

        if (tab == Tab.NOW) {
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(name, mw), mx, py + 28, COL_ACCENT);
            long pos = player.positionMs();
            long dur = player.durationMs();
            ctx.drawTextWithShadow(textRenderer, fmt(pos) + " / " + (dur > 0 ? fmt(dur) : "--:--"), mx, py + 42, COL_TEXT);
            int barY = py + 58;
            ctx.fill(mx, barY, mx + mw, barY + 6, 0xFF2C2A40);
            if (dur > 0) {
                int filled = (int) (mw * Math.min(1.0, (double) pos / dur));
                ctx.fill(mx, barY, mx + filled, barY + 6, COL_ACCENT);
            }
            ctx.drawTextWithShadow(textRenderer, "Loop: " + (player.isLoop() ? "On" : "Off")
                    + "   Queue: " + player.queueSize() + " song(s)", mx, py + 70, COL_DIM);
        }
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab != Tab.NOW) {
            scroll -= (int) Math.signum(verticalAmount);
            dirty = true;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ---------- volume slider ----------

    private static class VolumeSlider extends SliderWidget {
        VolumeSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(), MusicPlayer.INSTANCE.volume());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Volume: " + Math.round(value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            MusicPlayer.INSTANCE.setVolume((float) value);
        }
    }
}
