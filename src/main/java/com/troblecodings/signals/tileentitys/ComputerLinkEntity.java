package com.troblecodings.signals.tileentitys;

import java.util.ArrayList;
import java.util.List;

import com.troblecodings.core.NBTWrapper;
import com.troblecodings.linkableapi.ILinkableTile;
import com.troblecodings.signals.blocks.Signal;
import com.troblecodings.signals.cctweaked.ComputerLinkPeripheral;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.BlockPos;

public class ComputerLinkEntity extends SyncableTileEntity
        implements ILinkableTile, dan200.computercraft.api.peripheral.IPeripheralTile {

    private final List<BlockPos> linkedSignals = new ArrayList<>();

    @Override
    public void loadWrapper(NBTWrapper wrapper) {
        this.linkedSignals.clear();
        if (wrapper.tag.hasKey("linkedSignals")) {
            final NBTTagCompound linkedSignalsTag = wrapper.tag.getCompoundTag("linkedSignals");

            for (final String key : linkedSignalsTag.getKeySet()) {
                final NBTTagCompound posTag = linkedSignalsTag.getCompoundTag(key);
                final NBTWrapper posWrapper = new NBTWrapper(posTag);
                this.linkedSignals.add(posWrapper.getBlockPos("pos"));
            }
        }
    }

    @Override
    public void saveWrapper(NBTWrapper wrapper) {
        final NBTTagCompound linkedSignalsTag = new NBTTagCompound();
        for (int i = 0; i < this.linkedSignals.size(); i++) {
            final NBTWrapper posWrapper = new NBTWrapper();
            posWrapper.putBlockPos("pos", this.linkedSignals.get(i));
            linkedSignalsTag.setTag(String.valueOf(i), posWrapper.tag);
        }
        wrapper.tag.setTag("linkedSignals", linkedSignalsTag);

    }

    @Override
    public boolean hasLink() {
        return !linkedSignals.isEmpty();
    }

    @Override
    public boolean link(BlockPos pos, NBTTagCompound tag) {
        if (pos == null || !(this.world.getBlockState(pos).getBlock() instanceof Signal))
            return false;
        this.linkedSignals.add(pos);

        this.syncClient();
        return true;
    }

    @Override
    public boolean unlink() {
        linkedSignals.clear();
        this.syncClient();
        return true;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 1, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        this.saveWrapper(new NBTWrapper(tag));
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    public List<BlockPos> getLinkedSignals() {

        return this.linkedSignals;
    }

    private final ComputerLinkPeripheral peripheral = new ComputerLinkPeripheral(this);

    @Override
    public dan200.computercraft.api.peripheral.IPeripheral getPeripheral(net.minecraft.util.EnumFacing side) {
        return peripheral;
    }

    public List<BlockPos> getSignals() {
        return this.linkedSignals;
    }
}
