/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

@XmlRootElement(name = "instructions", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class PluginInstructions implements Instructions {


    private Set<InstructionSet> instructionSets = new LinkedHashSet<>();

    @Override
    public int totalInstructions() {
        int total = 0;
        if (instructionSets != null) {
            for (InstructionSet instructionSet : instructionSets) {
                final Set<Instruction> instructions = instructionSet.getInstructions();
                if (instructions != null) {
                    total += instructions.size();
                }
            }
        }
        return total;
    }

    @Override
    public int totalInstructionSets() {
        if (instructionSets != null) {
            return instructionSets.size();
        }
        return 0;
    }

    @XmlElementRefs({@XmlElementRef(type = PluginInstructionSet.class, name = "instructionSet")})
    @Override
    public Set<InstructionSet> getInstructionSets() {
        return instructionSets;
    }

    @Override
    public void setInstructionSets(final Set<InstructionSet> instructionSets) {
        this.instructionSets = instructionSets;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginInstructions{");
        sb.append("instructionSets=").append(instructionSets);
        sb.append('}');
        return sb.toString();
    }

    public void addDefaultInstruction(final Instruction instruction) {

        if (instructionSets == null) {
            instructionSets = new LinkedHashSet<>();
        }
        if (instructionSets.size() ==0) {
            final PluginInstructionSet set = new PluginInstructionSet();
            set.setGroup(EssentialConst.INSTRUCTION_GROUP_DEFAULT);
            set.addInstruction(instruction);
            instructionSets.add(set);

        }else{
            // check if we have default instruction set:
            for (InstructionSet instructionSet : instructionSets) {
                final Set<String> groups = instructionSet.getGroups();
                if (groups.size() == 1 && groups.iterator().next().equals(EssentialConst.INSTRUCTION_GROUP_DEFAULT)) {
                    instructionSet.addInstruction(instruction);
                    return;
                }
            }
            // add default instruction set:
            final PluginInstructionSet set = new PluginInstructionSet();
            set.setGroup(EssentialConst.INSTRUCTION_GROUP_DEFAULT);
            set.addInstruction(instruction);
            instructionSets.add(set);

        }

    }
}
