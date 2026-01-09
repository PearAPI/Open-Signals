package com.troblecodings.signals.cctweaked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.troblecodings.signals.SEProperty;
import com.troblecodings.signals.blocks.Signal;
import com.troblecodings.signals.core.StateInfo;
import com.troblecodings.signals.enums.ChangeableStage;
import com.troblecodings.signals.handler.SignalStateHandler;
import com.troblecodings.signals.handler.SignalStateInfo;
import com.troblecodings.signals.tileentitys.ComputerLinkEntity;
import com.troblecodings.signals.tileentitys.SignalTileEntity;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class ComputerLinkPeripheral extends PeripheralBase<ComputerLinkEntity> {
    private Map<BlockPos, Map<String, SEProperty>> signalProperties = new HashMap<>();

    public ComputerLinkPeripheral(final ComputerLinkEntity entity) {
        super(entity);
    }

    List<SignalTileEntity> getLinkedSignals() {
        List<SignalTileEntity> linkedSignals = new ArrayList<>();
        entity.getSignals().forEach(pos -> {
            TileEntity tileEntity = entity.getWorld().getTileEntity(pos);
            if (tileEntity instanceof SignalTileEntity) {
                linkedSignals.add((SignalTileEntity) tileEntity);
            }
        });
        return linkedSignals;
    }

    TileEntity getTileEntity(BlockPos pos) {
        return this.entity.getWorld().getTileEntity(pos);
    }

    Block getBlock(BlockPos pos) {
        return this.entity.getWorld().getBlockState(pos).getBlock();
    }

    void updateSignalProperties() {
        entity.getSignals().forEach(pos -> {
            TileEntity tileEntity = entity.getWorld().getTileEntity(pos);
            if (tileEntity instanceof SignalTileEntity) {
                Map<String, SEProperty> properties = new HashMap<>();

                getApplicableProperties(((SignalTileEntity) tileEntity).getProperties()).forEach(property -> {
                    properties.put(property.getName(), property);
                });

                signalProperties.put(pos, properties);
            }
        });
    }

    public static List<SEProperty> getApplicableProperties(Map<SEProperty, String> currentProps) {
        List<SEProperty> validProperties = new ArrayList<>();
        currentProps.forEach((property, value) -> {
            if ((property.isChangabelAtStage(ChangeableStage.APISTAGE)
                    || property.isChangabelAtStage(ChangeableStage.APISTAGE_NONE_CONFIG))
                    && property.testMap(currentProps)) {
                validProperties.add(property);
            }

            // OpenSignalsMain.getLogger().info("***********" + property.getName() + " " +
            // property.testMap(currentProps));
        });
        return validProperties;
    }

    /**
     * @return returns true if there is at least one linked signal
     */
    @LuaMethod
    public final boolean isLinked() {
        return getLinkedSignals().size() > 0;
    }

    /**
     * 
     * @return returns the linked signals as a table
     *         e.g {{posX, posY, posZ}, ...}
     */
    @LuaMethod
    public final Object getSignals() {
        List<SignalTileEntity> linkedSignals = this.getLinkedSignals();
        List<Object[]> obj = new ArrayList<>();

        updateSignalProperties();

        for (int i = 0; i < linkedSignals.size(); i++) {
            Object[] signal = new Object[3];
            signal[0] = linkedSignals.get(i).getPos().getX();
            signal[1] = linkedSignals.get(i).getPos().getY();
            signal[2] = linkedSignals.get(i).getPos().getZ();
            obj.add(signal);
        }
        return obj;
    }

    /**
     * 
     * @param args[0] index of the signal in the getSignals() table
     * @return returns the supported signal states as a table
     *         e.g {"signalState1", "signalState2", ...}
     */
    @LuaMethod(arguments = { Object.class })
    public final Object getSupportedSignalStates(int index) {
        updateSignalProperties();

        List<SignalTileEntity> linkedSignals = this.getLinkedSignals();

        if (index < 0 || index >= linkedSignals.size())
            return "Invalid index";

        List<SEProperty> properties = getApplicableProperties(linkedSignals.get(index).getProperties());

        List<String> propertyNames = properties.stream().map(SEProperty::getName).collect(Collectors.toList());

        return propertyNames;
    }

    /**
     * 
     * @param args[0] index of the signal in the getSignals() table
     * @return returns the current signal state as a string
     *         e.g "signalState1"
     */
    @LuaMethod(arguments = { int.class, String.class })
    public final Object getSignalState(int index, String signalString) {
        updateSignalProperties();

        if (index < 0 || index >= this.getLinkedSignals().size())
            return "Invalid index";

        List<SignalTileEntity> linkedSignals = this.getLinkedSignals();
        SignalTileEntity signalEntity = linkedSignals.get(index);
        BlockPos signalEntBlockPos = signalEntity.getPos();
        Signal signalBlock = (Signal) getBlock(signalEntBlockPos);

        Map<String, SEProperty> map = signalProperties.get(signalEntBlockPos);

        if (map == null)
            return "Invalid signal";

        return SignalStateHandler.getState(new SignalStateInfo(signalEntity.getWorld(), signalEntBlockPos, signalBlock),
                map.get(signalString)).get();
    }

    /**
     * 
     * @param args[0] index of the signal in the getSignals() table
     * @param args[1] signal state
     * @return returns true if the signal state was set
     */
    @LuaMethod(arguments = { int.class, String.class, String.class })
    public final Object setSignalState(int index, String signalString, String signalState) {
        updateSignalProperties();

        if (index < 0 || index >= this.getLinkedSignals().size())
            return "Invalid index";

        if (signalString == null)
            return "Invalid signal";

        if (index < 0 || index >= this.getLinkedSignals().size())
            return "Invalid index";

        List<SignalTileEntity> linkedSignals = this.getLinkedSignals();
        SignalTileEntity signalEntity = linkedSignals.get(index);
        BlockPos signalEntBlockPos = signalEntity.getPos();
        Signal signalBlock = (Signal) getBlock(signalEntBlockPos);

        Map<String, SEProperty> map = signalProperties.get(signalEntBlockPos);
        System.out.println(signalString);
        SEProperty property = map.get(signalString);

        map.forEach((key, value) -> {
            System.out.println(key + ": " + value.getName());
        });

        if (property == null)
            return "Invalid property";

        if (signalBlock == null)
            return "Invalid signal block";

        SignalStateHandler.setState(new SignalStateInfo(signalEntity.getWorld(), signalEntBlockPos, signalBlock),
                property,
                signalState);

        return true;
    }

    @LuaMethod(arguments = { int.class, String.class })
    public final Object getValidStates(int index, String signalString) {
        updateSignalProperties();

        if (index < 0 || index >= this.getLinkedSignals().size())
            return "Invalid index";

        if (signalString == null)
            return "Invalid signal";

        Map<String, SEProperty> properties = signalProperties.get(this.getLinkedSignals().get(index).getPos());
        if (properties == null)
            return "Invalid index or signal, index was " + index;

        SEProperty property = properties.get(signalString);
        if (property == null)
            return "Invalid signal, signal was " + signalString;

        return property.getAllowedValues();
    }
}
