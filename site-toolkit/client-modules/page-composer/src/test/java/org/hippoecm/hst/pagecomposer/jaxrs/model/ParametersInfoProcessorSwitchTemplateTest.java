/*
 *  Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.property.SwitchTemplatePropertyRepresentationFactory;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockBinary;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;

import com.google.common.collect.ImmutableSet;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl.TEMPLATE_PARAM_NAME;
import static org.hippoecm.hst.core.container.ContainerConstants.DEFAULT_PARAMETER_PREFIX;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.PREVIEW_EDITING_HST_MODEL_ATTR;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ParametersInfoProcessorSwitchTemplateTest extends AbstractTestParametersInfoProcessor {

    protected MockNode layoutFileNode;
    private static final ImmutableSet<String> FTL = ImmutableSet.of(".ftl");
    @Before
    @Override
    public void setup() throws RepositoryException {
        super.setup();

        mockHstComponentConfiguration.setRenderPath(ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL + "/ftl/main/layout.ftl");
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[]{"bar"});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[]{"barValue"});

        HstRequestContext hstRequestContext = createNiceMock(HstRequestContext.class);
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        Mount mount = createNiceMock(Mount.class);
        expect(mount.getContextPath()).andReturn("site");
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(hstRequestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();

        final InternalHstModel hstModel = createNiceMock(InternalHstModel.class);
        final VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        expect(hstRequestContext.getAttribute(eq(PREVIEW_EDITING_HST_MODEL_ATTR))).andStubReturn(hstModel);

        expect(hstModel.getVirtualHosts()).andStubReturn(virtualHosts);
        expect(virtualHosts.getContextPath()).andStubReturn("/site");

        replay(hstRequestContext, hstModel, virtualHosts, resolvedMount, mount);

        ModifiableRequestContextProvider.set(hstRequestContext);

        //   create:
        //  /webfiles/site/ftl/main/layout.ftl

        final MockNode rootNode = containerItemNode.getSession().getRootNode();
        layoutFileNode = rootNode.addNode("webfiles", "webfiles:webfiles")
                .addNode("site", "webfiles:bundle")
                .addNode("ftl", "nt:folder")
                .addNode("main", "nt:folder")
                .addNode("layout.ftl", "nt:file");
        layoutFileNode.addNode("jcr:content", "nt:resource");
    }

    @After
    public void tearDown() {
        ModifiableRequestContextProvider.clear();
    }


    @Test
    public void no_template_variants() throws RepositoryException {
        final MockHstComponentConfiguration component = createComponentReference();

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);

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

        final MockHstComponentConfiguration component = createComponentReference();
        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);

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
        assertEquals("webfile:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());

        final Map<String, String> sortedMap = SwitchTemplatePropertyRepresentationFactory.asKeySortedMap(
                new String[] {
                        "layout.ftl",
                        "layout-variant1.ftl",
                        "layout-variant2.ftl"},
                new String[]{
                        "webfile:/ftl/main/layout.ftl",
                        "webfile:/ftl/main/layout/layout-variant1.ftl",
                        "webfile:/ftl/main/layout/layout-variant2.ftl"}, FTL);

        String[] expectedValues = sortedMap.values().toArray(new String[0]);
        String[] expectedDisplayValues = sortedMap.keySet().toArray(new String[0]);

        assertArrayEquals(expectedValues, switchTemplateProp.getDropDownListValues());

        assertArrayEquals(expectedDisplayValues, switchTemplateProp.getDropDownListDisplayValues());

        final ContainerItemComponentPropertyRepresentation barProp = properties.get(1);
        assertEquals("bar", barProp.getName());
        assertEquals("barValue",barProp.getValue());
    }

    @Test
    public void existing_template_variants_no_i18n_bundles() throws RepositoryException {
        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        Locale[] locales = new Locale[]{null, Locale.FRENCH, Locale.FRANCE};
        final MockHstComponentConfiguration component = createComponentReference();
        for (Locale locale : locales) {
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo.type(), locale, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, component, helper, propertyPresentationFactories);

            String[] expectedSortedDisplayValues = {
                    "layout.ftl",
                    "layout-variant1.ftl",
                    "layout-variant2.ftl"};

            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertArrayEquals(expectedSortedDisplayValues, switchTemplateProp.getDropDownListDisplayValues());
        }
    }

    @Test
    public void existing_template_variants_with_i18n_resource_bundle() throws RepositoryException, IOException {

        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        for (String locale : new String[]{"", "_fr", "_fr_FR"}) {
            final Node propertiesNode = mainFolder.addNode(String.format("layout%s.properties", locale), JcrConstants.NT_FILE);
            final Node content = propertiesNode.addNode("jcr:content", JcrConstants.NT_RESOURCE);
            final InputStream i18nProperties = ParametersInfoProcessorSwitchTemplateTest.class.getClassLoader()
                    .getResourceAsStream(String.format("org/hippoecm/hst/pagecomposer/jaxrs/model/layout%s.properties", locale));
            MockBinary binary = new MockBinary(i18nProperties);
            content.setProperty(JcrConstants.JCR_DATA, binary);
        }

        Locale[] locales = new Locale[]{null, new Locale(""), Locale.FRENCH, Locale.FRANCE};
        final MockHstComponentConfiguration component = createComponentReference();
        for (Locale locale : locales) {
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo.type(), locale, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, component, helper, propertyPresentationFactories);

            String[] expectedSortedDisplayValues = {
                    "Layout",
                    "Layout Variant 1",
                    "Layout Variant 2" };

            if (locale == Locale.FRENCH || locale == Locale.FRANCE) {
                for (int i = 0; i < 3; i++) {
                    expectedSortedDisplayValues[i] = expectedSortedDisplayValues[i] + " ("+locale.toString()+")";
                }
            }

            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertArrayEquals(expectedSortedDisplayValues, switchTemplateProp.getDropDownListDisplayValues());
        }

    }

    @Test
    public void variants_not_of_type_ftl_ignored() throws RepositoryException {
        // add variant that is not of
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.XXX","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.YYY","nt:file").addNode("jcr:content", "nt:resource");

        final MockHstComponentConfiguration component = createComponentReference();
        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);
        // because *no* template variants that end with .ftl are present, no 'switchTemplate' ContainerItemComponentPropertyRepresentation
        // is avaible
        assertEquals(1, properties.size());


        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");
        List<ContainerItemComponentPropertyRepresentation> propertiesNew =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);

        assertEquals(2, propertiesNew.size());
    }

    @Test
    public void template_variant_configured_and_exists() throws RepositoryException {

        // set parameter for template to point to webfile:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webfile:/ftl/main/layout/layout-variant1.ftl"});

        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        final MockHstComponentConfiguration component = createComponentReference();

        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);

        // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
        final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
        assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
        assertEquals("webfile:/ftl/main/layout/layout-variant1.ftl", switchTemplateProp.getValue());
        assertEquals("webfile:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());
    }

    @Test
    public void template_variant_configured_does_not_exist_gets_added_as_first_to_dropdown() throws RepositoryException {
        // set parameter for template to point to webfile:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webfile:/ftl/main/layout/non-existing.ftl"});

        // add variants
        final MockNode mainFolder = layoutFileNode.getParent();
        final MockNode layoutFolder = mainFolder.addNode("layout", "nt:folder");
        layoutFolder.addNode("layout-variant1.ftl","nt:file").addNode("jcr:content", "nt:resource");
        layoutFolder.addNode("layout-variant2.ftl","nt:file").addNode("jcr:content", "nt:resource");

        final MockHstComponentConfiguration component = createComponentReference();
        List<ContainerItemComponentPropertyRepresentation> properties =
                getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);


        // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
        final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
        assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
        assertEquals("webfile:/ftl/main/layout/non-existing.ftl", switchTemplateProp.getValue());
        assertEquals("webfile:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());

        String[] expectedSortedValues = {
                "webfile:/ftl/main/layout/non-existing.ftl",
                "webfile:/ftl/main/layout.ftl",
                "webfile:/ftl/main/layout/layout-variant1.ftl",
                "webfile:/ftl/main/layout/layout-variant2.ftl",
                };

        assertArrayEquals(expectedSortedValues, switchTemplateProp.getDropDownListValues());

        String[] expectedSortedDisplayValues = {
                "Missing template 'non-existing.ftl'",
                "layout.ftl",
                "layout-variant1.ftl",
                "layout-variant2.ftl"};

        assertArrayEquals(expectedSortedDisplayValues, switchTemplateProp.getDropDownListDisplayValues());

        // and for NL
        List<ContainerItemComponentPropertyRepresentation> propertiesNL =
                getPopulatedProperties(parameterInfo.type(), new Locale("nl"), null, DEFAULT_PARAMETER_PREFIX,
                        containerItemNode, component, helper, propertyPresentationFactories);

        final ContainerItemComponentPropertyRepresentation switchTemplatePropNL = propertiesNL.get(0);
        String[] expectedDisplayValuesNL = {
                "Ontbrekende template 'non-existing.ftl'",
                "layout.ftl",
                "layout-variant1.ftl",
                "layout-variant2.ftl"};
        assertArrayEquals(expectedDisplayValuesNL, switchTemplatePropNL.getDropDownListDisplayValues());
    }

    @Test
    public void template_variant_configured_but_no_variants_present_any_more_still_results_in_switch_template_property() throws RepositoryException {
        // set parameter for template to point to webfile:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webfile:/ftl/main/layout/non-existing.ftl"});

        final MockHstComponentConfiguration component = createComponentReference();

        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                // add variants folder only this run
                final MockNode mainFolder = layoutFileNode.getParent();
                mainFolder.addNode("layout", "nt:folder");
                // no variants added
            }

            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, component, helper, propertyPresentationFactories);

            // even though there are no variant ftl templates, we still have the switchTemplateProp because there is a
            // value for TEMPLATE_PARAM_NAME param. If in this case we would not add a switchTemplateProp, then from the
            // channel mngr  not the default template can be selected any more.
            assertEquals(2, properties.size());

            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);

            assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
            assertEquals("webfile:/ftl/main/layout/non-existing.ftl", switchTemplateProp.getValue());
            assertEquals("webfile:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());

            String[] expectedValues = {
                    "webfile:/ftl/main/layout/non-existing.ftl",
                    "webfile:/ftl/main/layout.ftl"};

            assertArrayEquals(expectedValues, switchTemplateProp.getDropDownListValues());

            String[] expectedSortedDisplayValues = {
                    "Missing template 'non-existing.ftl'",
                    "layout.ftl"};

            assertArrayEquals(expectedSortedDisplayValues, switchTemplateProp.getDropDownListDisplayValues());
        }
    }


    @Test
    public void template_variant_configured_does_not_exist_but_still_in_i18n_resource_bundle() throws RepositoryException, IOException {
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{"bar", TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{"barValue", "webfile:/ftl/main/layout/non-existing.ftl"});

        final MockNode mainFolder = layoutFileNode.getParent();
        for (String locale : new String[]{"", "_fr", "_fr_FR"}) {
            final Node propertiesNode = mainFolder.addNode(String.format("layout%s.properties", locale), JcrConstants.NT_FILE);
            final Node content = propertiesNode.addNode("jcr:content", JcrConstants.NT_RESOURCE);
            final InputStream i18nProperties = ParametersInfoProcessorSwitchTemplateTest.class.getClassLoader()
                    .getResourceAsStream(String.format("org/hippoecm/hst/pagecomposer/jaxrs/model/layout%s.properties", locale));
            MockBinary binary = new MockBinary(i18nProperties);
            content.setProperty(JcrConstants.JCR_DATA, binary);
        }

        Locale[] locales = new Locale[]{null, new Locale(""), Locale.FRENCH, Locale.FRANCE};

        final MockHstComponentConfiguration component = createComponentReference();

        for (Locale locale : locales) {
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo.type(), locale, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, component, helper, propertyPresentationFactories);

            final String[] expectedSortedDisplayValues;


            if (locale == Locale.FRENCH) {
                final String[] values  = {
                        "Modèle manquant 'Non Existing (fr)'",
                        "Layout (fr)"};
                expectedSortedDisplayValues = values;
            } else if (locale == Locale.FRANCE) {
                final String[] values  = {
                        "Modèle manquant 'Non Existing (fr_FR)'",
                        "Layout (fr_FR)"};
                expectedSortedDisplayValues = values;
            } else {
                final String[] values  = {
                        "Missing template 'Non Existing'",
                        "Layout"};
                expectedSortedDisplayValues = values;
            }
            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertArrayEquals(expectedSortedDisplayValues, switchTemplateProp.getDropDownListDisplayValues());
        }

    }


    @Test
    public void multi_prefix_template_variant_configured_and_exists() throws RepositoryException {

        // set parameter for template to point to webfile:/ftl/main/layout/layout-variant1.ftl
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[]{
                        "bar",
                        TEMPLATE_PARAM_NAME,
                        "bar",
                        TEMPLATE_PARAM_NAME});
        containerItemNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[]{
                        "barValue",
                        "webfile:/ftl/main/layout/layout-variant1.ftl",
                        "barrrrrValue",
                        "webfile:/ftl/main/layout/layout-variant2.ftl"
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

        final MockHstComponentConfiguration component = createComponentReference();
        {
            // DEFAULT PREFIX
            String prefix = null;
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo.type(), null, null, DEFAULT_PARAMETER_PREFIX,
                            containerItemNode, component, helper, propertyPresentationFactories);

            // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
            assertEquals("webfile:/ftl/main/layout/layout-variant1.ftl", switchTemplateProp.getValue());
            assertEquals("webfile:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());
        }
        {
            // "some-prefix"
            String prefix = null;
            List<ContainerItemComponentPropertyRepresentation> properties =
                    getPopulatedProperties(parameterInfo.type(), null, null, "some-prefix",
                            containerItemNode, component, helper, propertyPresentationFactories);

            // 'switchTemplate' ContainerItemComponentPropertyRepresentation is *always* the first
            final ContainerItemComponentPropertyRepresentation switchTemplateProp = properties.get(0);
            assertEquals(TEMPLATE_PARAM_NAME, switchTemplateProp.getName());
            assertEquals("webfile:/ftl/main/layout/layout-variant2.ftl", switchTemplateProp.getValue());
            assertEquals("webfile:/ftl/main/layout.ftl", switchTemplateProp.getDefaultValue());
        }
    }

}
