/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBException;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerItemComponentResourceTest {

    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_PARAMETERNAMEPREFIXES = "hst:parameternameprefixes";
    private ContainerItemComponentResource containerItemComponentResource;

    @Before
    public void setUp() throws Exception {
        containerItemComponentResource = new ContainerItemComponentResource();
        containerItemComponentResource.setProcessor(new ParametersInfoProcessor());
    }

    @Test
    public void testGetParametersForEmptyPrefix() throws RepositoryException, ClassNotFoundException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-test-component.xml");

        List<ContainerItemComponentPropertyRepresentation> result = containerItemComponentResource.doGetParameters(node,
                                                                                                                   null,
                                                                                                                   "").getProperties();
        assertEquals(2, result.size());
        assertNameValueDefault(result.get(0), "parameterOne", "bar", "");
        assertNameValueDefault(result.get(1), "parameterTwo", "", "test");
    }

    @Test
    public void testGetParametersForDefaultPrefix() throws RepositoryException, ClassNotFoundException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-test-component.xml");

        List<ContainerItemComponentPropertyRepresentation> result = containerItemComponentResource.doGetParameters(node, null, "hippo-default").getProperties();
        assertEquals(2, result.size());
        assertNameValueDefault(result.get(0), "parameterOne", "bar", "");
        assertNameValueDefault(result.get(1), "parameterTwo", "", "test");
    }

    @Test
    public void testGetParametersForPrefix() throws RepositoryException, ClassNotFoundException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-test-component.xml");

        List<ContainerItemComponentPropertyRepresentation> result = containerItemComponentResource.doGetParameters(node, null, "prefix").getProperties();
        assertEquals(2, result.size());
        assertNameValueDefault(result.get(0), "parameterOne", "baz", "");
        assertNameValueDefault(result.get(1), "parameterTwo", "", "test");
    }

    private static void assertNameValueDefault(ContainerItemComponentPropertyRepresentation property, String name, String value, String defaultValue) {
        assertEquals("Wrong name", name, property.getName());
        assertEquals("Wrong value", value, property.getValue());
        assertEquals("Wrong default value", defaultValue, property.getDefaultValue());
    }

    @Test
    public void testVariantCreation() throws RepositoryException, JAXBException, IOException {
        Node node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/services/ContainerItemComponentResourceTest-empty-component.xml");

        MultivaluedMap<String, String> params;

        // 1. add a non annotated parameter for 'default someNonAnnotatedParameter = lux
        params = new MetadataMap<String, String>();
        params.add("parameterOne", "bar");
        params.add("someNonAnnotatedParameter", "lux");

        HstComponentParameters componentParameters = new HstComponentParameters(node);
        containerItemComponentResource.doSetParameters(componentParameters, null, params, 0);

        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        // do not contain HST_PARAMETERNAMEPREFIXES
        assertTrue(!node.hasProperty(HST_PARAMETERNAMEPREFIXES));

        Map<String, String> defaultAnnotated =  ContainerItemComponentResource.getAnnotatedDefaultValues(node);
        assertTrue(defaultAnnotated.containsKey("parameterOne"));
        assertEquals(defaultAnnotated.get("parameterOne"), "");
        assertTrue(defaultAnnotated.containsKey("parameterTwo"));
        assertEquals(defaultAnnotated.get("parameterTwo"), "test");


        Set<String> variants =  new ContainerItemComponentResource().doGetVariants(node);
        assertTrue(variants.size() == 1);
        assertTrue(variants.contains("hippo-default"));

        // 2. create a new variant 'lux' : The creation of the variant should 
        // pick up the explicitly defined parameters from 'default' that are ALSO annotated (thus parameterOne, and NOT someNonAnnotatedParameter) PLUS
        // the implicit parameters from the DummyInfo (parameterTwo but not parameterOne because already from 'default')

        containerItemComponentResource.doCreateVariant(node, new HstComponentParameters(node), "newvar", 0);
        assertTrue(node.hasProperty(HST_PARAMETERNAMES));
        assertTrue(node.hasProperty(HST_PARAMETERVALUES));
        // now it must contain HST_PARAMETERNAMEPREFIXES
        assertTrue(node.hasProperty(HST_PARAMETERNAMEPREFIXES));

        variants = new ContainerItemComponentResource().doGetVariants(node);
        assertTrue(variants.size() == 2);
        assertTrue(variants.contains("hippo-default"));
        assertTrue(variants.contains("newvar"));

        componentParameters = new HstComponentParameters(node);
        assertTrue(componentParameters.hasParameter("newvar", "parameterOne"));
        assertEquals("bar", componentParameters.getValue("newvar", "parameterOne"));
        assertTrue(componentParameters.hasParameter("newvar", "parameterTwo"));
        // from  @Parameter(name = "parameterTwo", required = true, defaultValue = "test")
        assertEquals("test", componentParameters.getValue("newvar", "parameterTwo"));
        assertFalse(componentParameters.hasParameter("newvar", "someNonAnnotatedParameter"));

        // 3. try to remove the new variant
        containerItemComponentResource.doDeleteVariant(new HstComponentParameters(node), "newvar", 0);
        variants = new ContainerItemComponentResource().doGetVariants(node);
        assertTrue(variants.size() == 1);
        assertTrue(variants.contains("hippo-default"));

        // 4. try to remove the 'default' variant : this should not be allowed
        boolean removeSucceeded = true;
        try {
            containerItemComponentResource.doDeleteVariant(new HstComponentParameters(node), "hippo-default", 0);
            fail("Default variant should not be possible to be removed");
        } catch (IllegalStateException e) {
            removeSucceeded = false;
        }
        assertFalse("Remove should not have succeeded", removeSucceeded);
    }

}
