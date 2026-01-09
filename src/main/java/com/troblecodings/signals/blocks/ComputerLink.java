package com.troblecodings.signals.blocks;

import java.util.Optional;

import com.troblecodings.signals.core.TileEntitySupplierWrapper;
import com.troblecodings.signals.tileentitys.ComputerLinkEntity;
import com.troblecodings.signals.OpenSignalsMain;

import com.troblecodings.signals.init.OSItems;

import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ComputerLink extends BasicBlock {

    public ComputerLink() {
        super(Material.ROCK);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new ComputerLinkEntity();
    }

    @Override
    public Optional<TileEntitySupplierWrapper> getSupplierWrapper() {
        return Optional.of(ComputerLinkEntity::new);
    }

    @Override
    public Optional<String> getSupplierWrapperName() {
        return Optional.of("computerrequest");
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote)
            return true;
        final Item item = playerIn.getHeldItemMainhand().getItem();
        if (item.equals(OSItems.LINKING_TOOL) || item.equals(OSItems.MULTI_LINKING_TOOL))
            return false;
        OpenSignalsMain.handler.invokeGui(ComputerLink.class, playerIn, worldIn, pos, "computerlink");
        return true;
    }
}
