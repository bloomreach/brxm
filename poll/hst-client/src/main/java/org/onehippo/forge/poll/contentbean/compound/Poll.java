/*
 * Copyright 2009-2023 Bloomreach
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
package org.onehippo.forge.poll.contentbean.compound;

import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

/**
 * Bean representation of:
 *
 *  [poll:poll]
 *    - poll:active (boolean) // deprecated
 *    - poll:text (string)
 *    - poll:introduction (string)
 *    + poll:option (poll:option) multiple
 */

@Node(jcrType = "poll:poll")
@HippoEssentialsGenerated(allowModifications = false)
public class Poll extends HippoItem {

    public String getText() {
        return this.getSingleProperty("poll:text");
    }

    public String getIntroduction() {
        return this.getSingleProperty("poll:introduction");
    }

    public List<Option> getOptions() {
        return this.getChildBeans("poll:option");
    }
}
