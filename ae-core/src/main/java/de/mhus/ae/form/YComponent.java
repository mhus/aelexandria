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
package de.mhus.ae.form;

import com.vaadin.flow.component.Component;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public abstract class YComponent<T> {

    @Setter
    protected String path;
    @Setter
    protected String label;
    @Setter
    protected T defaultValue;
    @Setter
    protected boolean readOnly = false;

    public abstract void initUi();

    public abstract Component getComponent();

    public abstract void load(ITreeNode content);

    public abstract void save(ITreeNode node);

    protected ITreeNode getParent(ITreeNode content) {
        return content.getObjectByPath(getParentPath()).orElse(MTree.EMPTY_MAP);
    }

    protected String getParentPath() {
        return MTree.getParentPath(path);
    }

    protected String getNodeName() {
        return MTree.getNodeName(path);
    }

}
