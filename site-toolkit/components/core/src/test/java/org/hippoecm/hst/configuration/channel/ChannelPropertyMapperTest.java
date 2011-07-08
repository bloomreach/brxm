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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class ChannelPropertyMapperTest extends AbstractHstTestCase {

    public static interface TestInfo {
        @Parameter(name = "test-name")
        String getName();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node root = getSession().getRootNode();
        root.addNode("test", "nt:unstructured");
        getSession().save();
    }

    @Test
    public void simplePropertyIsLoaded() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfo.class);

        HstPropertyDefinition nameDef = definitions.get(0);
        getSession().getNode("/test").setProperty("test-name", "aap");
        Map<HstPropertyDefinition, Object> values = ChannelPropertyMapper.loadProperties(getSession().getNode("/test"), definitions);
        assertTrue(values.containsKey(nameDef));
        Object value = values.get(nameDef);
        assertEquals("aap", value);
    }

    @Test
    public void unsetPropertyHasNullValue() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfo.class);

        HstPropertyDefinition nameDef = definitions.get(0);
        Map<HstPropertyDefinition, Object> values = ChannelPropertyMapper.loadProperties(getSession().getNode("/test"), definitions);
        assertTrue(values.containsKey(nameDef));
        assertNull(values.get(nameDef));
    }

    @Test
    public void simplePropertyIsStoredWithOwnName() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfo.class);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(definitions.get(0).getName(), "aap");
        ChannelPropertyMapper.saveProperties(getSession().getNode("/test"), definitions, values);

        assertTrue(getSession().itemExists("/test/test-name"));
        Property nameProperty = (Property) getSession().getItem("/test/test-name");
        assertEquals("aap", nameProperty.getString());
    }

}
