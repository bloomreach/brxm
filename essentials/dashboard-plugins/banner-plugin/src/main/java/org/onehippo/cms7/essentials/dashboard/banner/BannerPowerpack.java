/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.banner;

import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.packaging.DefaultPowerpack;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id$"
 */
public class BannerPowerpack extends DefaultPowerpack {

    private static final ImmutableSet<String> INSTRUCTION_GROUPS_SAMPLE = new ImmutableSet.Builder<String>().add(EssentialConst.INSTRUCTION_GROUP_DEFAULT).add("samples").build();

    @Override
    public Set<String> groupNames() {
        if (Boolean.valueOf((String) getProperties().get("sampleData"))) {
            return INSTRUCTION_GROUPS_SAMPLE;
        }
        return DEFAULT_GROUPS;
    }

    @Override
    public String getInstructionPath() {
        return "/META-INF/banner_instructions.xml";
    }
}
