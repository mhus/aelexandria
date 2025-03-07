package de.mhus.ae.main;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.ae.core.Core;

public class MainPanel extends VerticalLayout {
    private final Core core;

    public MainPanel(Core core) {
        this.core = core;
    }
}
