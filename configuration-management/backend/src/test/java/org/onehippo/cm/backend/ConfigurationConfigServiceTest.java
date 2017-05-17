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
package org.onehippo.cm.backend;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.testutils.jcr.event.ExpectedEvents;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests related to the handling of nodes and properties.
 */
public class ConfigurationConfigServiceTest extends BaseConfigurationConfigServiceTest {

    /**
     * Primary type changes:
     */

    @Test
    public void expect_primary_type_to_be_updated() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /change-type:\n"
                + "        jcr:primaryType: nt:unstructured";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /change-type:\n"
                + "        jcr:primaryType: hippo:document";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/change-type/jcr:primaryType")
                .expectPropertyAdded("/test/change-type/hippo:paths");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[keep-as-is, change-type]", "[jcr:primaryType]");
        expectNode("/test/keep-as-is", "[]", "[jcr:primaryType]");
        expectProp("/test/keep-as-is/jcr:primaryType", PropertyType.NAME, "nt:unstructured");
        expectNode("/test/change-type", "[]", "[hippo:paths, jcr:primaryType]");
        expectProp("/test/change-type/jcr:primaryType", PropertyType.NAME, "hippo:document");
    }

    @Test
    public void expect_tweaked_primary_type_to_persist_across_non_forced_bootstrap() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the primary type
        testNode.getNode("keep-as-is").setPrimaryType("hippo:document");
        session.save();

        final ExpectedEvents expectedEvents = new ExpectedEvents(); // no events expected

        applyDefinitions(baselineSource, baseline, expectedEvents);

        assertEquals(testNode.getNode("keep-as-is").getPrimaryNodeType().getName(), "hippo:document");
    }

    @Test
    public void expect_tweaked_primary_type_to_be_reset_on_forced_bootstrap() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the primary type
        testNode.getNode("keep-as-is").setPrimaryType("hippo:document");
        session.save();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(baselineSource, baseline, true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("[OVERRIDE] Primary type 'hippo:document' of node '/test/keep-as-is' is adjusted to 'nt:unstructured' as defined in [test-group/test-project/test-module-0 [string]].")));
        }

        assertEquals(testNode.getNode("keep-as-is").getPrimaryNodeType().getName(), "nt:unstructured");
    }

    @Test
    public void expect_tweaked_primary_type_to_be_reset_by_baseline_change() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /change-type:\n"
                + "        jcr:primaryType: nt:unstructured\n";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the primary type
        testNode.getNode("change-type").setPrimaryType("hippo:handle");
        session.save();

        final String updateSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /change-type:\n"
                + "        jcr:primaryType: hippo:document\n";
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(updateSource, baseline);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("[OVERRIDE] Primary type 'hippo:handle' of node '/test/change-type' has been changed from 'nt:unstructured'.Overriding to type 'hippo:document', defined in [test-group/test-project/test-module-0 [string]].")));
        }

        assertEquals("hippo:document", testNode.getNode("change-type").getPrimaryNodeType().getName());
    }

    /**
     * Mixin changes:
     */

    @Test
    public void expect_mixin_types_to_be_updated() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']\n"
                + "      /remove-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']\n"
                + "      /change-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']\n"
                + "      /add-mixin:\n"
                + "        jcr:primaryType: nt:unstructured";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']\n"
                + "      /remove-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /change-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['hippostd:relaxed']\n"
                + "      /add-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyRemoved("/test/remove-mixin/jcr:mixinTypes")
                .expectPropertyChanged("/test/change-mixin/jcr:mixinTypes")
                .expectPropertyAdded("/test/add-mixin/jcr:mixinTypes");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[keep-as-is, remove-mixin, change-mixin, add-mixin]", "[jcr:primaryType]");
        expectNode("/test/keep-as-is", "[]", "[jcr:mixinTypes, jcr:primaryType]");
        expectProp("/test/keep-as-is/jcr:mixinTypes", PropertyType.NAME, "[mix:language]");
        expectNode("/test/remove-mixin", "[]", "[jcr:primaryType]");
        expectNode("/test/change-mixin", "[]", "[jcr:mixinTypes, jcr:primaryType]");
        expectProp("/test/change-mixin/jcr:mixinTypes", PropertyType.NAME, "[hippostd:relaxed]");
        expectNode("/test/add-mixin", "[]", "[jcr:mixinTypes, jcr:primaryType]");
        expectProp("/test/add-mixin/jcr:mixinTypes", PropertyType.NAME, "[mix:language]");
    }

    @Test
    public void expect_tweaked_mixin_to_persist_across_non_forced_bootstrap() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the primary type
        testNode.getNode("keep-as-is").addMixin("hippostd:relaxed");
        session.save();

        ExpectedEvents expectedEvents = new ExpectedEvents(); // no events expected
        applyDefinitions(baselineSource, baseline, expectedEvents);

        assertTrue(hasMixins(testNode.getNode("keep-as-is"), Arrays.asList("mix:language", "hippostd:relaxed")));

        // tweak again
        testNode.getNode("keep-as-is").removeMixin("mix:language");
        session.save();

        expectedEvents = new ExpectedEvents(); // no events expected
        applyDefinitions(baselineSource, baseline, expectedEvents);

        assertTrue(hasMixins(testNode.getNode("keep-as-is"), Collections.singletonList("hippostd:relaxed")));

        // and again
        testNode.getNode("keep-as-is").removeMixin("hippostd:relaxed");
        session.save();

        expectedEvents = new ExpectedEvents(); // no events expected
        applyDefinitions(baselineSource, baseline, expectedEvents);

        assertTrue(hasMixins(testNode.getNode("keep-as-is"), Collections.emptyList()));
    }

    @Test
    public void expect_tweaked_mixin_to_be_reset_on_forced_bootstrap() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /tweak-mixins:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the mixin
        testNode.getNode("tweak-mixins").addMixin("hippostd:relaxed");
        testNode.getNode("tweak-mixins").removeMixin("mix:language");
        session.save();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(baselineSource, baseline, true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals(
                    "[OVERRIDE] Mixin 'hippostd:relaxed' has been added to node '/test/tweak-mixins', " +
                            "but is removed because it is not present in definition [test-group/test-project/test-module-0 [string]].")));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals(
                    "[OVERRIDE] Mixin 'mix:language' has been removed from node '/test/tweak-mixins', " +
                            "but is re-added because it is defined at [test-group/test-project/test-module-0 [string]].")));
        }

        assertTrue(hasMixins(testNode.getNode("tweak-mixins"), Collections.singletonList("mix:language")));
    }

    @Test
    public void expect_manually_removed_mixin_to_be_ignored_when_trying_to_delete_it() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /remove-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the primary type
        testNode.getNode("remove-mixin").removeMixin("mix:language");
        session.save();

        final String updateSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /remove-mixin:\n"
                + "        jcr:primaryType: nt:unstructured";
        applyDefinitions(updateSource, baseline);

        assertTrue(hasMixins(testNode.getNode("remove-mixin"), Collections.emptyList()));
    }

    private boolean hasMixins(final Node node, final List<String> mixins) throws Exception {
        NodeType[] mixinTypes = node.getMixinNodeTypes();
        if (mixinTypes.length != mixins.size()) {
            return false;
        }
        for (NodeType mixinType : mixinTypes) {
            if (!mixins.contains(mixinType.getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Regular properties
     */

    @Test
    public void expect_tweaked_properties_to_be_untouched() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]\n"
                + "";

        applyDefinitions(source, baseline, new ExpectedEvents());

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "org");
        expectProp("/test/multiple", PropertyType.STRING, "[org1, org2]");
    }

    @Test
    public void expect_unchanged_existing_properties_to_persist_across_non_forced_bootstrap() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the properties
        testNode.setProperty("single", "new");
        testNode.setProperty("multiple", new String[] {"new1"});
        session.save();

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]\n"
                + "";

        applyDefinitions(source, baseline, new ExpectedEvents());

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "new");
        expectProp("/test/multiple", PropertyType.STRING, "[new1]");
    }

    @Test
    public void expect_forced_bootstrap_to_override_tweaked_properties() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the properties
        testNode.setProperty("single", "new");
        testNode.setProperty("multiple", new String[] {"new1"});
        session.save();

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]\n"
                + "";

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(source, baseline, true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals(
                    "[OVERRIDE] Property '/test/single' has been changed in the repository, " +
                            "and will be overridden due to definition [test-group/test-project/test-module-0 [string]].")));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals(
                    "[OVERRIDE] Property '/test/multiple' has been changed in the repository, " +
                            "and will be overridden due to definition [test-group/test-project/test-module-0 [string]].")));
        }

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "org");
        expectProp("/test/multiple", PropertyType.STRING, "[org1, org2]");
    }

    @Test
    public void expect_tweaked_property_to_stay_unchanged_if_it_already_has_the_new_value() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        // tweak the properties
        testNode.setProperty("single", "new");
        testNode.setProperty("multiple", new String[] {"new1"});
        session.save();

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: new\n"
                + "      multiple: [new1]";

        applyDefinitions(source, baseline);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "new");
        expectProp("/test/multiple", PropertyType.STRING, "[new1]");
    }

    @Test
    public void expect_new_properties_to_be_created() throws Exception {
        // no initial content

        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: new\n"
                + "      multiple: [new1, new2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/single")
                .expectPropertyAdded("/test/multiple");

        applyDefinitions(definition, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "new");
        expectProp("/test/multiple", PropertyType.STRING, "[new1, new2]");
    }

    @Test
    public void expect_updated_properties_to_be_updated() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]\n"
                + "      reordered: [new2, new1]";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: new\n"
                + "      multiple: [new1, new2]\n"
                + "      reordered: [new1, new2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/single")
                .expectPropertyChanged("/test/multiple")
                .expectPropertyChanged("/test/reordered");

        applyDefinitions(definition, baseline, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, reordered, single]");
        expectProp("/test/single", PropertyType.STRING, "new");
        expectProp("/test/multiple", PropertyType.STRING, "[new1, new2]");
        expectProp("/test/reordered", PropertyType.STRING, "[new1, new2]");
    }

    @Test
    public void expect_deleted_properties_to_be_gone() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      explicitly-deleted: value";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        setProperty("/test", "not-in-config", PropertyType.STRING, "value");

        final String definition1
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      explicitly-deleted: value\n"
                + "      explicitly-deleted-non-existing: value\n"
                + "";
        final String definition2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      explicitly-deleted:\n"
                + "        operation: delete\n"
                + "      explicitly-deleted-non-existing:\n"
                + "        operation: delete\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyRemoved("/test/explicitly-deleted");

        applyDefinitions(new String[]{definition1,definition2}, baseline, false, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, not-in-config]");
    }

    @Test
    public void expect_propertytype_overrides_to_be_applied_if_necessary() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      incorrect-should-be-single: [org1, org2]\n"
                + "      incorrect-should-be-long: [org1, org2]\n"
                + "      already-changed-to-single: new\n"
                + "      already-changed-to-long: [42, 31415]";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String definition1
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      incorrect-should-be-single: [org1, org2]\n"
                + "      incorrect-should-be-long: [org1, org2]\n"
                + "      already-changed-to-single: [org1, org2]\n"
                + "      already-changed-to-long: [org1, org2]\n"
                + "      not-yet-existing: [org1, org2]\n"
                + "";
        final String definition2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      incorrect-should-be-single:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "      incorrect-should-be-long:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: [42, 31415]\n"
                + "      already-changed-to-single:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "      already-changed-to-long:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: [42, 31415]\n"
                + "      not-yet-existing:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/incorrect-should-be-single")
                .expectPropertyChanged("/test/incorrect-should-be-long")
                .expectPropertyAdded("/test/not-yet-existing");

        applyDefinitions(new String[]{definition1,definition2}, baseline, false, expectedEvents);

        expectNode("/test", "[]", "[already-changed-to-long, already-changed-to-single, incorrect-should-be-long, "
                + "incorrect-should-be-single, jcr:primaryType, not-yet-existing]");
        expectProp("/test/incorrect-should-be-single", PropertyType.STRING, "new");
        expectProp("/test/incorrect-should-be-long", PropertyType.LONG, "[42, 31415]");
        expectProp("/test/already-changed-to-single", PropertyType.STRING, "new");
        expectProp("/test/already-changed-to-long", PropertyType.LONG, "[42, 31415]");
        expectProp("/test/not-yet-existing", PropertyType.STRING, "new");
    }

    @Test
    public void expect_all_value_types_to_be_handled_correctly() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      string: hello world\n"
                + "      binary: !!binary |-\n"
                + "        aGVsbG8gd29ybGQ=\n"
                + "      long: 42\n"
                + "      double: 3.1415\n"
                + "      date: 2015-10-21T07:28:00.000+08:00\n"
                + "      boolean: true\n"
                + "      name:\n"
                + "          type: name\n"
                + "          value: nt:unstructured\n"
                + "      path:\n"
                + "          type: path\n"
                + "          value: /path/to/something\n"
                + "      reference:\n"
                + "          type: reference\n"
                + "          value: cafebabe-cafe-babe-cafe-babecafebabe\n"
                + "      weakreference:\n"
                + "          type: weakreference\n"
                + "          value: cafebabe-cafe-babe-cafe-babecafebabe\n"
                + "      uri:\n"
                + "          type: uri\n"
                + "          value: http://onehippo.org\n"
                + "      decimal:\n"
                + "          type: decimal\n"
                + "          value: '31415926535897932384626433832795028841971'\n"
                + "";

        ConfigurationModel baseline = applyDefinitions(definition); // ignore all events

        expectProp("/test/string", PropertyType.STRING, "hello world");
        expectProp("/test/binary", PropertyType.BINARY, "hello world");
        expectProp("/test/long", PropertyType.LONG, "42");
        expectProp("/test/double", PropertyType.DOUBLE, "3.1415");
        expectProp("/test/date", PropertyType.DATE, "2015-10-21T07:28:00.000+08:00");
        expectProp("/test/boolean", PropertyType.BOOLEAN, "true");
        expectProp("/test/name", PropertyType.NAME, "nt:unstructured");
        expectProp("/test/path", PropertyType.PATH, "/path/to/something");
        expectProp("/test/reference", PropertyType.REFERENCE, "cafebabe-cafe-babe-cafe-babecafebabe");
        expectProp("/test/weakreference", PropertyType.WEAKREFERENCE, "cafebabe-cafe-babe-cafe-babecafebabe");
        expectProp("/test/uri", PropertyType.URI, "http://onehippo.org");
        expectProp("/test/decimal", PropertyType.DECIMAL, "31415926535897932384626433832795028841971");

        // when applying the same definition again, expect no events
        final ExpectedEvents expectedEvents = new ExpectedEvents();
        applyDefinitions(definition, baseline, expectedEvents);
    }

    @Test
    public void expect_resources_are_loaded() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      string:\n"
                + "        type: string\n"
                + "        resource: folder/string1.txt\n"
                + "      binary:\n"
                + "        type: binary\n"
                + "        resource: folder/binary1.bin\n"
                + "";

        ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/string")
                .expectPropertyAdded("/test/binary");

        ConfigurationModel baseline = applyDefinitions(definition, expectedEvents);

        expectProp("/test/string", PropertyType.STRING, "string1");
        expectProp("/test/binary", PropertyType.BINARY, "binary1");

        // when applying the same definition again, expect no events
        expectedEvents = new ExpectedEvents();
        applyDefinitions(definition, baseline, expectedEvents);
    }

    @Test
    public void expect_value_add_on_resource_to_work() throws Exception {
        final String definition1
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      string:\n"
                + "        type: string\n"
                + "        resource: [folder/string1.txt]\n"
                + "";
        final String definition2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      string:\n"
                + "        operation: add\n"
                + "        resource: [folder/string1.txt]\n"
                + "";

        ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/string");

        ConfigurationModel baseline = applyDefinitions(new String[]{definition1,definition2}, expectedEvents);

        expectProp("/test/string", PropertyType.STRING,"[string1, string1]");

        // when applying the same definition again, expect no events
        expectedEvents = new ExpectedEvents();
        applyDefinitions(new String[]{definition1,definition2}, baseline, false, expectedEvents);
    }

    @Test
    public void expect_path_references_to_be_resolved() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      absolute:\n"
                + "        type: reference\n"
                + "        path: /test/foo/bar\n"
                + "      relative:\n"
                + "        type: reference\n"
                + "        path: foo/bar\n"
                + "      /foo:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        root-relative:\n"
                + "          type: reference\n"
                + "          path: ''\n"
                + "        /bar:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          jcr:mixinTypes: ['mix:referenceable']\n"
                + "";

        applyDefinitions(definition);

        final Node bar = testNode.getNode("foo/bar");
        expectProp("/test/absolute", PropertyType.REFERENCE, bar.getIdentifier());
        expectProp("/test/relative", PropertyType.REFERENCE, bar.getIdentifier());
        expectProp("/test/foo/root-relative", PropertyType.REFERENCE, testNode.getIdentifier());
    }

    @Test
    public void expect_uuid_reference_to_be_resolved() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /foo:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:uuid: e4ecf93e-2708-40b4-b091-51d84169a174\n"
                + "        /bar:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          jcr:mixinTypes: ['mix:referenceable']\n"
                + "          uuid-reference:\n"
                + "            type: reference\n"
                + "            value: e4ecf93e-2708-40b4-b091-51d84169a174\n"
                + "";

        applyDefinitions(definition);
        final Node foo = testNode.getNode("foo");
        expectProp("/test/foo/bar/uuid-reference", PropertyType.REFERENCE, foo.getIdentifier());
    }

    @Test
    public void expect_unresolved_references_to_be_removed() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /foo:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:uuid: e4ecf93e-2708-40b4-b091-51d84169a170\n"
                + "        /bar:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          jcr:mixinTypes: ['mix:referenceable']\n"
                + "          uuid-reference:\n"
                + "            type: reference\n"
                + "            value: e4ecf93e-2708-40b4-b091-51d84169a174\n"
                + "          uuid-references:\n"
                + "            type: reference\n"
                + "            value: [e4ecf93e-2708-40b4-b091-51d84169a174, e4ecf93e-2708-40b4-b091-51d84169a170]\n"
                + "          path-reference:\n"
                + "            type: reference\n"
                + "            path: /undefined\n"
                + "";

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(definition);
            assertFalse(testNode.hasProperty("foo/bar/uuid-reference"));
            assertTrue(testNode.hasProperty("foo/bar/uuid-references"));
            assertEquals(1, testNode.getProperty("foo/bar/uuid-references").getValues().length);
            assertFalse(testNode.hasProperty("foo/bar/path-reference"));
            assertTrue(interceptor.messages().anyMatch(m->m.equals("Reference e4ecf93e-2708-40b4-b091-51d84169a174 " +
                    "for property '/test/foo/bar/uuid-reference' defined in " +
                    "[test-group/test-project/test-module-0 [string]] not found: skipping.")));
            assertTrue(interceptor.messages().anyMatch(m->m.equals("Reference e4ecf93e-2708-40b4-b091-51d84169a174 " +
                    "for property '/test/foo/bar/uuid-references' defined in " +
                    "[test-group/test-project/test-module-0 [string]] not found: skipping.")));
            assertTrue(interceptor.messages().anyMatch(m->m.equals("Path reference '/undefined' for property " +
                    "'/test/foo/bar/path-reference' defined in " +
                    "[test-group/test-project/test-module-0 [string]] not found: skipping.")));
        }
    }

    @Test
    public void expect_jcr_uuid_to_be_retained() throws Exception {
        final String uuid = "e4ecf93e-2708-40b4-b091-51d84169a174";
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /child:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:uuid: "+uuid+"\n"
                + "";

        applyDefinitions(definition);
        assertEquals(uuid, testNode.getNode("child").getIdentifier());
    }

    @Test
    public void expect_new_jcr_uuid_created_on_collision() throws Exception {
        final String uuid = "e4ecf93e-2708-40b4-b091-51d84169a174";
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /child:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:uuid: "+uuid+"\n"
                + "      /child2:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:uuid: "+uuid+"\n"
                + "";

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationConfigService.class).build()) {
            applyDefinitions(definition);
            assertEquals(uuid, testNode.getNode("child").getIdentifier());
            assertNotEquals(uuid, testNode.getNode("child2").getIdentifier());
            assertTrue(interceptor.messages().anyMatch(m->m.equals("Specified jcr:uuid " + uuid +
                    " for node '/test/child2' defined in [test-group/test-project/test-module-0 [string]]" +
                    " already in use: a new jcr:uuid will be generated instead.")));
        }
    }

    @Test
    public void expect_protected_properties_to_be_untouched() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      jcr:mixinTypes: ['mix:created']\n" // will auto-created protected properties jcr:created and jcr:createdBy
                + "";

        ConfigurationModel baseline = applyDefinitions(definition);
        expectProp("/test/jcr:createdBy", PropertyType.STRING, "admin");

        final ExpectedEvents expectedEvents = new ExpectedEvents(); // aka, expect to see no events

        // validate the auto-created/protected and non-configured property jcr:createdBy is not (tried) to be removed
        applyDefinitions(definition, baseline, expectedEvents);
        expectProp("/test/jcr:createdBy", PropertyType.STRING, "admin");
    }

    @Test
    public void expect_unchanged_existing_nodes_to_be_untouched() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents(); // aka, expect to see no events

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[a, b]", "[jcr:primaryType]");
        expectNode("/test/a", "[z]", "[jcr:primaryType, property]");
        expectNode("/test/a/z", "[]", "[jcr:primaryType, property]");
        expectNode("/test/b", "[]", "[jcr:primaryType, property]");
        expectProp("/test/a/property", PropertyType.STRING, "a");
        expectProp("/test/a/z/property", PropertyType.STRING, "z");
        expectProp("/test/b/property", PropertyType.STRING, "b");
    }

    @Test
    public void expect_nodes_to_be_merged() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /first:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: first\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "        /recurse:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: recurse\n"
                + "      /middle:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: middle\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b\n"
                + "      /last:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: last\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/first", JCR_PRIMARYTYPE)
                .expectNodeAdded("/test/middle", JCR_PRIMARYTYPE)
                .expectNodeAdded("/test/last", JCR_PRIMARYTYPE)
                .expectNodeAdded("/test/a/recurse", JCR_PRIMARYTYPE)
                .expectPropertyAdded("/test/first/property")
                .expectPropertyAdded("/test/middle/property")
                .expectPropertyAdded("/test/last/property")
                .expectPropertyAdded("/test/a/recurse/property");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[first, a, middle, b, last]", "[jcr:primaryType]");
        expectNode("/test/first", "[]", "[jcr:primaryType, property]");
        expectProp("/test/first/property", PropertyType.STRING, "first");
        expectNode("/test/middle", "[]", "[jcr:primaryType, property]");
        expectProp("/test/middle/property", PropertyType.STRING, "middle");
        expectNode("/test/last", "[]", "[jcr:primaryType, property]");
        expectProp("/test/last/property", PropertyType.STRING, "last");
        expectNode("/test/a/recurse", "[]", "[jcr:primaryType, property]");
        expectProp("/test/a/recurse/property", PropertyType.STRING, "recurse");
    }

    @Test
    public void expect_deleted_nodes_to_be_deleted() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeRemoved("/test/a/z")
                .expectNodeRemoved("/test/b");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[a]", "[jcr:primaryType]");
    }

    @Test
    public void expect_reorders_to_be_applied() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /c:\n"
                + "        jcr:primaryType: nt:unstructured";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String idA = session.getNode("/test/a").getIdentifier();
        final String idB = session.getNode("/test/b").getIdentifier();
        final String idC = session.getNode("/test/c").getIdentifier();

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /c:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "";

        applyDefinitions(source, baseline);

        // JackRabbit's event system does not represent node reorders well. Checking that the node IDs did not change
        // to ensure the nodes did not get deleted and recreated.

        expectNode("/test", "[c, b, a]", "[jcr:primaryType]");
        assertEquals(idA, session.getNode("/test/a").getIdentifier());
        assertEquals(idB, session.getNode("/test/b").getIdentifier());
        assertEquals(idC, session.getNode("/test/c").getIdentifier());
    }

    @Test
    public void expect_node_order_ignored_in_non_orderable_node() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /non-orderable:\n"
                + "        jcr:primaryType: hippostd:directory\n"
                + "        jcr:mixinTypes: ['hippostd:relaxed']\n"
                + "        /a:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "        /b:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "        /c:\n"
                + "          jcr:primaryType: nt:unstructured";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /non-orderable:\n"
                + "        jcr:primaryType: hippostd:directory\n"
                + "        jcr:mixinTypes: ['hippostd:relaxed']\n"
                + "        /c:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "        /b:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "        /a:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents();

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test/non-orderable", "[a, b, c]", "[hippo:paths, jcr:mixinTypes, jcr:primaryType]");
    }

    @Test
    public void expect_sns_nodes_to_be_written() throws Exception {
        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns[1]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property1: value1\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property2: value2\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/sns", JCR_PRIMARYTYPE)
                .expectPropertyAdded("/test/sns/property1")
                .expectNodeAdded("/test/sns[2]", JCR_PRIMARYTYPE)
                .expectPropertyAdded("/test/sns[2]/property2");

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[sns, sns]", "[jcr:primaryType]");
        expectNode("/test/sns[1]", "[]", "[jcr:primaryType, property1]");
        expectNode("/test/sns[2]", "[]", "[jcr:primaryType, property2]");
    }

    @Test
    public void expect_sns_nodes_to_be_merged() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property1: value1\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property2: value2";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns[1]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property1: value1\n"
                + "        property2: value2\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property1: value1\n"
                + "        property2: value2\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/sns/property2")
                .expectPropertyAdded("/test/sns[2]/property1");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[sns, sns]", "[jcr:primaryType]");
        expectNode("/test/sns[1]", "[]", "[jcr:primaryType, property1, property2]");
        expectNode("/test/sns[2]", "[]", "[jcr:primaryType, property1, property2]");
    }

    @Test
    public void expect_redundant_sns_node_to_be_removed() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: nt:unstructured";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeRemoved("/test/sns[2]");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[sns]", "[jcr:primaryType]");
        expectNode("/test/sns", "[]", "[jcr:primaryType]");
    }

    @Test
    public void expect_no_fancy_sns_merging() throws Exception {
        final String baselineSource
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        /foo:\n"
                + "          jcr:primaryType: nt:unstructured";
        final ConfigurationModel baseline = applyDefinitions(baselineSource);

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        /foo:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/sns/foo", JCR_PRIMARYTYPE)
                .expectNodeRemoved("/test/sns[2]")
                .expectNodeRemoved("/test/sns[2]/foo");

        applyDefinitions(source, baseline, expectedEvents);

        expectNode("/test", "[sns]", "[jcr:primaryType]");
        expectNode("/test/sns", "[foo]", "[jcr:primaryType]");
    }
}
