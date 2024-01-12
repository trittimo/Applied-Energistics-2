/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.implementations;


import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.container.implementations.ContainerInterface;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.IInterfaceHost;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Mouse;

import java.io.IOException;


public class GuiInterface extends GuiUpgradeable {

    private GuiTabButton priority;
    private GuiImgButton blockMode;
    private GuiImgButton insertionMode;
    private GuiToggleButton interfaceMode;

    public GuiInterface(final InventoryPlayer inventoryPlayer, final IInterfaceHost te) {
        super(new ContainerInterface(inventoryPlayer, te));
        this.ySize = 256;
    }

    @Override
    protected void addButtons() {
        this.priority = new GuiTabButton(this.guiLeft + 154, this.guiTop, 2 + 4 * 16, GuiText.Priority.getLocal(), this.itemRender);
        this.buttonList.add(this.priority);

        this.blockMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 8, Settings.BLOCK, YesNo.NO);
        this.buttonList.add(this.blockMode);

        this.interfaceMode = new GuiToggleButton(this.guiLeft - 18, this.guiTop + 26, 84, 85, GuiText.InterfaceTerminal.getLocal(), GuiText.InterfaceTerminalHint.getLocal());
        this.buttonList.add(this.interfaceMode);

        this.insertionMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 44, Settings.INTERFACE_ALWAYS_ALLOW_INSERTION, YesNo.YES);
        this.buttonList.add(this.insertionMode);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        if (this.blockMode != null) {
            this.blockMode.set(((ContainerInterface) this.cvb).getBlockingMode());
        }

        if (this.insertionMode != null) {
            this.insertionMode.set(((ContainerInterface) this.cvb).getInsertionMode());
        }

        if (this.interfaceMode != null) {
            this.interfaceMode.setState(((ContainerInterface) this.cvb).getInterfaceTerminalMode() == YesNo.YES);
        }

        this.fontRenderer.drawString(this.getGuiDisplayName(GuiText.Interface.getLocal()), 8, 6, 4210752);

        this.fontRenderer.drawString(GuiText.Config.getLocal(), 8, 6 + 11 + 7, 4210752);
        this.fontRenderer.drawString(GuiText.StoredItems.getLocal(), 8, 6 + 60 + 7, 4210752);
        this.fontRenderer.drawString(GuiText.Patterns.getLocal(), 8, 6 + 73 + 7, 4210752);

    }

    @Override
    protected String getBackground() {
        int upgrades = ((ContainerInterface) this.cvb).getPatternUpgrades();
        if (upgrades == 0) {
            return "guis/newinterface.png";
        } else {
            return "guis/newinterface" + upgrades + ".png";
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        super.actionPerformed(btn);

        final boolean backwards = Mouse.isButtonDown(1);

        if (btn == this.priority) {
            NetworkHandler.instance().sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PRIORITY));
        }

        if (btn == this.interfaceMode) {
            NetworkHandler.instance().sendToServer(new PacketConfigButton(Settings.INTERFACE_TERMINAL, backwards));
        }

        if (btn == this.blockMode) {
            NetworkHandler.instance().sendToServer(new PacketConfigButton(this.blockMode.getSetting(), backwards));
        }

        if (btn == this.insertionMode) {
            NetworkHandler.instance().sendToServer(new PacketConfigButton(this.insertionMode.getSetting(), backwards));
        }
    }

}
