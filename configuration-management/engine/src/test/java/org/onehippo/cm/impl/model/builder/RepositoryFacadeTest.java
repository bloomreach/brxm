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
package org.onehippo.cm.impl.model.builder;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RepositoryFacadeTest extends RepositoryTestCase {

    @Ignore
    @Test
    public void test_order() throws Exception {
        final String yaml
                = "instructions:\n"
                + "- config:\n"
                + "  - /root:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - /c:\n"
                + "      - .meta:order-before: b\n"
                + "      - jcr:primaryType: nt:unstructured\n"
                + "      - prop-c: value-c\n"
                + "    - /b:\n"
                + "      - .meta:order-before: a\n"
                + "      - jcr:primaryType: nt:unstructured\n"
                + "      - prop-b: value-b\n"
                + "    - /a:\n"
                + "      - jcr:primaryType: nt:unstructured\n"
                + "      - prop-a: value-a\n"
                + "";

        final Node testNode = session.getRootNode().addNode("test");
        final Node rootNode = testNode.addNode("root");
        rootNode.addNode("a");
        session.save();

        final RepositoryFacade repositoryFacade = new RepositoryFacade(session, testNode);
        final MergedModelBuilder mergedModelBuilder = new MergedModelBuilder();
        mergedModelBuilder.push(utils.parseToConfigurationImpl(yaml));
        final MergedModel mergedModel = mergedModelBuilder.build();

        repositoryFacade.push(mergedModel);

        assertEquals("[c,b,a]", utils.nodeListToString(rootNode.getNodes()));
    }

    // TODO refactor
    private final Utils utils = new Utils();
    private class Utils extends AbstractBuilderBaseTest {
        ConfigurationImpl parseToConfigurationImpl(final String yaml) throws Exception {
            final List<Definition> definitions = parseNoSort(yaml);
            assertTrue(definitions.size() > 0);
            return (ConfigurationImpl) definitions.get(0).getSource().getModule().getProject().getConfiguration();
        }
        public String nodeListToString(final NodeIterator nodes) throws RepositoryException {
            final StringBuilder builder = new StringBuilder();
            builder.append('[');
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                if (builder.length() > 1) {
                    builder.append(',');
                }
                builder.append(node.getName());
            }
            builder.append(']');
            return builder.toString();
        }
    }

}
