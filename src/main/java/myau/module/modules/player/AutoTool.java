package myau.module.modules.player;

import myau.module.modules.combat.KillAura;
import myau.module.modules.render.HUD;
import myau.Myau;
import myau.component.SlotComponent;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.TickEvent;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.util.player.IInventoryPlayerAccessor;
import myau.util.player.ItemUtil;
import myau.util.network.PacketUtil;
import myau.util.player.TeamUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import myau.util.shader.RoundedUtils;

import java.awt.Color;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    /** The real slot that was held before AutoTool started spoofing. */
    private int serverSlot = -1;
    /** The slot AutoTool is currently spoofing. */
    private int spoofedToolSlot = -1;

    private float animationProgress = 0f;
    private long lastFrame = System.currentTimeMillis();
    private ItemStack lastSpoofedStack = null;

    public AutoTool() {
        super("AutoTool", false);
    }

    public boolean isKillAura() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura == null || !killAura.isEnabled()) return false;
        return TeamUtil.isEntityLoaded(killAura.getTarget()) && killAura.isAttackAllowed();
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.resetState();
            return;
        }

        if (!this.canAutoTool()) {
            this.resetState();
            return;
        }

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        int slot = this.findBestHotbarTool(block);
        if (slot == -1 || slot == Myau.slotComponent.getItemIndex()) return;

        this.selectTool(slot);
    }

    private boolean canAutoTool() {
        if (mc.currentScreen != null || mc.thePlayer.isDead || !mc.thePlayer.capabilities.allowEdit) return false;
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectType.BLOCK) return false;
        if (this.isKillAura() || mc.thePlayer.isUsingItem()) return false;
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        
        Block block = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
        return block.getBlockHardness(mc.theWorld, mc.objectMouseOver.getBlockPos()) != 0.0F;
    }

    private int findBestHotbarTool(Block block) {
        int currentSlot = Myau.slotComponent.getItemIndex();
        int bestSlot = ItemUtil.findInventorySlot(currentSlot, block);
        return bestSlot == currentSlot ? -1 : bestSlot;
    }

    /**
     * Switch to slot silently via SlotComponent (alternative slot) so the
     * server sees the tool without changing the client's visible currentItem.
     */
    private void selectTool(int slot) {
        if (this.serverSlot == -1) {
            // Remember the real slot before we start spoofing
            this.serverSlot = mc.thePlayer.inventory.currentItem;
        }
        if (this.spoofedToolSlot != slot || Myau.slotComponent.getItemIndex() != slot) {
            // Tell SlotComponent to use this slot for this tick
            Myau.slotComponent.setSlot(slot);
            this.spoofedToolSlot = slot;
        }
    }

    private void resetState() {
        if (this.serverSlot != -1) {
            // Clear alternative slot so the real currentItem is restored
            if (mc.thePlayer != null) {
                IInventoryPlayerAccessor inv = (IInventoryPlayerAccessor) mc.thePlayer.inventory;
                inv.myau$setAlternativeSlot(false);
                inv.myau$setAlternativeCurrentItem(mc.thePlayer.inventory.currentItem);
            }
        }
        this.serverSlot = -1;
        this.spoofedToolSlot = -1;
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (mc.thePlayer == null) return;

        long currentFrame = System.currentTimeMillis();
        float delta = (currentFrame - lastFrame) / 1000f;
        lastFrame = currentFrame;

        boolean shouldShow = this.isEnabled() && this.spoofedToolSlot != -1;

        float target = shouldShow ? 1f : 0f;
        animationProgress += (target - animationProgress) * 12f * delta;
        animationProgress = Math.max(0f, Math.min(1f, animationProgress));

        if (animationProgress <= 0.01f) return;

        ItemStack itemStack = null;
        if (this.spoofedToolSlot != -1) {
            itemStack = mc.thePlayer.inventory.getStackInSlot(this.spoofedToolSlot);
            if (itemStack != null) {
                lastSpoofedStack = itemStack;
            }
        } else {
            itemStack = lastSpoofedStack;
        }

        if (itemStack == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        String toolName = itemStack.getDisplayName();

        float textWidth = myau.util.font.Fonts.MAIN.get(18).width(toolName);
        float width = 16f + 8f + textWidth + 8f;
        float height = 22f;
        float x = (sr.getScaledWidth() - width) / 2f;
        float y = sr.getScaledHeight() - 90f;

        GlStateManager.pushMatrix();

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(animationProgress, animationProgress, 1f);
        GlStateManager.translate(-centerX, -centerY, 0);

        // ── Shader pass (Blur + Bloom) when HUD Shaders option is enabled ──
        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        boolean shaders = hud != null && hud.shaders.getValue();

        if (shaders) {
            int blurP = hud.blurPasses.getValue();
            float blurR = hud.blurRadius.getValue();
            int bloomP = hud.bloomPasses.getValue();
            float bloomR = hud.bloomRadius.getValue();

            // Blur pass — frosted-glass background
            myau.util.shader.RenderSystem.renderBlur(blurR, blurP, () -> {
                RoundedUtils.drawRound(x, y, width, height, 4f, new Color(0, 0, 0, 150));
            });

            // Bloom pass — soft glow outline
            myau.util.shader.RenderSystem.renderBloom(bloomR, bloomP, () -> {
                RoundedUtils.drawRound(x - 1, y - 1, width + 2, height + 2, 4f, new Color(81, 99, 149, 80));
            });
        }
        // ── End shader pass ──────────────────────────────────────────────────

        int bgAlpha = (int) (150 * animationProgress);
        RoundedUtils.drawRound(x, y, width, height, 4f, new Color(0, 0, 0, bgAlpha));

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, (int) x + 4, (int) y + 3);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        GlStateManager.enableBlend();
        int textAlpha = (int) (255 * animationProgress);
        float fontY = y + (height / 2f) - (myau.util.font.Fonts.MAIN.get(18).height() / 2f);
        float textX = x + 24f;

        myau.util.font.Fonts.MAIN.get(18).drawWithShadow(toolName, textX, fontY, new Color(255, 255, 255, textAlpha).getRGB());

        GlStateManager.popMatrix();
    }

    @Override
    public void onDisabled() {
        this.resetState();
        this.lastSpoofedStack = null;
        this.animationProgress = 0f;
    }
}