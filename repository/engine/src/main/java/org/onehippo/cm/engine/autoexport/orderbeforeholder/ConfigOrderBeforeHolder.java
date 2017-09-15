/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cm.model.impl.path.JcrPath;
import org.onehippo.cm.model.impl.path.JcrPathSegment;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;

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
        return -1;
    }

    private int getModuleIndex() {
        return moduleIndex;
    }

    JcrPath getRootPath() {
        return JcrPath.get(definitionNode.getDefinition().getRootPath());
    }

    private int getSiblingIndex() {
        if (siblingIndex == -1 && !definitionNode.isRoot()) {
            int index = -1;
            for (final String name : definitionNode.getParent().getNodes().keySet()) {
                index++;
                if (definitionNode.getJcrName().equals(JcrPathSegment.get(name))) {
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
