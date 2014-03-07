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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.JAXBException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HstComponentParametersTest {

    private ContainerItemHelper helper;

    @Before
    public void setUp() {
        helper = new ContainerItemHelper();
        final PageComposerContextService pageComposerContextService = new PageComposerContextService();
        helper.setPageComposerContextService(pageComposerContextService);
    }

    @Test
    public void emptyNode() throws RepositoryException, JAXBException, IOException {


        MockNode emptyNode = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-empty.xml");
        HstComponentParameters parameters = new HstComponentParameters(emptyNode, helper);

        assertFalse(parameters.hasPrefix(null));
        assertFalse(parameters.hasPrefix(""));
        assertFalse(parameters.hasPrefix("hippo-default"));
        assertFalse(parameters.hasPrefix("prefix"));
    }

    @Test
    public void onlyParameterNamesAndValuesHaveDefaultPrefix() throws RepositoryException, JAXBException, IOException {
        MockNode nodeWithoutPrefixes = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-no-prefixes-two-parameters.xml");
        HstComponentParameters parameters = new HstComponentParameters(nodeWithoutPrefixes, helper);

        assertTrue(parameters.hasPrefix(null));
        assertTrue(parameters.hasPrefix(""));
        assertTrue(parameters.hasPrefix("hippo-default"));
        assertFalse(parameters.hasPrefix("prefix"));

        assertEquals("valueOne", parameters.getValue(null, "parameterOne"));
        assertEquals("valueOne", parameters.getValue("", "parameterOne"));
        assertEquals("valueOne", parameters.getValue("hippo-default", "parameterOne"));
        assertEquals("valueTwo", parameters.getValue(null, "parameterTwo"));
        assertEquals("valueTwo", parameters.getValue("", "parameterTwo"));
        assertEquals("valueTwo", parameters.getValue("hippo-default", "parameterTwo"));
    }

    @Test
    public void prefixesAreRead() throws RepositoryException, JAXBException, IOException {
        MockNode nodeWithPrefixes = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-default-and-prefix-one-parameter.xml");
        HstComponentParameters parameters = new HstComponentParameters(nodeWithPrefixes, helper);

        assertTrue(parameters.hasPrefix(null));
        assertTrue(parameters.hasPrefix(""));
        assertTrue(parameters.hasPrefix("hippo-default"));
        assertTrue(parameters.hasPrefix("prefix"));

        assertEquals("defaultValue", parameters.getValue(null, "parameterOne"));
        assertEquals("defaultValue", parameters.getValue("", "parameterOne"));
        assertEquals("defaultValue", parameters.getValue("hippo-default", "parameterOne"));
        assertEquals("prefixValue", parameters.getValue("prefix", "parameterOne"));
    }

    @Test
    public void noPrefixesSaveWithoutChangesKeepsOldValuesAsIs() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-no-prefixes-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.save(0);

        assertHasPrefixesNamesValues(node, false, true, true);

        String[] expectedNames = {"parameterOne"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueOne"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());

        assertFalse(node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));
    }

    @Test
    public void noPrefixesAddFirstParameter() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-empty.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.setValue("hippo-default", "parameterOne", "bar");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, false, true, true);

        String[] expectedNames = {"parameterOne"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"bar"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    @Test
    public void noPrefixesAddSecondParameter() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-no-prefixes-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.setValue("", "parameterTwo", "valueTwo");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, false, true, true);

        String[] expectedNames = {"parameterOne", "parameterTwo"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueOne", "valueTwo"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());

        assertFalse(node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));
    }

    @Test
    public void prefixesAddFirstParameter() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-empty.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.setValue("prefix", "parameterOne", "valueOne");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, true, true, true);

        String[] expectedPrefixes = {"prefix"};
        assertValues(expectedPrefixes, node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues());

        String[] expectedNames = {"parameterOne"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueOne"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    @Test
    public void removingOnlyPrefixedParameterClearsNode() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-one-prefix-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.removePrefix("prefix");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, false, false, false);
    }

    @Test
    public void addPrefixedParameterIdenticalToDefaultValue() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-default-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.setValue("prefix", "parameterOne", "valueOne");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, true, true, true);

        String[] expectedPrefixes = {"", "prefix"};
        assertValues(expectedPrefixes, node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues());

        String[] expectedNames = {"parameterOne", "parameterOne"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueOne", "valueOne"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    @Test
    public void addingDifferentPrefixedParameter() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-no-prefixes-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.setValue("prefix", "parameterTwo", "valueTwo");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, true, true, true);

        String[] expectedPrefixes = {"", "prefix"};
        assertValues(expectedPrefixes, node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues());

        String[] expectedNames = {"parameterOne", "parameterTwo"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueOne", "valueTwo"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    @Test
    public void renamePrefix() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-one-prefix-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.renamePrefix("prefix", "foo");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, true, true, true);

        String[] expectedPrefixes = {"foo"};
        assertValues(expectedPrefixes, node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues());

        String[] expectedNames = {"parameterOne"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueOne"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRenameDefaultPrefix() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-default-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.renamePrefix("", "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRenamePrefixToDefault() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-one-prefix-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.renamePrefix("prefix", "");
    }

    @Test
    public void renamePrefixDoesNotChangeDefault() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-default-and-prefix-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        parameters.renamePrefix("prefix", "foo");
        parameters.save(0);

        assertHasPrefixesNamesValues(node, true, true, true);

        String[] expectedPrefixes = {"", "foo"};
        assertValues(expectedPrefixes, node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues());

        String[] expectedNames = {"parameterOne", "parameterOne"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"defaultValue", "prefixValue"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    @Test
    public void iteratedPrefixesCanBeRemoved() throws RepositoryException, JAXBException, IOException {
        MockNode node = MockNodeFactory.fromXml("/org/hippoecm/hst/pagecomposer/jaxrs/util/HstComponentParametersTest-two-prefixes-one-parameter.xml");

        HstComponentParameters parameters = new HstComponentParameters(node, helper);
        for (String prefix : parameters.getPrefixes()) {
            if (prefix.equals("prefixOne")) {
                parameters.removePrefix(prefix);
            }
        }
        parameters.save(0);

        assertHasPrefixesNamesValues(node, true, true, true);

        String[] expectedPrefixes = {"prefixTwo"};
        assertValues(expectedPrefixes, node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues());

        String[] expectedNames = {"parameter"};
        assertValues(expectedNames, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues());

        String[] expectedValues = {"valueTwo"};
        assertValues(expectedValues, node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues());
    }

    private void assertHasPrefixesNamesValues(final MockNode node, boolean hasPrefixes, boolean hasNames, boolean hasValues) {
        assertPresent("parameter prefixes", hasPrefixes, node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES));
        assertPresent("parameter names", hasNames, node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES));
        assertPresent("parameter values", hasValues, node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES));
    }

    private void assertPresent(String things, boolean expected, boolean actual) {
        String message = "There should be " + (expected ? "" : "no") + " " + things;
        assertTrue(message, expected == actual);
    }

    private void assertValues(String[] expected, final Value[] actual) throws RepositoryException {
        assertEquals("Wrong number of values", expected.length, actual.length);

        List<String> toExpect = new ArrayList<String>(Arrays.asList(expected));
        List<Value> toCheck = new ArrayList<Value>(Arrays.asList(actual));

        for (Value value : toCheck) {
            assertTrue("Expected values " + Arrays.toString(expected) + " but got " + valuesToString(actual),
                    toExpect.contains(value.getString()));
            toExpect.remove(value.toString());
        }
    }

    private String valuesToString(Value[] values) throws RepositoryException {
        StringBuilder b = new StringBuilder("[");
        String concat = "";
        for (Value value : values) {
            b.append(concat);
            b.append(value.getString());
            concat = ",";
        }
        b.append(']');
        return b.toString();
    }

}