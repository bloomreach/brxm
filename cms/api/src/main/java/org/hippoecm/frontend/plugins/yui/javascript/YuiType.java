/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.javascript;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.util.io.IClusterable;

public final class YuiType implements IClusterable {

    private static final long serialVersionUID = 1L;

    private List<Setting> properties = new ArrayList<Setting>();

    public YuiType(Setting... settings) {
        this(null, settings);
    }

    public YuiType(YuiType base, Setting... settings) {
        if (base != null) {
            properties.addAll(base.getProperties());
        }
        for (Setting setting : settings) {
            properties.add(setting);
        }
    }

    public List<Setting> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof YuiType) {
            return properties.equals(((YuiType) obj).properties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(31, 71).append(properties).toHashCode();
    }

}
