package org.onehippo.cms7.essentials.dashboard.simplecontent;

import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.packaging.DefaultPowerpack;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;


public class SimpleContentPowerpack extends DefaultPowerpack {

    @Override
    public Set<String> groupNames() {
        final Boolean sampleData = Boolean.valueOf((String) getProperties().get("sampleData"));
        final String templateName = (String) getProperties().get("templateName");
        final String templateGroup = Strings.isNullOrEmpty(templateName) ? "jsp" : templateName;
        if (sampleData) {
            return new ImmutableSet.Builder<String>()
                    .add(EssentialConst.INSTRUCTION_GROUP_DEFAULT)
                    .add("sampleData")
                    .add(templateGroup).build();
        }
        return new ImmutableSet.Builder<String>()
                .add(EssentialConst.INSTRUCTION_GROUP_DEFAULT)
                .add(templateGroup).build();
    }

    @Override
    public String getInstructionPath() {
        return "/META-INF/simpleContent_instructions.xml";
    }
}
