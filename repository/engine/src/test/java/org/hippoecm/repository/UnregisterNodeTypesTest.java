/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.Collection;
import java.util.LinkedList;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.hippoecm.repository.jackrabbit.HippoNodeTypeRegistry;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.fail;

/**
 * Test for REPO-2143 fix
 */
public class UnregisterNodeTypesTest extends RepositoryTestCase {

    @Test
    public void testUnregisterNodeTypes() throws RepositoryException {
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
        HippoNodeTypeRegistry ntreg = (HippoNodeTypeRegistry)ntmgr.getNodeTypeRegistry();
        NameFactory nameFactory = NameFactoryImpl.getInstance();
        final QNodeTypeDefinitionBuilder typeFoo = new QNodeTypeDefinitionBuilder();
        typeFoo.setName(nameFactory.create("", "typeFoo"));
        typeFoo.setSupertypes(new Name[]{NameConstants.NT_BASE});
        final QNodeTypeDefinitionBuilder typeBar = new QNodeTypeDefinitionBuilder();
        typeBar.setName(nameFactory.create("", "typeBar"));
        typeBar.setSupertypes(new Name[]{NameConstants.NT_BASE});
        Collection<QNodeTypeDefinition> ntDefCollection = new LinkedList<>();
        ntDefCollection.add(typeFoo.build());
        ntDefCollection.add(typeBar.build());
        try {
            ntreg.registerNodeTypes(ntDefCollection);
        } catch (InvalidNodeTypeDefException|RepositoryException e) {
            fail("Failed to register nodetypes: " + e.getMessage());
        }
        try {
            // allow unregisterNodeType call (once)
            ntreg.ignoreNextCheckReferencesInContent();
            ntreg.unregisterNodeType(typeBar.getName());
            // allow unregisterNodeType call (once)
            ntreg.ignoreNextCheckReferencesInContent();
            // with REPO-2143 fix in Jackrabbit core the following (subsequent unregisterNodeType) should no longer fail
            ntreg.unregisterNodeType(typeFoo.getName());
        } catch (RepositoryException e) {
            e.printStackTrace();
            fail("Failed to unregister nodetypes: " + e.getMessage());
        }
    }
}
