package dev._3000IQPlay.experium.features.modules.exploit;

import dev._3000IQPlay.experium.Experium;
import dev._3000IQPlay.experium.features.modules.Module;
import dev._3000IQPlay.experium.features.setting.Bind;
import dev._3000IQPlay.experium.features.setting.Setting;
import dev._3000IQPlay.experium.event.events.BlockEvent;
import dev._3000IQPlay.experium.event.events.Render3DEvent;
import dev._3000IQPlay.experium.util.InventoryUtil;
import dev._3000IQPlay.experium.util.RenderUtil;
import dev._3000IQPlay.experium.util.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class Speedmine
        extends Module {
    public final Setting<SwitchMode> autoSwitch = this.register(new Setting<SwitchMode>("Auto Switch", SwitchMode.SILENT));
    public final Setting<InstantMode> instantMode = this.register(new Setting<InstantMode>("Instant Mode", InstantMode.NONE));
    public final Setting<Bind> instantBind = this.register(new Setting<Bind>("Instant Bind", new Bind(-1)));
    public final Setting<Integer> instantDelay = this.register(new Setting<Integer>("Instant Delay", 1, 0, 50));
    public final Setting<Boolean> swing = this.register(new Setting<Boolean>("Swing", Boolean.valueOf(true)));
    public final Setting<Boolean> render = this.register(new Setting<Boolean>("Render", Boolean.valueOf(true)));
    public final Setting<Boolean> statusColor = this.register(new Setting<Boolean>("Status Color", Boolean.valueOf(false)));
    public final Setting<RenderMode> renderMode = this.register(new Setting<RenderMode>("Render Mode", RenderMode.RISE));
    public final Setting<ColorMode> statusMode = this.register(new Setting<ColorMode>("Status Mode", ColorMode.CUSTOM));
    public final Setting<Integer> red = this.register(new Setting<Integer>("Red", 135, 0, 255));
    public final Setting<Integer> green = this.register(new Setting<Integer>("Green", 0, 0, 255));
    public final Setting<Integer> blue = this.register(new Setting<Integer>("Blue", 255, 0, 255));
    public final Setting<Integer> alpha = this.register(new Setting<Integer>("Alpha", 150, 0, 255));
    public Timer timer = new Timer();
    public Timer instantTimer = new Timer();
    public Block block = null;
    public BlockPos breakPos = null;
    public EnumFacing enumFacing = null;
    public static float damage;

    public Speedmine() {
        super("Speedmine", "Allows you to mine blocks faster than normal", Module.Category.EXPLOIT, true, false, false);
    }

    @SubscribeEvent
    public void onBlockEvent(BlockEvent event) {
        if (Speedmine.fullNullCheck()) {
            return;
        }
        if (Speedmine.canBlockBeBroken(event.pos)) {
            this.breakPos = event.pos;
            this.enumFacing = event.facing;
            if (this.swing.getValue().booleanValue()) {
                Speedmine.mc.player.swingArm(EnumHand.MAIN_HAND);
            }
            this.timer.reset();
            this.instantTimer.reset();
            Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, this.breakPos, this.enumFacing));
            Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
        }
    }

    @Override
    public void onDisable() {
        this.breakPos = null;
        this.enumFacing = null;
    }

    @Override
    public void onLogout() {
        this.breakPos = null;
        this.enumFacing = null;
    }

    @Override
    public void onUpdate() {
        if (this.breakPos != null) {
            int oldSlot = Speedmine.mc.player.inventory.currentItem;
            try {
                this.block = Speedmine.mc.world.getBlockState(this.breakPos).getBlock();
            }
            catch (Exception exception) {
                // empty catch block
            }
            int toolSlot = this.getBestAvailableToolSlot(this.block.getBlockState().getBaseState());
            float breakTime = Speedmine.mc.world.getBlockState(this.breakPos).getBlockHardness((World)Speedmine.mc.world, this.breakPos);
            if (Speedmine.mc.world.getBlockState(this.breakPos).getBlock() == Blocks.AIR && this.instantMode.getValue() != InstantMode.NORMAL) {
                if (this.instantMode.getValue() == InstantMode.BIND && !this.instantBind.getValue().isDown()) {
                    this.breakPos = null;
                    this.enumFacing = null;
                    return;
                }
                if (this.instantMode.getValue() == InstantMode.NONE) {
                    this.breakPos = null;
                    this.enumFacing = null;
                    return;
                }
            } else if (this.instantMode.getValue() == InstantMode.NORMAL) {
                // empty if block
            }
            if (this.timer.passed((long)breakTime)) {
                if (this.autoSwitch.getValue() == SwitchMode.SILENT && Speedmine.mc.player.inventory.currentItem != toolSlot && toolSlot != -1) {
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketHeldItemChange(toolSlot));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.breakPos, this.enumFacing.UP));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketHeldItemChange(oldSlot));
                }
				if (this.autoSwitch.getValue() == SwitchMode.NORMAL && Speedmine.mc.player.inventory.currentItem != toolSlot && toolSlot != -1) {
                    InventoryUtil.switchToHotbarSlot(toolSlot, false);
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.breakPos, this.enumFacing.UP));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
                    InventoryUtil.switchToHotbarSlot(oldSlot, false);
                }
				if (this.autoSwitch.getValue() == SwitchMode.STI && Speedmine.mc.player.inventory.currentItem != toolSlot && toolSlot != -1) {
                    InventoryUtil.switchToHotbarSlot(toolSlot, true);
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.breakPos, this.enumFacing.UP));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.breakPos, this.enumFacing));
                    Speedmine.mc.player.connection.sendPacket((Packet)new CPacketHeldItemChange(oldSlot));
                }
				return;
            }
        }
    }

    @Override
    @SubscribeEvent
    public void onRender3D(Render3DEvent event) {
        if (Speedmine.mc.player != null && Speedmine.mc.world != null && this.render.getValue().booleanValue() && this.breakPos != null && Speedmine.mc.world.getBlockState(this.breakPos).getBlock() != Blocks.AIR) {
            Color renderColors = new Color(this.red.getValue(), this.green.getValue(), this.blue.getValue(), this.alpha.getValue());
            AxisAlignedBB bb = new AxisAlignedBB((double)this.breakPos.getX() - Speedmine.mc.getRenderManager().viewerPosX, (double)this.breakPos.getY() - Speedmine.mc.getRenderManager().viewerPosY, (double)this.breakPos.getZ() - Speedmine.mc.getRenderManager().viewerPosZ, (double)(this.breakPos.getX() + 1) - Speedmine.mc.getRenderManager().viewerPosX, (double)(this.breakPos.getY() + 1) - Speedmine.mc.getRenderManager().viewerPosY, (double)(this.breakPos.getZ() + 1) - Speedmine.mc.getRenderManager().viewerPosZ);
            float breakTime = Speedmine.mc.world.getBlockState(this.breakPos).getBlockHardness((World)Speedmine.mc.world, this.breakPos);
            double progression = (float)this.timer.getPassedTimeMs() / 40.0f / breakTime * Experium.serverManager.getTpsFactor();
            double centerX = bb.minX + (bb.maxX - bb.minX) / Double.longBitsToDouble(Double.doubleToLongBits(0.4677600299948414) ^ 0x7FDDEFC7C3CD0B6FL);
            double increaseX = progression * ((bb.maxX - centerX) / Double.longBitsToDouble(Double.doubleToLongBits(0.13526251945472825) ^ 0x7FE5504840B76025L));
            double centerY = bb.minY + (bb.maxY - bb.minY) / Double.longBitsToDouble(Double.doubleToLongBits(0.2265017733140163) ^ 0x7FCCFE02966F5283L);
            double increaseY = progression * ((bb.maxY - centerY) / Double.longBitsToDouble(Double.doubleToLongBits(0.1390228780814261) ^ 0x7FE5CB806D60B4E4L));
            double centerZ = bb.minZ + (bb.maxZ - bb.minZ) / Double.longBitsToDouble(Double.doubleToLongBits(0.7187776739735182) ^ 0x7FE7003A0959F571L);
            double increaseZ = progression * ((bb.maxZ - centerZ) / Double.longBitsToDouble(Double.doubleToLongBits(0.2497058873358051) ^ 0x7FEBF65CCDDCEBB7L));
            float shrinkFactor = MathHelper.clamp((float)((float)increaseX), (float)Float.intBitsToFloat(Float.floatToIntBits(-14.816234f) ^ 0x7EED0F4B), (float)Float.intBitsToFloat(Float.floatToIntBits(9.43287f) ^ 0x7E96ED09));
            AxisAlignedBB axisAlignedBB1 = new AxisAlignedBB(centerX - increaseX, centerY - increaseY, centerZ - increaseZ, centerX + increaseX, centerY + increaseY, centerZ + increaseZ);
            double oldMaxY = bb.maxY;
            double upY = progression * (bb.maxY - bb.minY);
            AxisAlignedBB riseBB = new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + upY, bb.maxZ);
            float sGreen = MathHelper.clamp((float)((float)upY), (float)0.0f, (float)1.0f);
            Color color = this.statusMode.getValue().equals((Object)ColorMode.STATIC) ? new Color(this.timer.hasReached((long)breakTime) ? 0 : 255, this.timer.hasReached((long)breakTime) ? 255 : 0, 0, this.alpha.getValue()) : new Color(255 - (int)(sGreen * 150.0f), (int)(sGreen * 255.0f), 0, this.alpha.getValue());
            float fadeA = MathHelper.clamp((float)((float)upY), (float)0.0f, (float)1.0f);
            Color alphaFade = new Color(renderColors.getRed(), renderColors.getGreen(), renderColors.getBlue(), (int)(fadeA * 255.0f));
            if (this.renderMode.getValue().equals((Object)RenderMode.RISE)) {
                if (riseBB.maxY <= oldMaxY) {
                    RenderUtil.drawFilledBox(riseBB, this.statusColor.getValue() != false ? color.getRGB() : renderColors.getRGB());
                    RenderUtil.drawBlockOutline(riseBB, this.statusColor.getValue() != false ? color : renderColors, 1.0f);
                }
                if (riseBB.maxY >= oldMaxY) {
                    RenderUtil.drawFilledBox(bb, this.statusColor.getValue() != false ? color.getRGB() : renderColors.getRGB());
                    RenderUtil.drawBlockOutline(bb, this.statusColor.getValue() != false ? color : renderColors, 1.0f);
                }
            } else if (this.renderMode.getValue().equals((Object)RenderMode.GROW)) {
                if (axisAlignedBB1.maxY <= oldMaxY) {
                    RenderUtil.drawFilledBox(axisAlignedBB1, this.statusColor.getValue() != false ? color.getRGB() : renderColors.getRGB());
                    RenderUtil.drawBlockOutline(axisAlignedBB1, this.statusColor.getValue() != false ? color : renderColors, Float.intBitsToFloat(Float.floatToIntBits(7.8420115f) ^ 0x7F7AF1C2));
                }
                if (axisAlignedBB1.maxY >= oldMaxY) {
                    RenderUtil.drawFilledBox(bb, this.statusColor.getValue() != false ? color.getRGB() : renderColors.getRGB());
                    RenderUtil.drawBlockOutline(bb, this.statusColor.getValue() != false ? color : renderColors, Float.intBitsToFloat(Float.floatToIntBits(117.64367f) ^ 0x7D6B498F));
                }
            } else if (this.renderMode.getValue().equals((Object)RenderMode.STATIC)) {
                RenderUtil.drawFilledBox(bb, this.statusColor.getValue() != false ? color.getRGB() : renderColors.getRGB());
                RenderUtil.drawBlockOutline(bb, this.statusColor.getValue() != false ? color : renderColors, 1.0f);
            } else if (this.renderMode.getValue().equals((Object)RenderMode.FADE)) {
                RenderUtil.drawFilledBox(bb, this.statusColor.getValue() != false ? new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(fadeA * 255.0f)).getRGB() : alphaFade.getRGB());
                RenderUtil.drawBlockOutline(bb, this.statusColor.getValue() != false ? new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(fadeA * 255.0f)) : alphaFade, 1.0f);
            } else if (this.renderMode.getValue().equals((Object)RenderMode.SHRINK)) {
                RenderUtil.drawFilledBox(RenderUtil.interpolateAxis(new AxisAlignedBB(this.breakPos)).shrink((double)shrinkFactor), this.statusColor.getValue() != false ? color.getRGB() : renderColors.getRGB());
                RenderUtil.drawBlockOutline(RenderUtil.interpolateAxis(new AxisAlignedBB(this.breakPos)).shrink((double)shrinkFactor), this.statusColor.getValue() != false ? color : renderColors, Float.intBitsToFloat(Float.floatToIntBits(119.3883f) ^ 0x7D6EC6CF));
            }
        }
    }

    public int getBestAvailableToolSlot(IBlockState blockState) {
        int toolSlot = -1;
        for (int i = 0; i < 9; ++i) {
			if (Speedmine.mc.player.inventory.getStackInSlot(i).getItem() != Items.DIAMOND_PICKAXE) continue;
            toolSlot = i;
        }
        return toolSlot;
    }

    public static boolean canBlockBeBroken(BlockPos pos) {
        IBlockState blockState = Speedmine.mc.world.getBlockState(pos);
        Block block = blockState.getBlock();
        return block.getBlockHardness(blockState, (World)Speedmine.mc.world, pos) != -1.0f;
    }

    public static enum InstantMode {
        NONE,
        NORMAL,
        BIND;
    }

    public static enum RenderMode {
        RISE,
        FADE,
        STATIC,
        SHRINK,
        GROW;
    }

    public static enum SwitchMode {
        SILENT,
		NORMAL,
		STI,
        NONE;
    }

    public static enum ColorMode {
        STATIC,
        CUSTOM;
    }
}
