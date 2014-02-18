package org.onehippo.cms7.essentials.powerpack;

import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

/**
 * User: obourgeois
 * Date: 11-11-13
 */
@Component
public class BasicPowerpackWithSamples extends BasicPowerpack {

    @Override
    public Set<String> groupNames() {
        return new ImmutableSet.Builder<String>().add(EssentialConst.INSTRUCTION_GROUP_DEFAULT).add("samples").build();
    }
}
