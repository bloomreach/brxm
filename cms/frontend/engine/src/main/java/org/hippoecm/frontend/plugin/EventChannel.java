/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;

public class EventChannel implements IClusterable {
    private static final long serialVersionUID = 1L;
    
    private final String name;

    public EventChannel(String name) {
        this.name = name;
    }

    public EventChannel(Plugin plugin) {
        this.name = plugin.getDescriptor().getPluginId();
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
       return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
           .append("name", name)
           .toString();
    }
    
    @Override
    public boolean equals(Object object) {
        if (object instanceof EventChannel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        EventChannel eventChannel = (EventChannel) object;
        return new EqualsBuilder()
            .append(name, eventChannel.name) 
            .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(779, 211)
            .append(name)
            .toHashCode();
    }


}
