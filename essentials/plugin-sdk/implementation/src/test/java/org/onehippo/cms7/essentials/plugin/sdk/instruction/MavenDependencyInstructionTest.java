/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.instruction;

import java.io.File;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.plugin.sdk.instructions.Instruction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenDependencyInstructionTest extends ResourceModifyingTest {
    private int counter;

    @Test
    public void default_change_message() {
        final BiConsumer<Instruction.Type, String> collector = (g, m) -> {
            assertTrue(Instruction.Type.EXECUTE == g);
            assertEquals("Add Maven dependency group:artifact to module 'cms'.", m);
            counter++;
        };
        final MavenDependencyInstruction instruction = makeInstruction();
        instruction.setGroupId("group");
        instruction.setArtifactId("artifact");
        instruction.setTargetPom("cms");

        counter = 0;
        instruction.populateChangeMessages(collector);
        assertEquals(1, counter);
    }

    @Test
    public void xml_based_change_message() {
        final String message = "anything";
        final BiConsumer<Instruction.Type, String> collector = (g, m) -> {
            assertTrue(Instruction.Type.EXECUTE == g);
            assertEquals(message, m);
            counter++;
        };
        final MavenDependencyInstruction instruction = makeInstruction();
        instruction.setMessage(message);

        counter = 0;
        instruction.populateChangeMessages(collector);
        assertEquals(1, counter);
    }

    @Test
    public void add_dependency() throws Exception {
        final String groupId = "test-group";
        final String artifactId = "test-artifact";
        final String version = "1.2.3";
        final String type = "test-war";
        final String scope = "test-compile";

        final File pom = createModifiableFile("/instructions/maven-dependency/pom.xml", "cms/pom.xml");

        final MavenDependencyInstruction instruction = makeInstruction();
        instruction.setGroupId(groupId);
        instruction.setArtifactId(artifactId);
        instruction.setVersion(version);
        instruction.setType(type);
        instruction.setScope(scope);

        assertEquals(groupId, instruction.getGroupId());
        assertEquals(artifactId, instruction.getArtifactId());
        assertEquals(version, instruction.getVersion());
        assertEquals(type, instruction.getType());
        assertEquals(scope, instruction.getScope());

        // no target pom specified
        assertEquals(Instruction.Status.FAILED, instruction.execute(null));
        assertEquals(0, nrOfOccurrences(pom, groupId));
        assertEquals(0, nrOfOccurrences(pom, artifactId));
        assertEquals(0, nrOfOccurrences(pom, version));
        assertEquals(0, nrOfOccurrences(pom, type));
        assertEquals(0, nrOfOccurrences(pom, scope));

        instruction.setTargetPom("cms");

        assertEquals("cms", instruction.getTargetPom());

        assertEquals(Instruction.Status.SUCCESS, instruction.execute(null));
        assertEquals(1, nrOfOccurrences(pom, groupId));
        assertEquals(1, nrOfOccurrences(pom, artifactId));
        assertEquals(1, nrOfOccurrences(pom, version));
        assertEquals(1, nrOfOccurrences(pom, type));
        assertEquals(1, nrOfOccurrences(pom, scope));
    }

    private MavenDependencyInstruction makeInstruction() {
        final MavenDependencyInstruction instruction = new MavenDependencyInstruction();
        autoWire(instruction);
        return instruction;
    }
}
