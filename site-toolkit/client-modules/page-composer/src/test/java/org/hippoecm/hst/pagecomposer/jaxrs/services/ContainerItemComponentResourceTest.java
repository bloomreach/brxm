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
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
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
        "hst:parameternames", "parameterOne",
        "hst:parameternames", "parameterOne",
        "hst:parametervalues", "bar",
        "hst:parametervalues", "baz"
    };
    
    @Test
    public void testGetParameters() throws RepositoryException, ClassNotFoundException {
        build(session, testComponent);
        Node node = session.getNode("/test/component");
        
        List<ContainerItemComponentPropertyRepresentation> result = new ContainerItemComponentResource().doGetParameters(node, null, "").getProperties();
        assertEquals(2, result.size());
        assertEquals("parameterOne", result.get(0).getName());
        assertEquals("bar", result.get(0).getValue());
        assertEquals("", result.get(0).getDefaultValue());
        assertEquals("parameterTwo", result.get(1).getName());
        assertEquals("", result.get(1).getValue());
        assertEquals("test", result.get(1).getDefaultValue());
        
        result = new ContainerItemComponentResource().doGetParameters(node, null, "prefix").getProperties();
        assertEquals(2, result.size());
        assertEquals("parameterOne", result.get(0).getName());
        assertEquals("baz", result.get(0).getValue());
        assertEquals("", result.get(0).getDefaultValue());
        assertEquals("parameterTwo", result.get(1).getName());
        assertEquals("", result.get(1).getValue());
        assertEquals("test", result.get(1).getDefaultValue());
    }
    
    @Test
    public void testSetParametersWithoutPrefix() throws RepositoryException {
        build(session, emptyTestComponent);
        Node node = session.getNode("/test/component");

        // 1. add foo = bar
        MultivaluedMap<String, String> params = new MetadataMap<String, String>();
        params.add("parameterOne", "bar");
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertFalse(node.hasProperty(HST_PARAMETERNAMEPREFIXES));

        Value[] names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(1, names.length);
        assertEquals("parameterOne", names[0].getString());

        Value[] values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(1, values.length);
        assertEquals("bar", values[0].getString());
        
        // 2. if params is empty, old values should be kept for 'default' AS IS
        params = new MetadataMap<String, String>();
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(1, names.length);
        assertEquals("parameterOne", names[0].getString());
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(1, values.length);
        assertEquals("bar", values[0].getString());
        assertFalse(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        // 3. add bar = test without prefix
        // We should keep the already existing "parameterOne = bar" 
        // but now should also have "parameterTwo = test" : Even though the default
        // value for DummyInfo for "parameterTwo = test" , we STILL store it
        params = new MetadataMap<String, String>();
        params.add("parameterTwo", "test");
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(2, names.length);
        
        // names[0] is either  parameterOne or parameterTwo (not sure about the order)
        assertTrue(names[0].getString().equals("parameterOne") || names[0].getString().equals("parameterTwo"));
        assertTrue(names[1].getString().equals("parameterOne") || names[1].getString().equals("parameterTwo"));
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(2, values.length);
        if(names[0].getString().equals("parameterOne")) {
            assertEquals("bar", values[0].getString());
            assertEquals("test", values[1].getString());
        } else {
            assertEquals("test", values[0].getString());
            assertEquals("bar", values[1].getString());
        }
        assertFalse(node.hasProperty(HST_PARAMETERNAMEPREFIXES));

    }
    
    @Test
    public void testSetParameterWithPrefix() throws RepositoryException {
        build(session, emptyTestComponent);
        Node node = session.getNode("/test/component");
        
        Value[] names;
        Value[] values;
        Value[] prefixes;
        
        MultivaluedMap<String, String> params;
        
        // 1. add foo = bar
        params = new MetadataMap<String, String>();
        params.add("parameterOne", "bar");
        
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(1, names.length);
        assertEquals("parameterOne", names[0].getString());
        
        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(1, values.length);
        assertEquals("bar", values[0].getString());
        
        prefixes = node.getProperty(HST_PARAMETERNAMEPREFIXES).getValues();
        assertEquals(1, prefixes.length);
        assertEquals("prefix", prefixes[0].getString());
        

        // 2. if params is empty, old values for a PREFIX should be REMOVED : because
        // there is no 'default' prefix configured, all parameters should be removed now
        params = new MetadataMap<String, String>();
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);

        assertFalse(node.hasProperty(HST_PARAMETERNAMES));
        assertFalse(node.hasProperty(HST_PARAMETERVALUES));
        assertFalse(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        // 3. if prefixed parameter value is same as default parameter value then persist anyway
        
        // first set default parameter parameterOne = bar
        node.setProperty(HST_PARAMETERNAMES, new String[] {"parameterOne"});
        node.setProperty(HST_PARAMETERVALUES, new String[] {"bar"});
        node.setProperty(HST_PARAMETERNAMEPREFIXES, new String[] {""});
        session.save();

        // try to add parameterOne = bar in variant "prefix"
        params = new MetadataMap<String, String>();
        params.add("parameterOne", "bar");
        new ContainerItemComponentResource().doSetParameters(node, "prefix", params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        // we should now have default parameterOne = bar and prefixed parameterOne = bar
        
        names = node.getProperty(HST_PARAMETERNAMES).getValues();
        assertEquals(2, names.length);
        assertEquals("parameterOne", names[0].getString());
        assertEquals("parameterOne", names[1].getString());

        values = node.getProperty(HST_PARAMETERVALUES).getValues();
        assertEquals(2, values.length);
        assertEquals("bar", values[0].getString());
        assertEquals("bar", values[1].getString());

        assertEquals(2, values.length);
        
    }
    
    
    @Test
    public void testVariantCreation()  throws RepositoryException {
        build(session, emptyTestComponent);
        Node node = session.getNode("/test/component");

       
        MultivaluedMap<String, String> params;

        // 1. add a non annotated parameter for 'default someNonAnnotatedParameter = lux 
        params = new MetadataMap<String, String>();
        params.add("parameterOne", "bar");
        params.add("someNonAnnotatedParameter", "lux");
        new ContainerItemComponentResource().doSetParameters(node, null, params);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        // do not contain HST_PARAMETERNAMEPREFIXES
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        // the explicit configured parameters for 'default' prefix
        Map<String, String> defaultParams =  ContainerItemComponentResource.getConfiguredDefaultValues(node);
        assertTrue(defaultParams.containsKey("parameterOne"));
        assertTrue(defaultParams.containsKey("someNonAnnotatedParameter"));
        
        Map<String, String> defaultAnnotated =  ContainerItemComponentResource.getAnnotatedDefaultValues(node);
        assertTrue(defaultAnnotated.containsKey("parameterOne"));
        assertEquals(defaultAnnotated.get("parameterOne"), "");
        assertTrue(defaultAnnotated.containsKey("parameterTwo"));
        assertEquals(defaultAnnotated.get("parameterTwo"), "test");
       
        
        Set<String> variants =  ContainerItemComponentResource.getVariants(node);
        assertTrue(variants.size() == 1);
        assertTrue(variants.contains("default"));
        
        // 2. create a new variant 'lux' : The creation of the variant should 
        // pick up the explicitly defined parameters from 'default' that are ALSO annotated (thus parameterOne, and NOT someNonAnnotatedParameter) PLUS
        // the implicit parameters from the DummyInfo (parameterTwo but not parameterOne because already from 'default')
        new ContainerItemComponentResource().doCreateVariant(node, "newvar");
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        // now it must contain HST_PARAMETERNAMEPREFIXES
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));
        
        variants = ContainerItemComponentResource.getVariants(node);
        assertTrue(variants.size() == 2);
        assertTrue(variants.contains("default"));
        assertTrue(variants.contains("newvar"));
        
        // getHstParameters(node, true) only fetches the default parameters
        Map<String, Map<String, String>> hstParameters = ContainerItemComponentResource.getHstParameters(node, true);
        assertTrue(hstParameters.containsKey("default"));
        assertFalse(hstParameters.containsKey("newvar"));
        

        // getHstParameters(node, false) only fetches all parameters
        hstParameters = ContainerItemComponentResource.getHstParameters(node, false);
        assertTrue(hstParameters.containsKey("default"));
        assertTrue(hstParameters.containsKey("newvar"));
        
        Map<String, String> newVarParams = hstParameters.get("newvar");
        assertTrue(newVarParams.size() == 2);
        assertTrue(newVarParams.containsKey("parameterOne"));
        assertEquals(newVarParams.get("parameterOne"), "bar");
        assertTrue(newVarParams.containsKey("parameterTwo"));
        // from  @Parameter(name = "parameterTwo", required = true, defaultValue = "test")
        assertEquals(newVarParams.get("parameterTwo"), "test");
        
        assertFalse(newVarParams.containsKey("someNonAnnotatedParameter"));
        
        // 3. try to remove the new variant
        new ContainerItemComponentResource().doDeleteVariant(node, "newvar");
        variants = ContainerItemComponentResource.getVariants(node);
        assertTrue(variants.size() == 1);
        assertTrue(variants.contains("default"));
        
        // 4. try to remove the 'default' variant : this should not be allowed
        boolean removeSucceeded = true;
        try {
            new ContainerItemComponentResource().doDeleteVariant(node, "default");
            fail("Default variant should not be possible to be removed");
        } catch (IllegalArgumentException e) {
            removeSucceeded = false;
        }
        assertFalse("Remove should not have succeeded", removeSucceeded);
    }
    
}
