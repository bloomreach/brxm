/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentProperty;
import org.hippoecm.repository.TestCase;
import org.junit.Test;

public class ContainerItemComponentResourceTest extends TestCase {
    
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_PARAMETERNAMEPREFIXES = "hst:parameternameprefixes";

    private static final String[] emptyTestComponent = {
        "/test", "nt:unstructured",
        "/test/component", "hst:containeritemcomponent",
        "hst:componentclassname", "org.hippoecm.hst.pagecomposer.jaxrs.services.DummyComponent",
        "hst:xtype", "HST.Item"
    };
    
    private static final String[] testComponent = {
        "/test", "nt:unstructured",
        "/test/component", "hst:containeritemcomponent",
        "hst:componentclassname", "org.hippoecm.hst.pagecomposer.jaxrs.services.DummyComponent",
        "hst:xtype", "HST.Item",
        "hst:parameternameprefixes", "",
        "hst:parameternameprefixes", "prefix",
        "hst:parameternames", "foo",
        "hst:parameternames", "foo",
        "hst:parametervalues", "bar",
        "hst:parametervalues", "baz"
    };
    
    @Test
    public void testGetParameters() throws RepositoryException, ClassNotFoundException {
        build(session, testComponent);
        Node node = session.getNode("/test/component");
        
        List<ContainerItemComponentProperty> result = null;
                
        result = new ContainerItemComponentResource().doGetParameters(node, null, "").getProperties();
        assertEquals(2, result.size());
        assertEquals("foo", result.get(0).getName());
        assertEquals("bar", result.get(0).getValue());
        assertEquals("", result.get(0).getDefaultValue());
        assertEquals("bar", result.get(1).getName());
        assertEquals("", result.get(1).getValue());
        assertEquals("test", result.get(1).getDefaultValue());
        
        result = new ContainerItemComponentResource().doGetParameters(node, null, "prefix").getProperties();
        assertEquals(2, result.size());
        assertEquals("foo", result.get(0).getName());
        assertEquals("baz", result.get(0).getValue());
        assertEquals("bar", result.get(0).getDefaultValue());
        assertEquals("bar", result.get(1).getName());
        assertEquals("", result.get(1).getValue());
        assertEquals("test", result.get(1).getDefaultValue());
    }
    
    @Test
    public void testSetParametersWithoutPrefix() throws RepositoryException {
        build(session, emptyTestComponent);
        Node node = session.getNode("/test/component");
        
        Value[] names = null;
        Value[] values = null;
        
        MultivaluedMap<String, String> params = null;
        
        // 1. add foo = bar
        params = new MetadataMap<String, String>();
        params.add("foo", "bar");
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(1, names.length);
        assertEquals("foo", names[0].getString());
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(1, values.length);
        assertEquals("bar", values[0].getString());
        
        // 2. if params is empty parameters should be removed
        params = new MetadataMap<String, String>();
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        assertTrue(!node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(!node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        // 3. add bar = test without prefix
        // because test is annotated default value for bar nothing should be persisted
        params = new MetadataMap<String, String>();
        params.add("bar", "test");
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        assertTrue(!node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(!node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));

    }
    
    @Test
    public void testSetParameterWithPrefix() throws RepositoryException {
        build(session, emptyTestComponent);
        Node node = session.getNode("/test/component");
        
        Value[] names = null;
        Value[] values = null;
        Value[] prefixes = null;
        
        MultivaluedMap<String, String> params = null;
        
        // 1. add foo = bar
        params = new MetadataMap<String, String>();
        params.add("foo", "bar");
        
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(1, names.length);
        assertEquals("foo", names[0].getString());
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(1, values.length);
        assertEquals("bar", values[0].getString());
        
        prefixes = node.getProperty(HST_PARAMETERNAMEPREFIXES).getValues();
        assertEquals(1, prefixes.length);
        assertEquals("prefix", prefixes[0].getString());
        
        // 2. if params is empty parameters should be removed
        params = new MetadataMap<String, String>();
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);
        assertTrue(!node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(!node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));

        // 3. if prefixed parameter value is same as default parameter value then don't persist
        
        // first set default parameter foo = bar
        node.setProperty(HST_PARAMETERNAMES, new String[] {"foo"});
        node.setProperty(HST_PARAMETERVALUES, new String[] {"bar"});
        session.save();

        // try to add foo = bar in variant "prefix"
        params = new MetadataMap<String, String>();
        params.add("foo", "bar");
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(1, names.length);
        assertEquals("foo", names[0].getString());
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(1, values.length);
        assertEquals("bar", values[0].getString());
        
        // 4. if prefixed parameter value is different from default parameter value but same
        // as annotated default value then do persist
        
        // first set default parameter bar = baz (different from annotated default)
        node.setProperty(HST_PARAMETERNAMES, new String[] {"bar"});
        node.setProperty(HST_PARAMETERVALUES, new String[] {"baz"});
        session.save();
        
        // try to add bar = test in variant "prefix" (test is annotated default for bar)
        params = new MetadataMap<String, String>();
        params.add("bar", "test");
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(2, names.length);
        assertEquals("bar", names[0].getString());
        assertEquals("bar", names[1].getString());
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(2, values.length);
        assertEquals("baz", values[0].getString());
        assertEquals("test", values[1].getString());
        
        prefixes = node.getProperty(HST_PARAMETERNAMEPREFIXES).getValues();
        assertEquals(2, prefixes.length);
        assertEquals("", prefixes[0].getString());
        assertEquals("prefix", prefixes[1].getString());
    }
}
