/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoItem;

/**
 * Bean representation of:
 *  
 *  [poll:option]
 *    - poll:value (string)
 *    - poll:label (string)
 */

@Node(jcrType = "poll:option")
public class Option extends HippoItem {
    
    public String getValue() {
        String value = this.getProperty("poll:value");
        if (value != null) {
            return value;
        }
        
        // if not there, take label's first word in lower case
        String label = this.getProperty("poll:label");
        return (label == null) ? "null" : label.split(" ")[0].toLowerCase();
    }
    
    public String getLabel() {
        String label = this.getProperty("poll:label");
        return (label != null) ? label : getValue();
    }
}
