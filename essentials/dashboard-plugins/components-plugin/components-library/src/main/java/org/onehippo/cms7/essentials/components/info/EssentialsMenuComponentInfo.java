/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */
public interface EssentialsMenuComponentInfo {


    @Parameter(name = "menu", required = true, displayName = "Menu name")
    String getSiteMenu();

    @Parameter(name = "level", required = false, defaultValue = "1", displayName = "Menu display level (depth)", description = "How many menu levels will be displayed")
    @DropDownList(value = {"1", "2", "3", "4", "5"})
    String getLevel();

    /**
     * When multi menu levels are shown, do we show only selected menu item and it's children items or all of them
     *
     * @return true by default
     */
    @Parameter(name = "selectedMenu", required = false, defaultValue = "true", displayName = "Only show selected menu level")
    Boolean getShowOnlySelectedLevel();

}
