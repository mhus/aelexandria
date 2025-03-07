/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.ae.core;

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.lang.Function0;
import de.mhus.ae.cfg.CfgFactory;
import de.mhus.ae.cfg.GlobalCfgPanel;

import de.mhus.ae.system.SystemInfoPanel;
import de.mhus.ae.system.SystemLogPanel;
import de.mhus.ae.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class PanelService {

    private DeskTab addPanel(
            DeskTab parentTab,
            String id, String title, boolean unique, AbstractIcon icon, Function0<com.vaadin.flow.component.Component> panelCreator) {
        return parentTab.getTabBar().addTab(
                id,
                title,
                true,
                unique,
                icon,
                panelCreator)
        .setColor(parentTab.getColor())
        .setParentTab(parentTab);
    }

    public DeskTab addPanel(
            Core core,
            String id, String title, boolean unique, AbstractIcon icon, Function0<com.vaadin.flow.component.Component> panelCreator) {
        return core.getTabBar().addTab(
                        id,
                        title,
                        true,
                        unique,
                        icon,
                        panelCreator)
                .setColor(UiUtil.COLOR.NONE);
    }

    public DeskTab showGlobalCfgPanel(Core core, List<CfgFactory> globalFactories, File configDir, File ... fallbackDirs) {
        return addPanel(
                core,
                "global-cfg",
                "Global Settings",
                true,
                VaadinIcon.COGS.create(),
                () -> new GlobalCfgPanel(
                        core,
                        true,
                        globalFactories,
                        configDir,
                        fallbackDirs))
                .setReproducable(true)
                .setHelpContext("global_cfg")
                .setWindowTitle("Global Settings");
    }

    public DeskTab showUserCfgPanel(Core core, List<CfgFactory> factories, File configDir, File ... fallbackDirs) {
        return addPanel(
                core,
                "user-cfg",
                "User Settings",
                true,
                VaadinIcon.COG.create(),
                () -> new GlobalCfgPanel(
                        core,
                        false,
                        factories,
                        configDir,
                        fallbackDirs))
                .setReproducable(true)
                .setHelpContext("user_cfg")
                .setWindowTitle("User Settings");
    }

    public DeskTab showSystemInfoPanel(Core core) {
        return addPanel(
                core,
                "system-info",
                "System Info",
                true,
                VaadinIcon.INFO_CIRCLE_O.create(),
                () -> new SystemInfoPanel()
        )
                .setReproducable(true)
                .setHelpContext("system_info");
    }

    public DeskTab showSystemLogPanel(Core core) {
        return addPanel(
                core,
                "system-log",
                "System Logs",
                true,
                VaadinIcon.INFO_CIRCLE_O.create(),
                () -> new SystemLogPanel()
        )
                .setReproducable(true)
                .setHelpContext("system_logs");
    }

}
