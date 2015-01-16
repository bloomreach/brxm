/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_PARAMETER_PREFIX;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ParametersInfoProcessorSwitchTemplateTest extends ParametersInfoProcessorPopulatedPropertiesTest {

    protected MockNode layoutFileNode;

    @Before
    @Override
    public void setup() throws RepositoryException {
        super.setup();

        mockHstComponentConfiguration.setRenderPath(ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL + "/ftl/main/layout.ftl");
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue"});

        HstRequestContext hstRequestContext = createNiceMock(HstRequestContext.class);
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        Mount mount = createNiceMock(Mount.class);
        expect(mount.getContextPath()).andReturn("site");
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(hstRequestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        Object[] mocks = new Object[]{hstRequestContext, resolvedMount, mount};
        replay(mocks);
        ModifiableRequestContextProvider.set(hstRequestContext);

        //   create:
        //  /webresources/site/ftl/main/layout.ftl

        final MockNode rootNode = containerItemNode.getSession().getRootNode();
        layoutFileNode = rootNode.addNode("webresources", "webresources:webresources")
                .addNode("site", "webresources:bundle")
                .addNode("ftl", "nt:folder")
                .addNode("main", "nt:folder")
                .addNode("layout.ftl", "nt:file");
        layoutFileNode.addNode("jcr:content", "nt:resource");
    }

    @After
    public void tearDown()  {
        ModifiableRequestContextProvider.clear();
    }

    @Test
    public void no_template_variants() throws RepositoryException {

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);

        // because *no* template variants present, no 'switchTemplate' ContainerItemComponentPropertyRepresentation
        // is avaible
        assertEquals(1, properties.size());

        final ContainerItemComponentPropertyRepresentation prop = properties.get(0);
        assertEquals("bar", prop.getName());
        assertEquals("barValue",prop.getValue());

    }


    @Test
    public void existing_template_variants() throws RepositoryException {
        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);

        // because there are variants for templates available, there must be added a
        // 'switchTemplate' ContainerItemComponentPropertyRepresentation
        assertEquals(2, properties.size());

        // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
        final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
        assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());

        // no value set
        assertNull(switchTemplateProp.getValue());

        assertEquals("Template", switchTemplateProp.getLabel());
        assertEquals("Choose a template", switchTemplateProp.getGroupLabel());
        assertEquals("webresource:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());

        String[] expectedValues = {
                "webresource:/ftl/main/layout.ftl",
                "webresource:/ftl/main/layout/layout-variant1.ftl",
                "webresource:/ftl/main/layout/layout-variant2.ftl"};

        assertTrue(Arrays.equals(expectedValues, switchTemplateProp.getDropDownListValues()));

        String[] expectedDisplayValues = {
                "layout.ftl",
                "layout-variant1.ftl",
                "layout-variant2.ftl"};

        assertTrue(Arrays.equals(expectedDisplayValues, switchTemplateProp.getDropDownListDisplayValues()));

        final ContainerItemComponentPropertyRepresentation barProp = properties.get(1);
        assertEquals("bar", barProp.getName());
        assertEquals("barValue",barProp.getValue());
    }


    @Test
    public void existing_template_variants_with_i18n_resource_bundle() throws RepositoryException {
        // TODO i18n support!
        // TODO test also with locale support
    }

    @Test
    public void variants_not_of_type_ftl_ignored() throws RepositoryException {
        // add variant that is not of
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.XXX","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.YYY","nt:file").addNode("jcr:content", "nt:resource");

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);
        // because *no* template variants that end with .ftl are present, no 'switchTemplate' ContainerItemComponentPropertyRepresentation
        // is avaible
        assertEquals(1, properties.size());


        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");
        List<ContainerItemComponentPropertyRepresentation> propertiesNew =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);

        assertEquals(2, propertiesNew.size());
    }

    @Test
    public void template_variant_configured_and_exists() throws RepositoryException {

        // set parameter for template to point to webresource:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webresource:/ftl/main/layout/layout-variant1.ftl"});

        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);

        // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
        final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
        assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
        assertEquals("webresource:/ftl/main/layout/layout-variant1.ftl", switchTemplateProp.getValue());
        assertEquals("webresource:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());
    }

    @Test
    public void template_variant_configured_does_not_exist_gets_added_as_first_to_dropdown() throws RepositoryException {
        // set parameter for template to point to webresource:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webresource:/ftl/main/layout/non-existing.ftl"});

        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);


        // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
        final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
        assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
        assertEquals("webresource:/ftl/main/layout/non-existing.ftl", switchTemplateProp.getValue());
        assertEquals("webresource:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());

        String[] expectedValues = {
                "webresource:/ftl/main/layout/non-existing.ftl",
                "webresource:/ftl/main/layout.ftl",
                "webresource:/ftl/main/layout/layout-variant1.ftl",
                "webresource:/ftl/main/layout/layout-variant2.ftl"};

        assertTrue(Arrays.equals(expectedValues, switchTemplateProp.getDropDownListValues()));

        String[] expectedDisplayValues = {
                "Missing template 'non-existing.ftl'",
                "layout.ftl",
                "layout-variant1.ftl",
                "layout-variant2.ftl"};

        assertTrue(Arrays.equals(expectedDisplayValues, switchTemplateProp.getDropDownListDisplayValues()));

        // and for NL
        List<ContainerItemComponentPropertyRepresentation> propertiesNL =
                getPopulatedProperties(parameterInfo, new Locale("nl"), null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, helper);

        final ContainerItemComponentPropertyRepresentation switchTemplatePropNL = propertiesNL.get(0);
        String[] expectedDisplayValuesNL = {
                "Ontbrekend template 'non-existing.ftl'",
                "layout.ftl",
                "layout-variant1.ftl",
                "layout-variant2.ftl"};
        assertTrue(Arrays.equals(expectedDisplayValuesNL, switchTemplatePropNL.getDropDownListDisplayValues()));
    }

    @Test
    public void template_variant_configured_but_no_variants_present_any_more_still_results_in_switch_template_property() throws RepositoryException {
        // set parameter for template to point to webresource:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webresource:/ftl/main/layout/non-existing.ftl"});


        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                // add variants folder only this run
                final MockNode mainFolder = layoutFileNode.getParent();
                final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
                // no variants added
            }

            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, helper);

            // even though there are no variant ftl templates, we still have the switchTemplateProp because there is a
            // value for TEMPLATE_PARAM_NAME param. If in this case we would not add a switchTemplateProp, then from the
            // channel mngr  not the default template can be selected any more.
            assertEquals(2, properties.size());

            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);

            assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
            assertEquals("webresource:/ftl/main/layout/non-existing.ftl", switchTemplateProp.getValue());
            assertEquals("webresource:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());

            String[] expectedValues = {
                    "webresource:/ftl/main/layout/non-existing.ftl",
                    "webresource:/ftl/main/layout.ftl"};

            assertTrue(Arrays.equals(expectedValues, switchTemplateProp.getDropDownListValues()));

            String[] expectedDisplayValues = {
                    "Missing template 'non-existing.ftl'",
                    "layout.ftl"};

            assertTrue(Arrays.equals(expectedDisplayValues, switchTemplateProp.getDropDownListDisplayValues()));
        }
    }

    @Test
    public void multi_prefix_template_variant_configured_and_exists() throws RepositoryException {

        // set parameter for template to point to webresource:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{
                        "bar",
                        TEMPLATE_PARAM_NAME,
                        "bar",
                        TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{
                        "barValue",
                        "webresource:/ftl/main/layout/layout-variant1.ftl",
                        "barrrrrValue",
                        "webresource:/ftl/main/layout/layout-variant2.ftl"
                 });
        containerItemNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES,
                new String[]{
                        "",
                        "",
                        "some-prefix",
                        "some-prefix",
                });

        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        {
            // DEFAULT PREFIX
            String prefix = null;
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo, null, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, helper);

            // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
            assertEquals("webresource:/ftl/main/layout/layout-variant1.ftl", switchTemplateProp.getValue());
            assertEquals("webresource:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());
        }
        {
            // "some-prefix"
            String prefix = null;
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo, null, null, "some-prefix",
                            containerItemNode, helper);

            // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
            assertEquals("webresource:/ftl/main/layout/layout-variant2.ftl", switchTemplateProp.getValue());
            assertEquals("webresource:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());
        }
    }

}
