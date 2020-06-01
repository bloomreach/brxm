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
package org.hippoecm.repository;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.NT_BASE;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;

public class HippoRepRootNodeTypeTest extends RepositoryTestCase {

    @Test
    public void verifyRepRootNodeType() throws RepositoryException {
        NodeType repRootNodeType = session.getRootNode().getPrimaryNodeType();
        assertTrue(repRootNodeType.getName().equals("rep:root"));
        NodeType[] superTypes = repRootNodeType.getDeclaredSupertypes();
        assertTrue(superTypes.length == 1);
        // rep:root < nt:base (native Jackrabbit rep:root < nt:unstructured)
        assertTrue(superTypes[0].getName().equals(NT_BASE));
        // hippo rep:root doesn't allow orderable children  (native Jackrabbit rep:root allows this, as inherited from nt:unstructured)
        assertFalse(repRootNodeType.hasOrderableChildNodes());
        NodeDefinition[] childNodeDefs = repRootNodeType.getChildNodeDefinitions();
        assertTrue(childNodeDefs.length == 2);
        NodeDefinition residualChildNodeDef = childNodeDefs[0].getName().equals("*") ? childNodeDefs[0] : childNodeDefs[1];
        assertTrue(residualChildNodeDef.getName().equals("*"));
        assertTrue(residualChildNodeDef.getRequiredPrimaryTypeNames().length == 1);
        assertTrue(residualChildNodeDef.getRequiredPrimaryTypeNames()[0].equals(NT_BASE));
        assertTrue(residualChildNodeDef.getDefaultPrimaryTypeName().equals(NT_UNSTRUCTURED));
        // hippo rep:root doesn't allow SNS (native Jackrabbit repo:root allows this, as inherited from nt:unstructured)
        assertFalse(residualChildNodeDef.allowsSameNameSiblings());
    }
}
