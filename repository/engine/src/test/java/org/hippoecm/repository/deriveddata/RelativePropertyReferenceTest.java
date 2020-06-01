/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.deriveddata;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.hippoecm.repository.ext.DerivedDataFunction;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RelativePropertyReferenceTest extends RepositoryTestCase {

    private String derivedDataFunction = "/test-relative-dd:\n" +
            "  jcr:primaryType: hipposys:deriveddefinition\n" +
            "  hipposys:classname: " + MultiplePythagoreanTheorem.class.getName() + "\n" +
            "  hipposys:nodetype: relativePropertyReferenceTest:basedocument\n" +
            "  hipposys:serialver: 1\n" +
            "  /hipposys:accessed:\n" +
            "    jcr:primaryType: hipposys:propertyreferences\n" +
            "    /testprop:\n" +
            "      jcr:primaryType: hipposys:relativepropertyreference\n" +
            "      hipposys:relPath: relativePropertyReferenceTest:testprop\n" +
            "  /hipposys:derived:\n" +
            "    jcr:primaryType: hipposys:propertyreferences\n" +
            "    /derivedprop:\n" +
            "      jcr:primaryType: hipposys:relativepropertyreference\n" +
            "      hipposys:relPath: relativePropertyReferenceTest:derivedprop\n" +
            "    /derivedpropmultivalued:\n" +
            "      jcr:primaryType: hipposys:relativepropertyreference\n" +
            "      hipposys:relPath: relativePropertyReferenceTest:derivedpropmultivalued\n" +
            "      hipposys:multivalue: true\n" +
            "    /registeredmultiplederivedprop:\n" +
            "      jcr:primaryType: hipposys:relativepropertyreference\n" +
            "      hipposys:relPath: relativePropertyReferenceTest:registeredmultiplederivedprop\n";

    private String testContent = "/relativepropertytestcontent:\n" +
            "  jcr:primaryType: relativePropertyReferenceTest:basedocument\n" +
            "  jcr:mixinTypes: ['mix:versionable']\n" +
            "  relativePropertyReferenceTest:testprop: testvalue";

    private String cnd = "<'relativePropertyReferenceTest'='http://www.onehippo.org/relativePropertyReferenceTest/nt/1.0'>\n" +
            "<'hippo'='http://www.onehippo.org/jcr/hippo/nt/2.0.4'>\n" +
            "<'hippostd'='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n" +
            "\n" +
            "[relativePropertyReferenceTest:basedocument] > hippo:document, hippostd:relaxed \n" +
            "  - relativePropertyReferenceTest:registeredmultiplederivedprop (string) multiple \n";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        CndImporter.registerNodeTypes(new StringReader(cnd), session);

        final ConfigurationService configurationService = HippoServiceRegistry.getService(ConfigurationService.class);
        configurationService.importPlainYaml(
                new ByteArrayInputStream(derivedDataFunction.getBytes()),
                session.getNode("/hippo:configuration/hippo:derivatives"));
        configurationService.importPlainYaml(
                new ByteArrayInputStream(testContent.getBytes()),
                session.getRootNode());

        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        if (session.nodeExists("/relativepropertytestcontent")) {
            session.getNode("/relativepropertytestcontent").remove();
        }
        if (session.nodeExists("/hippo:configuration/hippo:derivatives/test-relative-dd")) {
            session.getNode("/hippo:configuration/hippo:derivatives/test-relative-dd").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void test_relativepropertyreference_deriveddata_with_multivalue_config() throws RepositoryException {

        final Node testContent = session.getNode("/relativepropertytestcontent");
        testContent.setProperty("relativePropertyReferenceTest:testprop", "new test value");
        session.save();

        //Backwards compatibility, when 'hipposys:multivalue' isn't set, generated property is single  
        assertTrue("Derived property is not set", testContent.hasProperty("relativePropertyReferenceTest:derivedprop"));
        assertFalse("Derived property is multiple", testContent.getProperty("relativePropertyReferenceTest:derivedprop").isMultiple());
        assertEquals("Derived property doesn't have expected value", "new test value", testContent.getProperty("relativePropertyReferenceTest:derivedprop").getString());

        //Backwards compatibility, when 'hipposys:multivalue' isn't set, but the derived property is registered in the cnd and marked as multiple, then generated prop is multiple
        assertTrue("CND registered derived property is not set", testContent.hasProperty("relativePropertyReferenceTest:registeredmultiplederivedprop"));
        assertTrue("CND registered derived property is not multiple", testContent.getProperty("relativePropertyReferenceTest:registeredmultiplederivedprop").isMultiple());
        assertArrayEquals("CND registered derived property doesn't have expected values",
                new String[]{"new test value", "new test value second", "new test value third"},
                JcrUtils.getMultipleStringProperty(testContent, "relativePropertyReferenceTest:registeredmultiplederivedprop",  new String[0]));

        //Test new feature: the derived property can be configured to be multiple 
        assertTrue("Multivalue derived property is not set", testContent.hasProperty("relativePropertyReferenceTest:derivedpropmultivalued"));
        assertTrue("Multivalue derived property is not multiple", testContent.getProperty("relativePropertyReferenceTest:derivedpropmultivalued").isMultiple());
        assertArrayEquals("Multivalue derived property doesn't have expected values",
                new String[]{"new test value", "new test value second", "new test value third"},
                JcrUtils.getMultipleStringProperty(testContent, "relativePropertyReferenceTest:derivedpropmultivalued", new String[0]));
    }

    static class MultiplePythagoreanTheorem extends DerivedDataFunction {
        static final long serialVersionUID = 1;

        public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
            try {
                String a = parameters.get("testprop")[0].getString();
                Value[] values = new Value[]{
                        getValueFactory().createValue(a),
                        getValueFactory().createValue(a + " second"),
                        getValueFactory().createValue(a + " third")};

                parameters.put("derivedprop", values);
                parameters.put("derivedpropmultivalued", values);
                parameters.put("registeredmultiplederivedprop", values);
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            return parameters;

        }
    }
}

