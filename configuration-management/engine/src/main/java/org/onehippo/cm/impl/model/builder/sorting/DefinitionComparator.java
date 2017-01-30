/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Comparator;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ModuleImpl;

class DefinitionComparator implements Comparator<Definition> {

    private final ModuleImpl module;

    DefinitionComparator(final ModuleImpl module) {
        this.module = module;
    }

    @Override
    public int compare(final Definition def1, final Definition def2) {
        int nsComparison = compareNameSpaceDefs(def1, def2);
        if (nsComparison != 0) {
            return nsComparison;
        }

        int ntComparison = compareNodeTypeDefs(def1, def2);
        if (ntComparison != 0) {
            return ntComparison;
        }

        return compareContentDefs(def1, def2);
    }

    private int compareNameSpaceDefs(final Definition def1, final Definition def2) {
        if (def1 instanceof NamespaceDefinition) {
            if (def2 instanceof NamespaceDefinition) {
                return ((NamespaceDefinition)def1).getPrefix().compareTo(((NamespaceDefinition)def2).getPrefix());
            }
            // def 1 should be first
            return -1;
        }
        if (def2 instanceof NamespaceDefinition) {
            return 1;
        }
        return 0;
    }

    private int compareNodeTypeDefs(final Definition def1, final Definition def2) {
        if (def1 instanceof NodeTypeDefinition) {
            if (def2 instanceof NodeTypeDefinition) {
                // TODO both def1 and def2 are of type NodeTypeDefinition : It might be that cnd of def2
                // TODO depends on the cnd of def1 : Hence we need to parse the actual cnd value to find out
                // TODO whether def1 or def2 should be loaded first. For now. just compare the cnd string
                return ((NodeTypeDefinition)def1).getValue().compareTo(((NodeTypeDefinition)def2).getValue());
            }
            // def1 should be first
            return -1;
        }
        if (def2 instanceof NodeTypeDefinition) {
            return 1;
        }
        return 0;
    }

    private int compareContentDefs(final Definition def1, final Definition def2) {
        final String rootPath1 = ((ContentDefinitionImpl) def1).getNode().getPath();
        final String rootPath2 = ((ContentDefinitionImpl) def2).getNode().getPath();

        if (def1 != def2 && rootPath1.equals(rootPath2)) {
            throw new IllegalStateException("Duplicate content root paths '" + rootPath1 + "' in module '"
                    + module.getName() +"'.");
        }
        return rootPath1.compareTo(rootPath2);
    }
}
