/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefDiff;

public class HippoNodeTypeRegistry extends NodeTypeRegistry {

    private final NamespaceRegistry registry;

    public HippoNodeTypeRegistry(NamespaceRegistry registry, FileSystem fileSystem) throws RepositoryException {
        super(registry, fileSystem);
        this.registry = registry;
    }

    /**
     * Skip checks for changes in the hippo namespaces. "Trust me, I know what I'm doing".
     *
     * @param ntd  The node type definition replacing the former node type definition of the same name.
     * @param diff The diff of the node type definition with the currently registered type
     * @throws javax.jcr.RepositoryException
     */
    @Override
    protected void checkForConflictingContent(final QNodeTypeDefinition ntd, NodeTypeDefDiff diff) throws RepositoryException {
        final Name name = ntd.getName();
        final String prefix = registry.getPrefix(name.getNamespaceURI());
        final String[] systemPrefixes = {"hippo", "hipposys", "hipposysedit", "hippofacnav", "hipposched"};
        for (String systemPrefix : systemPrefixes) {
            if (prefix.equals(systemPrefix)) {
                return;
            }
        }
        super.checkForConflictingContent(ntd, diff);
    }
}