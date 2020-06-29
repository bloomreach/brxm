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
import javax.jcr.nodetype.ConstraintViolationException;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.Utilities;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PropertyUuidTest extends RepositoryTestCase {

    @Test
    public void property_uuid_placement() throws Exception {

        final Node test = session.getRootNode().addNode("test", "nt:unstructured");

        final Node car = test.addNode("car", "hippo:testcardocument");

        final Node car2 = JcrUtils.copy(session, car.getPath(), test.getPath() + "/car2");

        session.save();

        assertThat(car.getProperty("hippo:uuid").getString())
                .isEqualTo(car2.getProperty("hippo:uuid").getString());

        assertThat(car.getProperty("hippo:uuid2").getString())
                .isEqualTo(car2.getProperty("hippo:uuid2").getString());

        String[] content = new String[]{
                "/test/car3", "hippo:testcardocument",
                "/test/car4", "hippo:testcardocument",
                "hippo:uuid", "foo",
        };

        build(content, session);

        session.save();

        assertThat(session.getNode("/test/car3").hasProperty("hippo:uuid")).isTrue();
        // bootstrapping is possible for 'non protected property'
        assertThat(session.getNode("/test/car4").getProperty("hippo:uuid").getString()).isEqualTo("foo");

        // set extra property with replacement function replaces value with uuid

        car.setProperty("hippo:brand", "${fn:new-uuid()}");

        // ${fn:new-uuid()} should be replaced by a uuid
        assertThat(car.getProperty("hippo:brand").getString()).isNotEqualTo("${fn:new-uuid()}");

        try {
            UUID.fromString(car.getProperty("hippo:brand").getString());
        } catch (IllegalArgumentException e) {
            fail("property value for foo should be a uuid");
        }

        // not allowed to set protected property
        String[] content2 = new String[]{
                "/test/car5", "hippo:testcardocument",
                "hippo:uuid2", "foo",
        };

        try {
            build(content2, session);
            fail("hippo:uuid2 is protected and should not be allowed to be bootstrapped");
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

    @Test
    public void property_uuid_placement_yaml_fixture() throws Exception {
        assertThat(session.nodeExists("/autocreate-test"))
                .as("expected 'autocreate-test' to be have been bootstrapped")
                .isTrue();

        final Node autocreate_uuid_node = session.getNode("/autocreate-test/autocreate-uuid-node");

        assertThat(autocreate_uuid_node.hasProperty("hippo:uuid2"))
                .as("Expected 'hippo:uuid2' to be autocreated, see 'hippo:testcardocument'")
                .isTrue();;
        assertThat(autocreate_uuid_node.hasProperty("hippo:uuid"))
                .as("Expected 'hippo:uuid' to be autocreated, see 'hippo:testcardocument'")
                .isTrue();;

        validateUUID(autocreate_uuid_node.getProperty("hippo:uuid2").getString());
        validateUUID(autocreate_uuid_node.getProperty("hippo:uuid").getString());


        final Node bootstrapped_autocreate_node = session.getNode("/autocreate-test/bootstrapped-autocreate-node");

        assertThat(bootstrapped_autocreate_node.hasProperty("hippo:uuid2"))
                .as("Expected 'hippo:uuid2' to be autocreated, see 'hippo:testcardocument'")
                .isTrue();;
        assertThat(bootstrapped_autocreate_node.hasProperty("hippo:uuid"))
                .as("Expected 'hippo:uuid' to be autocreated, see 'hippo:testcardocument'")
                .isTrue();;

        validateUUID(bootstrapped_autocreate_node.getProperty("hippo:uuid2").getString());

        final String bootstrapped = bootstrapped_autocreate_node.getProperty("hippo:uuid").getString();

        assertEquals("hippo:uuid can also be bootstrapped by yaml even though autocreated as uuid if missing",
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
