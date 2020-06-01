/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;


@XmlRootElement(name = "instructionSet", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class PluginInstructionSet {

    private Set<Instruction> instructions = new LinkedHashSet<>();
    private String group;

    @XmlElementRefs({
            @XmlElementRef(type = XmlInstruction.class),
            @XmlElementRef(type = CndInstruction.class),
            @XmlElementRef(type = ExecuteInstruction.class),
            @XmlElementRef(type = DirectoryInstruction.class),
            @XmlElementRef(type = FreemarkerInstruction.class),
            @XmlElementRef(type = FileInstruction.class),
            @XmlElementRef(type = TranslationsInstruction.class),
            @XmlElementRef(type = HstBeanClassesInstruction.class),
            @XmlElementRef(type = MavenDependencyInstruction.class)
    })
    public Set<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(final Set<Instruction> instructions) {
        this.instructions = instructions;
    }

    public void addInstruction(final Instruction instruction) {
        if (instructions == null) {
            instructions = new LinkedHashSet<>();
        }
        if (instruction == null) {
            return;
        }
        instructions.add(instruction);
    }

    @XmlAttribute
    public String getGroup() {
        if (Strings.isNullOrEmpty(group)) {
            group = EssentialConst.INSTRUCTION_GROUP_DEFAULT;
        }
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public Set<String> getGroups() {
        if (Strings.isNullOrEmpty(group)) {
            return EssentialConst.DEFAULT_GROUPS;
        }
        final Iterable<String> groups = Splitter.on(',').trimResults().omitEmptyStrings().split(group);
        return Sets.newHashSet(groups);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginInstructionSet{");
        sb.append("instructions=").append(instructions);
        sb.append(", group='").append(group).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
