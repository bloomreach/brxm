/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.UUID;

import javax.jcr.Node;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PropertyUuidTest extends RepositoryTestCase {

    @Test
    public void property_identifier_placement() throws Exception {

        final Node test = session.getRootNode().addNode("test", "nt:unstructured");

        final Node car = test.addNode("car", "hippo:testcardocument");

        assertThat(car.hasProperty("hippo:identifier"))
                .as("Expected 'hippo:identifier' autocreated")
                .isTrue();

        final String identifier = car.getProperty("hippo:identifier").getString();

        validateUUID(identifier);

        final Node car2 = JcrUtils.copy(session, car.getPath(), test.getPath() + "/car2");

        session.save();

        assertThat(identifier)
                .as("Expected autocreated property to be consistent across copy")
                .isEqualTo(car2.getProperty("hippo:identifier").getString());


        String[] content = new String[]{
                "/test/car3", "hippo:testcardocument",
                "/test/car4", "hippo:testcardocument",
                "hippo:identifier", "foo",
                "/test/car5", "hippo:testcardocument",
        };

        build(content, session);

        session.save();

        assertThat(session.getNode("/test/car3").hasProperty("hippo:identifier")).isTrue();
        // setting value is possible for auto created property 'hippo:identifier'
        assertThat(session.getNode("/test/car4").getProperty("hippo:identifier").getString()).isEqualTo("foo");


        assertThat(session.getNode("/test/car3").getProperty("hippo:identifier").getString())
                .as("Every newly created node (not a copy) should get a unique new identifier")
                .isNotEqualTo(session.getNode("/test/car5").getProperty("hippo:identifier").getString());

        test.addMixin("hippo:identifiable");
        assertThat(test.hasProperty("hippo:identifier"))
                .as("Adding mixin 'hippo:identifiable' should result in autocreated 'hippo:identifier' property");
    }

    @Test
    public void property_identifier_placement_yaml_fixture() throws Exception {
        assertThat(session.nodeExists("/autocreate-test"))
                .as("expected 'autocreate-test' to be have been bootstrapped")
                .isTrue();

        final Node autocreate_uuid_node = session.getNode("/autocreate-test/autocreate-uuid-node");

        assertThat(autocreate_uuid_node.hasProperty("hippo:identifier"))
                .as("Expected 'hippo:identifier' to be autocreated, see 'hippo:testcardocument'")
                .isTrue();;

        validateUUID(autocreate_uuid_node.getProperty("hippo:identifier").getString());


        final Node bootstrapped_autocreate_node = session.getNode("/autocreate-test/bootstrapped-autocreate-node");

        assertThat(bootstrapped_autocreate_node.hasProperty("hippo:identifier"))
                .as("Expected 'hippo:identifier' to be bootstrapped")
                .isTrue();;

        final String bootstrapped = bootstrapped_autocreate_node.getProperty("hippo:identifier").getString();

        assertEquals("hippo:identifier can also be bootstrapped by yaml even though autocreated as uuid if missing",
                "bootstrapped-fixed", bootstrapped);

    }

    private void validateUUID(final String expectedUUID) {
        try {
            UUID.fromString(expectedUUID);
        } catch (IllegalArgumentException e) {
            fail("Expected uuid format");
        }
    }

}
