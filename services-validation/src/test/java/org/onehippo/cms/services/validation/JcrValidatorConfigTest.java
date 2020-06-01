/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;

public class JcrValidatorConfigTest {

    @Test(expected = IllegalStateException.class)
    public void throwsIllegalStateExceptionIfClassNameIsNotConfigured() throws Exception {
        final Node configNode = createConfigNode("validator-name", null);
        new JcrValidatorConfig(configNode);
    }

    @Test
    public void loadsValidatorNameAndClassName() throws Exception {
        final Node configNode = createConfigNode("validator-name", "validator-class-name");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        assertEquals("validator-class-name", validatorConfig.getClassName());
    }

    @Test
    public void getNode() throws Exception {
        final MockNode configNode = createConfigNode("validator-name", "validator-class-name");
        final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);

        assertEquals(configNode, validatorConfig.getNode());
    }

    private MockNode createConfigNode(final String name, final String className) throws RepositoryException {
        final MockNode node = MockNode.root().addNode(name, JcrConstants.NT_UNSTRUCTURED);
        if (className != null) {
            node.setProperty(JcrValidatorConfig.CLASS_NAME, className);
        }
        return node;
    }

}
