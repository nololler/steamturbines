package com.xciel.steamturbine.content.nd;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.xciel.steamturbine.client.gui.STGuiTextures;
import com.xciel.steamturbine.network.DiagnoserEditPacket;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class DiagnoserScreen extends AbstractSimiScreen {

    private static final int TEXTURE_WIDTH = STGuiTextures.NETWORK_DIAGNOSER.width;
    private static final int TEXTURE_HEIGHT = STGuiTextures.NETWORK_DIAGNOSER.height;
    private static final int WINDOW_WIDTH = TEXTURE_WIDTH;
    private static final int WINDOW_HEIGHT = TEXTURE_HEIGHT;

    private static final int TITLE_Y_OFFSET = 5;

    private static final int SU_LABEL_X = 30;
    private static final int SU_LABEL_Y = 39;
    private static final int SU_INPUT_X = 105;
    private static final int SU_INPUT_Y = 39;

    private static final int STRESS_TEST_BUTTON_X = 170;
    private static final int STRESS_TEST_BUTTON_Y = 108;
    private static final int STRESS_TEST_STATUS_X = 58;
    private static final int STRESS_TEST_STATUS_Y = 77;

    private static final int CURRENT_READOUTS_X = 8;
    private static final int CURRENT_RPM_LABEL_Y = 109;
    private static final int CURRENT_SU_LABEL_Y = 119;

    private final NetworkDiagnoserBlockEntity be;
    private final float initialMaxTestSU;
    private final boolean initialStressTesting;

    private EditBox suInput;
    private IconButton stressTestButton;
    private Label suLabel;
    private Label currentRpmLabel;
    private Label currentSuLabel;

    public DiagnoserScreen(NetworkDiagnoserBlockEntity be) {
        super(Component.literal("Network Diagnoser"));
        this.be = be;
        this.initialMaxTestSU = be.getMaxTestSU();
        this.initialStressTesting = be.isStressTesting();
    }

    @Override
    protected void init() {
        setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        super.init();

        int x = guiLeft;
        int y = guiTop;

        suInput = new EditBox(font, x + SU_INPUT_X, y + SU_INPUT_Y, 80, 16, Component.literal("SU"));
        suInput.setMaxLength(10);
        suInput.setValue(String.format("%.0f", be.getMaxTestSU()));
        suInput.setBordered(false);
        suInput.setTextColor(0xFFFFFF);
        suInput.setResponder(this::onSUChanged);
        addRenderableWidget(suInput);

        stressTestButton = new IconButton(x + STRESS_TEST_BUTTON_X, y + STRESS_TEST_BUTTON_Y, 50, 18, AllIcons.I_CONFIRM);
        stressTestButton.setToolTip(Component.literal("Toggle Stress Test"));
        stressTestButton.withCallback(this::onStressTestToggle);
        stressTestButton.green = be.isStressTesting();
        addRenderableWidget(stressTestButton);

        suLabel = new Label(x + SU_LABEL_X, y + SU_LABEL_Y, Component.literal(""));
        suLabel.text = Component.literal("Max SU:").withStyle(ChatFormatting.GRAY);
        addRenderableWidget(suLabel);

        currentRpmLabel = new Label(x + CURRENT_READOUTS_X, y + CURRENT_RPM_LABEL_Y, Component.literal(""));
        currentRpmLabel.text = Component.literal("Network RPM: --").withStyle(ChatFormatting.DARK_GRAY);
        addRenderableWidget(currentRpmLabel);

        currentSuLabel = new Label(x + CURRENT_READOUTS_X, y + CURRENT_SU_LABEL_Y, Component.literal(""));
        currentSuLabel.text = Component.literal("Network SU: --/--").withStyle(ChatFormatting.DARK_GRAY);
        addRenderableWidget(currentSuLabel);
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        STGuiTextures.NETWORK_DIAGNOSER.render(graphics, x, y);

        graphics.drawString(font, title, x + WINDOW_WIDTH / 2 - font.width(title) / 2, y + TITLE_Y_OFFSET, 0x592424, false);

        renderAdditional(graphics, mouseX, mouseY, partialTicks, x, y);
    }

    protected void renderAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, int x, int y) {
        float speed = be.getSpeed();
        float stress = be.getNetworkStress();
        float capacity = be.getNetworkCapacity();

        currentRpmLabel.text = Component.literal(String.format("N-RPM: %.0f", speed));
        currentSuLabel.text = Component.literal(String.format("N-SU: %.0f / %.0f", stress, capacity));

        String stressTestStatus = stressTestButton.green ? "ON" : "OFF";
        ChatFormatting color = stressTestButton.green ? ChatFormatting.GREEN : ChatFormatting.RED;
        graphics.drawString(font, "Stress Test: " + stressTestStatus, x + STRESS_TEST_STATUS_X, y + STRESS_TEST_STATUS_Y, color.getColor() != null ? color.getColor() : 0xFFFFFF, false);
    }

    private void onSUChanged(String text) {
        float newSU = initialMaxTestSU;
        try {
            if (!text.isEmpty()) {
                newSU = Float.parseFloat(text);
            }
        } catch (NumberFormatException ignored) {
        }
        CatnipServices.NETWORK.sendToServer(
            DiagnoserEditPacket.create(be.getBlockPos(), newSU, stressTestButton.green)
        );
    }

    private void onStressTestToggle() {
        boolean newState = !stressTestButton.green;
        stressTestButton.green = newState;
        CatnipServices.NETWORK.sendToServer(
            DiagnoserEditPacket.create(be.getBlockPos(), getCurrentSU(), newState)
        );
    }

    private float getCurrentSU() {
        try {
            String text = suInput.getValue();
            if (!text.isEmpty()) {
                return Float.parseFloat(text);
            }
        } catch (NumberFormatException ignored) {
        }
        return initialMaxTestSU;
    }

    private void sendPacket() {
        float newSU = initialMaxTestSU;
        boolean newStressTesting = initialStressTesting;

        try {
            if (!suInput.getValue().isEmpty()) {
                newSU = Float.parseFloat(suInput.getValue());
            }
        } catch (NumberFormatException ignored) {
        }

        newStressTesting = stressTestButton.green;

        if (newSU != initialMaxTestSU || newStressTesting != initialStressTesting) {
            CatnipServices.NETWORK.sendToServer(
                DiagnoserEditPacket.create(be.getBlockPos(), newSU, newStressTesting)
            );
        }
    }

    @Override
    public void removed() {
        sendPacket();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        int x = guiLeft;
        int y = guiTop;

        if (pButton == 0) {
            if (isMouseOver(x + SU_INPUT_X, y + SU_INPUT_Y, 80, 16, pMouseX, pMouseY)) {
                suInput.mouseClicked(pMouseX, pMouseY, pButton);
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (suInput.isFocused()) {
            if (pKeyCode == 256) {
                suInput.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private boolean isMouseOver(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}