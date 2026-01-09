package com.troblecodings.signals.guis;

import com.troblecodings.guilib.ecs.ContainerBase;
import com.troblecodings.guilib.ecs.GuiBase;
import com.troblecodings.guilib.ecs.GuiElements;
import com.troblecodings.guilib.ecs.GuiInfo;
import com.troblecodings.guilib.ecs.entitys.UIBox;
import com.troblecodings.guilib.ecs.entitys.UIEntity;
import com.troblecodings.guilib.ecs.entitys.input.UIScroll;
import com.troblecodings.guilib.ecs.entitys.render.UITexture;
import com.troblecodings.signals.tileentitys.ComputerLinkEntity;
import com.troblecodings.signals.handler.ClientRenderUpdate;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import com.troblecodings.signals.OpenSignalsMain;
import com.troblecodings.signals.enums.LinkType;

public class GuiComputerLink extends GuiBase {

    public GuiComputerLink(GuiInfo info) {
        super(info);
        final ComputerLinkEntity tile = info.getTile(ComputerLinkEntity.class);
        if (tile == null) {
            // Handle null tile entity if necessary, but no print statement
        } else {
            // No print statement
        }
        final UIEntity scrollContainer = new UIEntity();
        scrollContainer.setInherits(true);

        final UIBox vbox = new UIBox(UIBox.VBOX, 5);
        final UIEntity list = new UIEntity();
        list.add(vbox);
        list.setInheritWidth(true);
        list.setHeight(tile.getLinkedSignals().size() * 25);

        for (BlockPos pos : tile.getLinkedSignals()) {
            final UIEntity row = new UIEntity();
            row.setHeight(20);
            row.setInheritWidth(true);
            row.add(new UIBox(UIBox.HBOX, 5));

            final UITexture texture = new UITexture(UISignalBoxTile.ICON, 0.2 * 0, 0.5, 0.2 * 0 + 0.2, 1);

            UIEntity entity = new UIEntity();
            entity.add(texture);
            entity.setWidth(20);
            entity.setHeight(20);
            entity.setInheritWidth(false);
            row.add(entity);

            UIEntity btn = GuiElements.createButton(GuiSignalBox.getSignalInfo(pos, LinkType.SIGNAL), e -> {
                ClientRenderUpdate.INSTANCE.clearHighlights();
                ClientRenderUpdate.INSTANCE.addHighlight(pos);
            });
            btn.setWidth(200);
            btn.setX(25);
            btn.setHeight(20);

            row.add(btn);

            list.add(row);
        }

        scrollContainer.add(new UIScroll(s -> {
            list.setY(list.getY() + s * 10);
        }));

        scrollContainer.add(list);
        this.entity.add(scrollContainer);
    }

    @Override
    public ContainerBase getNewGuiContainer(GuiInfo info) {
        return new ContainerComputerLink(info);
    }
}
