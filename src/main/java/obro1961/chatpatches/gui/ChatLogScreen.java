package obro1961.chatpatches.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import obro1961.chatpatches.ChatLog;

import java.util.ArrayList;
import java.util.List;

public class ChatLogScreen extends Screen {

    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int STATUS_HEIGHT = 16;

    private static final int COLOR_TIME   = 0xFFAA55FF;
    private static final int COLOR_NAME   = 0xFFFFFFFF;
    private static final int COLOR_TEXT   = 0xFFCCCCCC;
    private static final int COLOR_HOVER  = 0x22FFFFFF;
    private static final int COLOR_BORDER = 0x44FFFFFF;
    private static final int COLOR_BG     = 0xAA000000;

    private final Screen parent;

    private EditBox textSearchBox;
    private EditBox nameSearchBox;
    private Button caseSensitiveBtn;
    private Button regexBtn;

    private boolean caseSensitive = false;
    private boolean useRegex = false;

    private final List<String> allMessages = new ArrayList<>();
    private List<String> filteredMessages = new ArrayList<>();

    private int scrollOffset = 0;

    public ChatLogScreen(Screen parent) {
        super(Component.translatable("text.chatpatches.chatlog.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        allMessages.clear();
        for (var msg : ChatLog.getMessages()) {
            allMessages.add(msg.getString());
        }
        filteredMessages = new ArrayList<>(allMessages);
        scrollOffset = Math.max(0, filteredMessages.size() - getVisibleRows());

        int mid = width / 2;

        textSearchBox = new EditBox(font, PADDING, PADDING + 4, mid - PADDING - 60, 16,
                Component.translatable("text.chatpatches.chatlog.search.text"));
        textSearchBox.setHint(Component.translatable("text.chatpatches.chatlog.search.text.hint"));
        textSearchBox.setResponder(s -> applyFilter());
        addWidget(textSearchBox);
        setInitialFocus(textSearchBox);

        nameSearchBox = new EditBox(font, mid - 56, PADDING + 4, mid - PADDING - 60, 16,
                Component.translatable("text.chatpatches.chatlog.search.name"));
        nameSearchBox.setHint(Component.translatable("text.chatpatches.chatlog.search.name.hint"));
        nameSearchBox.setResponder(s -> applyFilter());
        addWidget(nameSearchBox);

        caseSensitiveBtn = Button.builder(getCaseSensitiveLabel(), btn -> {
            caseSensitive = !caseSensitive;
            caseSensitiveBtn.setMessage(getCaseSensitiveLabel());
            applyFilter();
        }).bounds(width - 84, PADDING + 2, 20, 18).build();
        addRenderableWidget(caseSensitiveBtn);

        regexBtn = Button.builder(getRegexLabel(), btn -> {
            useRegex = !useRegex;
            regexBtn.setMessage(getRegexLabel());
            applyFilter();
        }).bounds(width - 62, PADDING + 2, 20, 18).build();
        addRenderableWidget(regexBtn);

        Button.builder(Component.literal("×"), btn -> {
            textSearchBox.setValue("");
            nameSearchBox.setValue("");
            applyFilter();
        }).bounds(width - 40, PADDING + 2, 20, 18).build();

        addRenderableWidget(Button.builder(
                Component.translatable("text.chatpatches.chatlog.back"),
                btn -> onClose()
        ).bounds(width - 80, height - STATUS_HEIGHT - PADDING - 18, 76, 16).build());
    }

    private Component getCaseSensitiveLabel() {
        return caseSensitive ? Component.literal("§eAa") : Component.literal("Aa");
    }

    private Component getRegexLabel() {
        return useRegex ? Component.literal("§e.*") : Component.literal(".*");
    }

    private void applyFilter() {
        String tq = textSearchBox.getValue();
        String nq = nameSearchBox.getValue();

        if (tq.isEmpty() && nq.isEmpty()) {
            filteredMessages = new ArrayList<>(allMessages);
            scrollOffset = Math.max(0, filteredMessages.size() - getVisibleRows());
            return;
        }

        filteredMessages = new ArrayList<>();
        for (String msg : allMessages) {
            boolean textMatch = matchesQuery(msg, tq);
            boolean nameMatch = nq.isEmpty() || matchesName(msg, nq);
            if (textMatch && nameMatch) filteredMessages.add(msg);
        }
        scrollOffset = Math.max(0, filteredMessages.size() - getVisibleRows());
    }

    private boolean matchesQuery(String msg, String query) {
        if (query.isEmpty()) return true;
        try {
            if (useRegex) {
                String flags = caseSensitive ? "" : "(?i)";
                return msg.matches(flags + ".*" + query + ".*");
            } else {
                return caseSensitive ? msg.contains(query) : msg.toLowerCase().contains(query.toLowerCase());
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesName(String msg, String nameQuery) {
        // Убираем временную метку типа [18:45:51] в начале
        String withoutTime = msg.replaceFirst("^\\[\\d{2}:\\d{2}:\\d{2}\\]\\s*", "");
        // Ищем nameQuery просто как подстроку во всём сообщении
        return matchesQuery(withoutTime, nameQuery);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        super.render(gfx, mouseX, mouseY, delta);

        gfx.fill(0, 0, width, TOOLBAR_HEIGHT, COLOR_BG);
        textSearchBox.render(gfx, mouseX, mouseY, delta);
        nameSearchBox.render(gfx, mouseX, mouseY, delta);

        int listY = TOOLBAR_HEIGHT + 2;
        int listH = height - listY - STATUS_HEIGHT - PADDING - 20;
        int listBottom = listY + listH;

        gfx.fill(PADDING - 1, listY - 1, width - PADDING + 1, listBottom + 1, COLOR_BORDER);
        gfx.fill(PADDING, listY, width - PADDING, listBottom, COLOR_BG);

        gfx.enableScissor(PADDING, listY, width - PADDING, listBottom);

        int visibleRows = getVisibleRows();
        int startIdx = Math.max(0, Math.min(scrollOffset, filteredMessages.size() - visibleRows));

        for (int i = 0; i < visibleRows; i++) {
            int msgIdx = startIdx + i;
            if (msgIdx >= filteredMessages.size()) break;

            String msg = filteredMessages.get(msgIdx);
            int y = listY + i * ROW_HEIGHT + 2;

            if (mouseY >= y && mouseY < y + ROW_HEIGHT) {
                gfx.fill(PADDING, y, width - PADDING, y + ROW_HEIGHT, COLOR_HOVER);
            }

            String display = font.width(msg) > width - PADDING * 2 - 4
                    ? font.plainSubstrByWidth(msg, width - PADDING * 2 - 10) + "..."
                    : msg;
            gfx.drawString(font, display, PADDING + 2, y + 1, COLOR_TEXT, false);
        }

        gfx.disableScissor();

        String status = Component.translatable(
                "text.chatpatches.chatlog.status",
                filteredMessages.size(),
                allMessages.size()
        ).getString();
        gfx.drawString(font, status, PADDING, height - STATUS_HEIGHT - PADDING - 18 + 1, 0xAAAAAA, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredMessages.size() - getVisibleRows());
        scrollOffset = (int) Mth.clamp(scrollOffset - verticalAmount * 3, 0, maxScroll);
        return true;
    }

    private int getVisibleRows() {
        int listY = TOOLBAR_HEIGHT + 2;
        int listH = height - listY - STATUS_HEIGHT - PADDING - 20;
        return Math.max(1, listH / ROW_HEIGHT);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent); // null = закрыть в игру, это нормально
    }
}