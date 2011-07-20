/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.repository.TestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class BlueprintServiceTest extends TestCase {

    public static interface TestChannelInfo {
        @Parameter(name = "testme", defaultValue = "aap")
        String getTestme();
    }

    @Test
    public void defaultValuesAreOverriddenByBlueprint() throws RepositoryException {
        Node test = session.getRootNode().addNode("test", "nt:unstructured");
        Node bp = test.addNode("blueprint", "hst:blueprint");
        bp.setProperty("hst:name", "test-blueprint");
        bp.setProperty("hst:channelinfoclass", TestChannelInfo.class.getName());

        Node props = bp.addNode("hst:defaultchannelinfo", "hst:channelinfo");
        props.setProperty("testme", "noot");

        BlueprintService bps = new BlueprintService(bp);
        List<HstPropertyDefinition> propertyDefinitions = bps.getPropertyDefinitions();
        assertEquals(1, propertyDefinitions.size());

        HstPropertyDefinition definition = propertyDefinitions.get(0);
        assertEquals("noot", definition.getDefaultValue());
    }

}
