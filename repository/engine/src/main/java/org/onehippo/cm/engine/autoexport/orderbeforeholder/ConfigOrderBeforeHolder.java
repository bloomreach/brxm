/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onehippo.cm.engine.autoexport.orderbeforeholder;

import java.util.Iterator;

import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.path.JcrPath;

public abstract class ConfigOrderBeforeHolder extends OrderBeforeHolder {

    private final int moduleIndex;
    private final DefinitionNodeImpl definitionNode;
    private int siblingIndex;

    ConfigOrderBeforeHolder(final int moduleIndex, final DefinitionNodeImpl definitionNode) {
        this.moduleIndex = moduleIndex;
        this.definitionNode = definitionNode;
        this.siblingIndex = -1;
    }

    @Override
    public int compareTo(final Object object) {
        if (object == null) {
            return -1;
        }
        if (object == this) {
            return 0;
        }
        if (object instanceof ConfigOrderBeforeHolder) {
            // Definitions are merged into the configuration model by first sorting all modules, then all definitions
            // within a module on their root path, and then by applying the nodes in a definition in the order they
            // appear in the source file. ConfigOrderBeforeHolders must be sorted identically, so:
            // - first, compare the module order
            // - second, compare the root path of the definition
            // - third, compare the position within the parent

            final ConfigOrderBeforeHolder other = (ConfigOrderBeforeHolder) object;

            int result = Integer.compare(this.getModuleIndex(), other.getModuleIndex());
            if (result != 0) {
                return result;
            }

            result = this.getRootPath().compareTo(other.getRootPath());
            if (result != 0) {
                return result;
            }

            return Integer.compare(this.getSiblingIndex(), other.getSiblingIndex());
        }
        return -1; // Assuming 'object' is content, which means this config object must be sorted earlier
    }

    private int getModuleIndex() {
        return moduleIndex;
    }

    JcrPath getRootPath() {
        return definitionNode.getDefinition().getRootPath();
    }

    private int getSiblingIndex() {
        if (siblingIndex == -1 && !definitionNode.isRoot()) {
            int index = -1;
            for (Iterator<DefinitionNodeImpl> it = definitionNode.getParent().getNodes().iterator(); it.hasNext();) {
                index++;
                if (definitionNode.getJcrName().equals(it.next().getJcrName())) {
                    break;
                }
            }
            siblingIndex = index;
        }
        return siblingIndex;
    }

    @Override
    DefinitionNodeImpl getDefinitionNode() {
        return definitionNode;
    }
}
