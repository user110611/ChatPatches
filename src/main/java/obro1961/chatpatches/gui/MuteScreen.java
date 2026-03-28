package obro1961.chatpatches.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MuteScreen extends Screen {

    private static final int COLOR_BG     = 0xCC000000;
    private static final int COLOR_BORDER = 0x88FFAA00;
    private static final int COLOR_TITLE  = 0xFFFFAA00;
    private static final int COLOR_TEXT   = 0xFFCCCCCC;
    private static final int COLOR_SELECT = 0xAA0055FF;

    record Violation(String label, String reason, List<String> times) {}

    private static final List<Violation> VIOLATIONS = List.of(
            new Violation("Оскорбление родных",         "Оскорбление родных",                List.of("15m", "30m", "1h", "2h")),
            new Violation("Упоминание родных",           "Упоминание родных",                 List.of("15m", "30m", "1h", "2h")),
            new Violation("Реклама",                     "Реклама",                           List.of("1h", "2h", "6h")),
            new Violation("Мат",                         "Мат",                               List.of("15m", "30m", "1h")),
            new Violation("Скрытый мат",                 "Скрытый мат",                       List.of("15m", "30m", "1h")),
            new Violation("Флуд/Спам",                   "Флуд/Спам",                         List.of("15m", "30m", "1h")),
            new Violation("Оскорбление",                 "Оскорбление",                       List.of("15m", "30m", "1h")),
            new Violation("Межнац. рознь",               "Межнациональная рознь",             List.of("15m", "30m", "1h")),
            new Violation("Тематика 18+",                "Тематика 18+",                      List.of("15m", "30m", "1h", "2h")),
            new Violation("Доксинг",                     "Доксинг",                           List.of("2h", "4h", "6h")),
            new Violation("Казино-бот",                  "Казино-бот",                        List.of("1h", "3h", "6h")),
            new Violation("Громкие звуки (войс)",        "Громкие звуки (войс)",              List.of("15m", "1h", "2h")),
            new Violation("Мат (войс)",                  "Злоупотребление матами (войс)",     List.of("15m", "1h")),
            new Violation("Флуд/Спам",                   "Флуд/Спам",                         List.of("15m", "1h")),
            new Violation("Своя причина",                "",                                  List.of("15m", "30m", "1h", "2h", "6h"))
    );

    private static final int PANEL_W     = 400;
    private static final int PANEL_H     = 300;
    private static final int LIST_X      = 8;
    private static final int LIST_Y      = 56;
    private static final int LIST_ITEM_H = 14;
    private static final int TIME_X      = 220;
    private static final int MAX_TIMES   = 5;

    private final Screen parent;
    private EditBox nickBox;
    private EditBox customTimeBox;
    private EditBox customReasonBox;
    private int selectedViolation = -1;
    private int selectedTime      = -1;
    private final String prefilledNick;

    // статичные кнопки времени
    private final Button[] timeBtns = new Button[MAX_TIMES];

    public MuteScreen(Screen parent) {
        this(parent, "");
    }

    public MuteScreen(Screen parent, String prefilledNick) {
        super(Component.literal("Меню мута"));
        this.parent = parent;
        this.prefilledNick = prefilledNick;
    }

    @Override
    protected void init() {
        int ox = (width  - PANEL_W) / 2;
        int oy = (height - PANEL_H) / 2;

        nickBox = new EditBox(font, ox + LIST_X + 40, oy + 18, 180, 14, Component.literal("Ник"));
        nickBox.setHint(Component.literal("Ник игрока..."));
        nickBox.setMaxLength(64);
        if (!prefilledNick.isEmpty()) nickBox.setValue(prefilledNick);
        addWidget(nickBox);
        setInitialFocus(nickBox);

        // Поле для своего времени (перенесено в правый столбец)
        customTimeBox = new EditBox(font, ox + TIME_X, oy + 192, PANEL_W - TIME_X - LIST_X - 4, 14, Component.literal("Время"));
        customTimeBox.setHint(Component.literal("Например: 12h"));
        customTimeBox.setMaxLength(32);
        addWidget(customTimeBox);

        // Поле для своей причины (перенесено в правый столбец)
        customReasonBox = new EditBox(font, ox + TIME_X, oy + 230, PANEL_W - TIME_X - LIST_X - 4, 14, Component.literal("Причина"));
        customReasonBox.setHint(Component.literal("Введите причину..."));
        customReasonBox.setMaxLength(128);
        addWidget(customReasonBox);

        // кнопки нарушений
        for (int i = 0; i < VIOLATIONS.size(); i++) {
            final int idx = i;
            int iy = oy + LIST_Y + i * LIST_ITEM_H;
            addRenderableWidget(
                    Button.builder(Component.literal(VIOLATIONS.get(i).label()), btn -> selectViolation(idx))
                            .bounds(ox + LIST_X, iy, TIME_X - LIST_X - 4, LIST_ITEM_H - 1).build()
            );
        }

        // статичные кнопки времени (максимум 5)
        for (int i = 0; i < MAX_TIMES; i++) {
            final int ti = i;
            int ty = oy + LIST_Y + i * 22;
            timeBtns[i] = Button.builder(Component.literal("-"), btn -> {
                selectedTime = ti;
                if (selectedViolation >= 0) {
                    Violation v = VIOLATIONS.get(selectedViolation);
                    if (ti < v.times().size()) {
                        customTimeBox.setValue(v.times().get(ti)); // Подставляем время в поле
                    }
                }
            }).bounds(ox + TIME_X, ty, PANEL_W - TIME_X - LIST_X - 4, 18).build();
            timeBtns[i].visible = false;
            addRenderableWidget(timeBtns[i]);
        }

        addRenderableWidget(Button.builder(Component.literal("Замутить"), btn -> sendMute())
                .bounds(ox + PANEL_W - 90, oy + PANEL_H - 22, 84, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), btn -> onClose())
                .bounds(ox + PANEL_W - 180, oy + PANEL_H - 22, 84, 16).build());
    }

    private void selectViolation(int idx) {
        selectedViolation = idx;
        Violation v = VIOLATIONS.get(idx);

        // обновляем кнопки времени
        for (int i = 0; i < MAX_TIMES; i++) {
            if (i < v.times().size()) {
                timeBtns[i].setMessage(Component.literal(v.times().get(i)));
                timeBtns[i].visible = true;
            } else {
                timeBtns[i].visible = false;
            }
        }

        // Автоматически подставляем макс. время и причину в редактируемые поля
        if (!v.times().isEmpty()) {
            selectedTime = v.times().size() - 1;
            customTimeBox.setValue(v.times().get(selectedTime));
        } else {
            selectedTime = -1;
            customTimeBox.setValue("");
        }

        customReasonBox.setValue(v.reason());
    }

    private void sendMute() {
        String nick = nickBox.getValue().trim();
        String time = customTimeBox.getValue().trim();
        String reason = customReasonBox.getValue().trim();

        // Проверяем, что все поля заполнены
        if (nick.isEmpty() || time.isEmpty() || reason.isEmpty()) return;

        String command = "/tempmute " + nick + " " + time + " " + reason;
        Minecraft.getInstance().player.connection.sendChat(command);
        onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        int ox = (width  - PANEL_W) / 2;
        int oy = (height - PANEL_H) / 2;

        gfx.fill(ox, oy, ox + PANEL_W, oy + PANEL_H, COLOR_BG);
        gfx.fill(ox, oy, ox + PANEL_W, oy + 1, COLOR_BORDER);
        gfx.fill(ox, oy + PANEL_H - 1, ox + PANEL_W, oy + PANEL_H, COLOR_BORDER);
        gfx.fill(ox, oy, ox + 1, oy + PANEL_H, COLOR_BORDER);
        gfx.fill(ox + PANEL_W - 1, oy, ox + PANEL_W, oy + PANEL_H, COLOR_BORDER);

        gfx.drawString(font, "=== Меню мута ===", ox + LIST_X, oy + 6, COLOR_TITLE, false);
        gfx.drawString(font, "Ник:", ox + LIST_X, oy + 20, COLOR_TEXT, false);
        gfx.drawString(font, "Нарушение:", ox + LIST_X, oy + LIST_Y - 10, COLOR_TEXT, false);

        nickBox.setX(ox + LIST_X + 40);
        nickBox.setY(oy + 18);
        nickBox.render(gfx, mouseX, mouseY, delta);

        if (selectedViolation >= 0) {
            int iy = oy + LIST_Y + selectedViolation * LIST_ITEM_H;
            gfx.fill(ox + LIST_X, iy, ox + TIME_X - 4, iy + LIST_ITEM_H - 1, COLOR_SELECT);
            gfx.drawString(font, "Время:", ox + TIME_X, oy + LIST_Y - 10, COLOR_TEXT, false);
        }

        // Рендер полей для кастомного ввода (справа внизу)
        gfx.drawString(font, "Свое время:", ox + TIME_X, oy + 180, COLOR_TEXT, false);
        customTimeBox.setX(ox + TIME_X);
        customTimeBox.setY(oy + 192);
        customTimeBox.render(gfx, mouseX, mouseY, delta);

        gfx.drawString(font, "Своя/Точная причина:", ox + TIME_X, oy + 218, COLOR_TEXT, false);
        customReasonBox.setX(ox + TIME_X);
        customReasonBox.setY(oy + 230);
        customReasonBox.render(gfx, mouseX, mouseY, delta);

        // Динамический предпросмотр команды
        String nick = nickBox.getValue().trim();
        if (!nick.isEmpty()) {
            String t = customTimeBox.getValue().trim();
            if (t.isEmpty()) t = "<время>";
            String r = customReasonBox.getValue().trim();
            if (r.isEmpty()) r = "<причина>";

            String preview = "/tempmute " + nick + " " + t + " " + r;
            gfx.drawString(font, preview, ox + LIST_X, oy + PANEL_H - 34, 0xFFAAAAFF, false);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
//.\gradlew "1.21.11-fabric:build" 2>&1 | Select-String "error:"