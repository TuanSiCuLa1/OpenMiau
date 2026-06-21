package myau.module.modules.render;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.util.render.RenderUtil;
import myau.property.properties.*;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
public class BedESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final CopyOnWriteArraySet<BlockPos> beds = new CopyOnWriteArraySet<>();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "FULL"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"CUSTOM", "HUD"});
    public final ColorProperty customColor;
    public final PercentProperty opacity;
    public final BooleanProperty outline;
    public final BooleanProperty obsidian;
    public final BooleanProperty analyzer;
    public final ModeProperty analyzerMode;
    public final BooleanProperty analyzerCounts;
    private Color getColor() {
        switch (this.color.getValue()) {
            case 0:
                return new Color(this.customColor.getValue());
            case 1:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    private void drawObsidianBox(AxisAlignedBB axisAlignedBB) {
        if (this.outline.getValue()) {
            RenderUtil.drawBoundingBox(axisAlignedBB, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawFilledBox(axisAlignedBB, 170, 0, 170);
    }

    private void drawObsidian(BlockPos blockPos) {
        if (this.outline.getValue()) {
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawBlockBox(
                blockPos, 1.0, 170, 0, 170
        );
    }

    public BedESP() {
        super("BedESP", false);
        this.customColor = new ColorProperty("custom-color", (int) 8085714755840333141L, () -> this.color.getValue() == 0);
        this.opacity = new PercentProperty("opacity", 25);
        this.outline = new BooleanProperty("outline", false);
        this.obsidian = new BooleanProperty("obsidian", true);
        this.analyzer = new BooleanProperty("analyzer", true);
        this.analyzerMode = new ModeProperty("analyzer-mode", 0, new String[]{"TOOLS", "BLOCKS"});
        this.analyzerCounts = new BooleanProperty("analyzer-counts", true);
    }

    public double getHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            RenderUtil.enableRenderState();
            for (BlockPos blockPos : this.beds) {
                IBlockState state = mc.theWorld.getBlockState(blockPos);
                if (state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                    BlockPos opposite = blockPos.offset(state.getValue(BlockBed.FACING).getOpposite());
                    IBlockState oppositeState = mc.theWorld.getBlockState(opposite);
                    if (oppositeState.getBlock() instanceof BlockBed && oppositeState.getValue(BlockBed.PART) == EnumPartType.FOOT) {
                        if (this.obsidian.getValue()) {
                            for (EnumFacing facing : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                                BlockPos offsetX = blockPos.offset(facing);
                                BlockPos offsetZ = opposite.offset(facing);
                                boolean xObsidian = mc.theWorld.getBlockState(offsetX).getBlock() instanceof BlockObsidian;
                                boolean zObsidian = mc.theWorld.getBlockState(offsetZ).getBlock() instanceof BlockObsidian;
                                if (xObsidian && zObsidian) {
                                    this.drawObsidianBox(
                                            new AxisAlignedBB(
                                                    Math.min(offsetX.getX(), offsetZ.getX()),
                                                    offsetX.getY(),
                                                    Math.min(offsetX.getZ(), offsetZ.getZ()),
                                                    Math.max((double) offsetX.getX() + 1.0, (double) offsetZ.getX() + 1.0),
                                                    (double) offsetX.getY() + 1.0,
                                                    Math.max((double) offsetX.getZ() + 1.0, (double) offsetZ.getZ() + 1.0)
                                            )
                                                    .offset(
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                                                    )
                                    );
                                } else if (xObsidian) {
                                    this.drawObsidian(offsetX);
                                } else if (zObsidian) {
                                    this.drawObsidian(offsetZ);
                                }
                            }
                        }
                        AxisAlignedBB aabb = new AxisAlignedBB(
                                Math.min(blockPos.getX(), opposite.getX()),
                                blockPos.getY(),
                                Math.min(blockPos.getZ(), opposite.getZ()),
                                Math.max((double) blockPos.getX() + 1.0, (double) opposite.getX() + 1.0),
                                (double) blockPos.getY() + this.getHeight(),
                                Math.max((double) blockPos.getZ() + 1.0, (double) opposite.getZ() + 1.0)
                        )
                                .offset(
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                                );
                        Color color = this.getColor();
                        if (this.outline.getValue()) {
                            RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), 255, 1.5F);
                        }
                        RenderUtil.drawFilledBox(
                                aabb,
                                color.getRed(),
                                color.getGreen(),
                                color.getBlue()
                        );
                    }
                } else {
                    this.beds.remove(blockPos);
                }
            }

            if (this.analyzer.getValue()) {
                for (BlockPos blockPos : this.beds) {
                    IBlockState state = mc.theWorld.getBlockState(blockPos);
                    if (state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                        analyzeAndRenderBed(blockPos);
                    }
                }
            }

            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onEnabled() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    private void analyzeAndRenderBed(BlockPos bedHead) {
        BlockPos bedFoot = bedHead;
        IBlockState headState = mc.theWorld.getBlockState(bedHead);
        if (headState.getBlock() instanceof BlockBed && headState.getValue(BlockBed.PART) == EnumPartType.HEAD) {
            bedFoot = bedHead.offset(headState.getValue(BlockBed.FACING).getOpposite());
        }

        Map<Integer, Map<Block, Integer>> layers = new HashMap<>();
        for (int i = 1; i <= 5; i++) layers.put(i, new HashMap<>());

        int maxDist = 5;
        int minX = Math.min(bedHead.getX(), bedFoot.getX()) - maxDist;
        int maxX = Math.max(bedHead.getX(), bedFoot.getX()) + maxDist;
        int minY = Math.min(bedHead.getY(), bedFoot.getY()) - maxDist;
        int maxY = Math.max(bedHead.getY(), bedFoot.getY()) + maxDist;
        int minZ = Math.min(bedHead.getZ(), bedFoot.getZ()) - maxDist;
        int maxZ = Math.max(bedHead.getZ(), bedFoot.getZ()) + maxDist;

        Map<Integer, Integer> layerTotalBlocks = new HashMap<>();
        Map<Integer, Integer> layerAirBlocks = new HashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    
                    int dx1 = Math.abs(p.getX() - bedHead.getX());
                    int dy1 = Math.abs(p.getY() - bedHead.getY());
                    int dz1 = Math.abs(p.getZ() - bedHead.getZ());
                    int dist1 = Math.max(dx1, Math.max(dy1, dz1));

                    int dx2 = Math.abs(p.getX() - bedFoot.getX());
                    int dy2 = Math.abs(p.getY() - bedFoot.getY());
                    int dz2 = Math.abs(p.getZ() - bedFoot.getZ());
                    int dist2 = Math.max(dx2, Math.max(dy2, dz2));

                    int dist = Math.min(dist1, dist2);

                    if (dist >= 1 && dist <= maxDist) {
                        layerTotalBlocks.put(dist, layerTotalBlocks.getOrDefault(dist, 0) + 1);
                        Block block = mc.theWorld.getBlockState(p).getBlock();
                        if (block == Blocks.air || block == Blocks.water || block == Blocks.lava || block == Blocks.flowing_water || block == Blocks.flowing_lava) {
                            layerAirBlocks.put(dist, layerAirBlocks.getOrDefault(dist, 0) + 1);
                        } else if (block != Blocks.bed) {
                            Map<Block, Integer> layerCounts = layers.get(dist);
                            layerCounts.put(block, layerCounts.getOrDefault(block, 0) + 1);
                        }
                    }
                }
            }
        }

        Map<Block, Integer> finalCounts = new HashMap<>();
        int sparseLayerCount = 0;
        for (int i = 1; i <= 5; i++) {
            int total = layerTotalBlocks.getOrDefault(i, 0);
            int air = layerAirBlocks.getOrDefault(i, 0);
            if (total == 0 || (float) air / total > 0.2F) {
                if (++sparseLayerCount >= 2) break;
                continue;
            }
            sparseLayerCount = 0;
            Map<Block, Integer> layerCounts = layers.get(i);
            for (Map.Entry<Block, Integer> entry : layerCounts.entrySet()) {
                if ((float) entry.getValue() / total >= 0.2F) {
                    finalCounts.put(entry.getKey(), finalCounts.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            }
        }

        if (finalCounts.isEmpty()) return;

        List<DefenseEntry> displayEntries = new ArrayList<>();

        if (this.analyzerMode.getValue() == 0) {
            java.util.Set<ToolOverlayType> toolTypes = new java.util.HashSet<>();
            for (Block b : finalCounts.keySet()) {
                ToolOverlayType toolType = ToolOverlayType.fromBlock(b);
                if (toolType != null) toolTypes.add(toolType);
            }
            if (toolTypes.contains(ToolOverlayType.DIAMOND_PICKAXE)) {
                toolTypes.remove(ToolOverlayType.IRON_PICKAXE);
            }
            for (ToolOverlayType t : toolTypes) {
                displayEntries.add(new DefenseEntry(t.renderStack.copy(), 1));
            }
            displayEntries.sort((a, b) -> a.stack.getDisplayName().compareToIgnoreCase(b.stack.getDisplayName()));
        } else {
            for (Map.Entry<Block, Integer> entry : finalCounts.entrySet()) {
                ItemStack blockStack = new ItemStack(entry.getKey());
                if (blockStack.getItem() == null) {
                    blockStack = new ItemStack(Items.iron_pickaxe);
                }
                displayEntries.add(new DefenseEntry(blockStack, entry.getValue()));
            }
            displayEntries.sort((a, b) -> Integer.compare(b.count, a.count));
        }

        if (!displayEntries.isEmpty()) {
            renderToolNametag(bedHead, bedFoot, displayEntries);
        }
    }

    private void renderToolNametag(BlockPos head, BlockPos foot, List<DefenseEntry> entries) {
        double centerX = (head.getX() + foot.getX()) / 2.0 + 0.5 - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double centerY = Math.max(head.getY(), foot.getY()) + 1.5 - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double centerZ = (head.getZ() + foot.getZ()) / 2.0 + 0.5 - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(centerX, centerY, centerZ);
        
        net.minecraft.client.renderer.entity.RenderManager renderManager = mc.getRenderManager();
        net.minecraft.client.renderer.GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        net.minecraft.client.renderer.GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        
        float scale = 0.035f;
        net.minecraft.client.renderer.GlStateManager.scale(-scale, -scale, scale);
        
        net.minecraft.client.renderer.GlStateManager.disableLighting();
        net.minecraft.client.renderer.GlStateManager.depthMask(false);
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float padding = 4f;
        float itemSize = 16f;
        float spacing = 18f;
        float width = padding * 2 + (entries.size() * spacing) - (spacing - itemSize);
        float height = padding * 2 + itemSize;
        float rectX = -width / 2f;
        float rectY = -height / 2f;

        RenderUtil.drawRoundedRectangle(rectX, rectY, rectX + width, rectY + height, 4f, new Color(0, 0, 0, 150).getRGB());

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        
        float currentX = rectX + padding;
        float currentY = rectY + padding;
        for (DefenseEntry entry : entries) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(entry.stack, (int) currentX, (int) currentY);
            
            if (this.analyzerMode.getValue() == 1 && this.analyzerCounts.getValue() && entry.count > 1) {
                net.minecraft.client.renderer.GlStateManager.pushMatrix();
                net.minecraft.client.renderer.GlStateManager.translate(0, 0, 100);
                String countText = String.valueOf(entry.count);
                mc.fontRendererObj.drawStringWithShadow(countText, currentX + 17 - mc.fontRendererObj.getStringWidth(countText), currentY + 9, 0xFFFFFF);
                net.minecraft.client.renderer.GlStateManager.popMatrix();
            }
            
            currentX += spacing;
        }
        
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        net.minecraft.client.renderer.GlStateManager.popMatrix();

        net.minecraft.client.renderer.GlStateManager.enableDepth();
        net.minecraft.client.renderer.GlStateManager.depthMask(true);
        net.minecraft.client.renderer.GlStateManager.enableLighting();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    private static class DefenseEntry {
        ItemStack stack;
        int count;
        DefenseEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    private enum ToolOverlayType {
        DIAMOND_PICKAXE(Items.diamond_pickaxe),
        IRON_AXE(Items.iron_axe),
        IRON_HOE(Items.iron_hoe),
        IRON_PICKAXE(Items.iron_pickaxe),
        SHEARS(Items.shears),
        IRON_SHOVEL(Items.iron_shovel),
        IRON_SWORD(Items.iron_sword);

        private final net.minecraft.item.Item item;
        private final ItemStack renderStack;

        ToolOverlayType(net.minecraft.item.Item item) {
            this.item = item;
            this.renderStack = new ItemStack(item);
        }

        private static ToolOverlayType fromBlock(Block block) {
            if (block == Blocks.obsidian) return DIAMOND_PICKAXE;

            ToolOverlayType bestTool = IRON_PICKAXE;
            float bestEfficiency = 1.0F;
            for (ToolOverlayType toolType : values()) {
                if (toolType == DIAMOND_PICKAXE) continue;
                float efficiency = toolType.renderStack.getStrVsBlock(block);
                if (efficiency > bestEfficiency) {
                    bestEfficiency = efficiency;
                    bestTool = toolType;
                }
            }
            return bestTool;
        }
    }
}
