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
package org.hippoecm.frontend.model.map;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class HippoMap extends HashMap<String, Object> implements IHippoMap {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String primaryType = "nt:unstructured";
    private List<String> mixins = new LinkedList<String>();

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String type) {
        primaryType = type;
    }

    public String[] getMixinTypes() {
        return mixins.toArray(new String[mixins.size()]);
    }
    
    public void addMixinType(String type) {
        mixins.add(type);
    }

    public void removeMixinType(String type) {
        mixins.remove(type);
    }

    public void reset() {
        clear();
    }

    public void save() {
    }

}
