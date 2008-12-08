/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.frontend.plugins.yui.layout;

import java.util.Map;

import org.apache.wicket.behavior.AbstractBehavior;
import org.hippoecm.frontend.plugins.yui.util.OptionsUtil;

public class UnitBehavior extends AbstractBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private UnitSettings settings;
    
    public UnitBehavior(UnitSettings settings) {
        this.settings = settings;
    }
    
    /**
     * Configure a LayoutUnit
     * @param position  Position in the wireframe
     * @param options   String array containing options in key=value scheme
     */
    public UnitBehavior(String position, String... options) {
        this(position, OptionsUtil.keyValuePairsToMap(options));
    }

    public UnitBehavior(String position, Map<String, String> options) {
        settings = new UnitSettings(position);
        settings.updateValues(options);
    }
    
    public UnitSettings getSettings() {
        return settings;
    }
 
}
