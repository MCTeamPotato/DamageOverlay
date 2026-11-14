package me.kall.damageoverlay.impl;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.damageoverlay.DamageOverlay;
import me.kall.damageoverlay.api.DamageApi;
import me.kall.damageoverlay.api.PopupOptions;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@ApiStatus.Internal
public final class DamageRenderer implements DamageApi {
    public static final DamageRenderer INSTANCE = new DamageRenderer();

    private final List<PopupInstance> popupList = new ObjectArrayList<>();
    private final List<ScreenPopup> inScreen = new ObjectArrayList<>();

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Vector4f vec = new Vector4f();

    private int width = 0;
    private int height = 0;

    private float screenX = 0.0F;
    private float screenY = 0.0F;

    public static void setup(@NotNull FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            final IEventBus bus = NeoForge.EVENT_BUS;

            bus.addListener(INSTANCE::tick);
            bus.addListener(INSTANCE::update);
            bus.addListener(INSTANCE::render);
        });
    }

    @Override
    public void popup(double damageAmount, double x, double y, double z, PopupOptions options) {
        Minecraft.getInstance().execute(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double spread = DamageOverlay.SPREAD.getAsDouble();
            double minSlope = DamageOverlay.MIN_SLOPE.getAsDouble();
            double maxSlope = DamageOverlay.MAX_SLOPE.getAsDouble();

            String text = String.format(Locale.ROOT, "%.1f", damageAmount);
            if (options.appendExclamationMark()) {
                text += "!";
            }
            double popupX = x + (random.nextDouble() - 0.5D) * spread;
            double popupZ = z + (random.nextDouble() - 0.5D) * spread;
            float slope = (float) random.nextDouble(minSlope, maxSlope);
            int color = options.color() & 0xFFFFFF;
            float scale = options.scale();
            float endScaleFactor = options.endScaleFactor();
            long life = options.lifetimeMs();
            float riseSpeed = options.riseSpeed();

            this.popupList.add(new PopupInstance(text, popupX, y, popupZ, slope, color, scale, endScaleFactor, life, riseSpeed, Util.getMillis()));
        });
    }

    private void tick(ClientTickEvent.Post event) {
        long now = Util.getMillis();
        this.popupList.removeIf(popup -> now - popup.createdAt() > popup.lifetimeMs());
    }

    private void update(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        this.inScreen.clear();

        if (this.popupList.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        this.projection.set(event.getProjectionMatrix());
        this.modelView.set(event.getModelViewMatrix());

        Window window = minecraft.getWindow();
        this.width = window.getGuiScaledWidth();
        this.height = window.getGuiScaledHeight();

        long now = Util.getMillis();
        Vec3 camera = event.getCamera().getPosition();
        for (PopupInstance popup : this.popupList) {
            if (!this.world2Screen(popup.x() - camera.x, popup.y() - camera.y, popup.z() - camera.z)) continue;
            float age = Math.min(1.0F, (float) (now - popup.createdAt()) / (float) popup.lifetimeMs());
            float travel = popup.riseSpeed() * (float) popup.lifetimeMs() / 1000.0F;
            this.inScreen.add(new ScreenPopup(popup.text(), this.screenX, this.screenY, age, popup.slope(), popup.color(), popup.scale(), popup.endScaleFactor(), travel));
        }
    }

    private boolean world2Screen(double x, double y, double z) {
        this.vec.set((float) x, (float) y, (float) z, 1.0F);
        this.vec.mul(this.modelView);
        this.vec.mul(this.projection);

        if (this.vec.w <= 0.0F) return false;

        float ndcX = this.vec.x / this.vec.w;
        float ndcY = this.vec.y / this.vec.w;
        float ndcZ = this.vec.z / this.vec.w;
        if (ndcZ < -1.0F || ndcZ > 1.0F) return false;

        this.screenX = (ndcX * 0.5F + 0.5F) * this.width;
        this.screenY = (1.0F - (ndcY * 0.5F + 0.5F)) * this.height;
        return true;
    }

    private void render(RenderGuiEvent.Post event) {
        if (this.inScreen.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        PoseStack pose = graphics.pose();

        boolean dropShadow = DamageOverlay.DROP_SHADOW.getAsBoolean();

        for (ScreenPopup popup : this.inScreen) {
            int alpha = (int) ((1.0F - popup.age()) * 255.0F);
            if (alpha < 4) continue;

            float rise = popup.age() * popup.travel();
            float x = popup.x() + rise / popup.slope();
            float y = popup.y() - rise;

            if (x < 0 || x > this.width) continue;
            if (y < 0 || y > this.height) continue;

            pose.pushPose();
            pose.translate(x, y, 0.0F);
            float scale = popup.scale() * (1.0F + (popup.endScaleFactor() - 1.0F) * popup.age());
            pose.scale(scale, scale, 1.0F);
            graphics.drawString(font, popup.text(), -font.width(popup.text()) / 2.0F, 0.0F, (alpha << 24) | popup.color(), dropShadow);
            pose.popPose();
        }
    }

    private record PopupInstance(String text, double x, double y, double z, float slope, int color, float scale, float endScaleFactor, long lifetimeMs, float riseSpeed, long createdAt) {}

    private record ScreenPopup(String text, float x, float y, float age, float slope, int color, float scale, float endScaleFactor, float travel) {}
}
