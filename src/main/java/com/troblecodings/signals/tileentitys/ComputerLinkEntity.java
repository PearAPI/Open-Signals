package com.troblecodings.signals.tileentitys;

import java.util.ArrayList;
import java.util.List;

import com.troblecodings.core.NBTWrapper;
import com.troblecodings.linkableapi.ILinkableTile;
import com.troblecodings.signals.blocks.CombinedRedstoneInput;
import com.troblecodings.signals.blocks.RedstoneIO;
import com.troblecodings.signals.blocks.RedstoneInput;
import com.troblecodings.signals.blocks.Signal;
import com.troblecodings.signals.cctweaked.ComputerLinkPeripheral;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class ComputerLinkEntity extends SyncableTileEntity
        implements ILinkableTile, IPeripheralTile {

    private List<BlockPos> linkedBlocks = new ArrayList<>();
    private List<BlockPos> linkedSignals = new ArrayList<>();
    private List<BlockPos> linkedRedstoneInputs = new ArrayList<>();
    private List<BlockPos> linkedRedstoneOutputs = new ArrayList<>();

    // linkedBlock:[{pos:{X: 0, Y:0, Z: 0}},{pos:{X: 0, Y:0, Z: 0}}]
    @Override
    public void loadWrapper(NBTWrapper wrapper) {
        this.linkedBlocks = new ArrayList<>();
        if (wrapper.tag.hasKey("linkedBlocks")) {
            List<NBTWrapper> list = wrapper.getList("linkedBlocks");

            for (int i = 0; i < list.size(); i++) {
                NBTWrapper tag = list.get(i);
                linkedBlocks.add(tag.getBlockPos("pos"));
                System.out.println(tag.getBlockPos("pos"));
            }
        }
    }

    @Override
    public void saveWrapper(NBTWrapper wrapper) {
        List<NBTWrapper> tag = new ArrayList<>();

        linkedBlocks.forEach(pos -> {
            NBTWrapper posTag = new NBTWrapper();
            posTag.putBlockPos("pos", pos);
            tag.add(posTag);
        });

        wrapper.putList("linkedBlocks", tag);
    }

    @Override
    public boolean hasLink() {
        return !linkedBlocks.isEmpty();
    }

    @Override
    public boolean link(BlockPos pos, NBTTagCompound tag) {
        Block block = this.world.getBlockState(pos).getBlock();
        if (pos == null)
            return false;

        if (!(block instanceof Signal || block instanceof RedstoneIO) || (block instanceof CombinedRedstoneInput)) {
            return false;
        }

        this.linkedBlocks.add(pos);

        this.updateCache();

        this.syncClient();
        return true;
    }

    @Override
    public boolean unlink() {
        linkedBlocks.clear();
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

    public List<BlockPos> getLinkedBlocks() {

        return this.linkedBlocks;
    }

    private final ComputerLinkPeripheral peripheral = new ComputerLinkPeripheral(this);

    @Override
    public IPeripheral getPeripheral(EnumFacing side) {
        return peripheral;
    }

    boolean isCacheValid() {
        int size = linkedSignals.size() + linkedRedstoneInputs.size() + linkedRedstoneOutputs.size();

        if (size != linkedBlocks.size())
            return false;

        return true;
    }

    void updateCache() {
        this.linkedSignals.clear();
        this.linkedRedstoneInputs.clear();
        this.linkedRedstoneOutputs.clear();

        this.linkedBlocks.forEach(pos -> {
            Block block = this.world.getBlockState(pos).getBlock();

            if (block instanceof Signal) {
                this.linkedSignals.add(pos);
            } else if (block instanceof RedstoneInput) {
                this.linkedRedstoneInputs.add(pos);
            } else if (block instanceof RedstoneIO) {
                this.linkedRedstoneOutputs.add(pos);
            }
        });
    }

    public List<BlockPos> getBlocks() {
        if (!isCacheValid())
            updateCache();
        return this.linkedBlocks;
    }

    public List<BlockPos> getSignals() {
        if (!isCacheValid())
            updateCache();
        return this.linkedSignals;
    }

    public List<BlockPos> getRedstoneInputs() {
        if (!isCacheValid())
            updateCache();
        return this.linkedRedstoneInputs;
    }

    public List<BlockPos> getRedstoneOutputs() {
        if (!isCacheValid())
            updateCache();
        return this.linkedRedstoneOutputs;
    }
}
