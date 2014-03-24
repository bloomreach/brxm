package org.onehippo.cms7.essentials.dashboard.simplecontent;

import org.onehippo.cms7.essentials.dashboard.packaging.DefaultPowerpack;


public class SimpleContentPowerpack extends DefaultPowerpack {

    @Override
    public String getInstructionPath() {
        return "/META-INF/simpleContent_instructions.xml";
    }
}
