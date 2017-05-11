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
package org.onehippo.cm.backend;

import org.junit.Test;
import org.onehippo.cm.api.ConfigurationModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModelTestUtils;
import org.onehippo.cm.impl.model.builder.MergedModelBuilder;
import org.onehippo.repository.testutils.RepositoryTestCase;

import javax.jcr.Node;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.impl.model.ModelTestUtils.parseNoSort;

public class JcrContentProcessingServiceTest extends RepositoryTestCase {

    @Test
    public void process_content() throws Exception {
        final String source
                =
                "/test:\n" +
                "   jcr:primaryType: nt:unstructured\n" +
                "   boolean: true\n" +
                "   date: 2015-10-21T07:28:00+08:00\n" +
                "   double: 3.1415\n" +
                "   longAsInt: 42\n" +
                "   longAsLong: 4200000000\n" +
                "   string: hello world\n" +
                "   /testChildNode:\n" +
                "       jcr:primaryType: nt:unstructured\n" +
                "       boolean: true\n" +
                "       date: 2015-10-21T07:28:00+08:00";


        final String source1
                = "/test/node1:\n" +
                "  jcr:primaryType: nt:unstructured\n" +
                "  boolean: true\n" +
                "  date: 2015-10-21T07:28:00+08:00\n" +
                "  double: 3.1415\n" +
                "  longAsInt: 42\n" +
                "  longAsLong: 4200000000\n" +
                "  string: hello world";

        final String source2
                = "/test/node2:\n" +
                "  jcr:primaryType: nt:unstructured\n" +
                "  boolean: true\n" +
                "  date: 2015-10-21T07:28:00+08:00\n" +
                "  double: 3.1415\n" +
                "  longAsLong: 4200000000\n" +
                "  string: hello world";

        final String source3
                = "/test/node3:\n" +
                "  jcr:primaryType: nt:unstructured\n" +
                "  boolean: true\n" +
                "  date: 2015-10-21T07:28:00+08:00\n" +
                "  double: 3.1415\n" +
                "  string: hello world";

        applyAndSaveDefinitions(new String[]{source, source1});
        applyAndSaveDefinitions(new String[]{source2, source3});

        Node node1 = session.getNode("/test/node1");
        assertEquals(7, node1.getProperties().getSize());
        Node node2 = session.getNode("/test/node2");
        assertEquals(6, node2.getProperties().getSize());
        Node node3 = session.getNode("/test/node3");
        assertEquals(5, node3.getProperties().getSize());
        Node testChildNode = session.getNode("/test/testChildNode");
        assertEquals(3, testChildNode.getProperties().getSize());
    }

    private void applyAndSaveDefinitions(final String[] sources) throws Exception {

        final Map<Module, ResourceInputProvider> resourceInputProviders = new HashMap<>();
        final MergedModelBuilder mergedModelBuilder = new MergedModelBuilder();
        for (int i = 0; i < sources.length; i++) {
            final List<Definition> definitions = parseNoSort(sources[i], "test-module-" + i, ContentDefinition.class);
            assertTrue(definitions.size() == 1);
            final Module module = definitions.get(0).getSource().getModule();
            final GroupImpl configuration = (GroupImpl) module.getProject().getGroup();
            mergedModelBuilder.push(configuration);
            resourceInputProviders.put(module, ModelTestUtils.getTestResourceInputProvider());
        }
        final ConfigurationModel configurationModel = mergedModelBuilder.build();

        final ContentProcessingService processingService = new JcrContentProcessingService(session, resourceInputProviders);
        processingService.apply(null, configurationModel.getContentDefinitions());

        session.save();
    }



}