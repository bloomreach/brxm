/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JavaLayoutPad implements ILayoutPad {

    private static final long serialVersionUID = 1L;

    private String name;
    private boolean isList = false;
    private Orientation orientation = Orientation.VERTICAL;
    private Map<String, ILayoutTransition> transitions;

    public JavaLayoutPad(String name) {
        this.name = name;
        this.transitions = new TreeMap<String, ILayoutTransition>();
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public void setIsList(boolean isList) {
        this.isList = isList;
    }

    public void addTransition(String name, ILayoutTransition transition) {
        transitions.put(name, transition);
    }

    public String getName() {
        return name;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public ILayoutTransition getTransition(String name) {
        return transitions.get(name);
    }

    public List<String> getTransitions() {
        return new ArrayList<String>(transitions.keySet());
    }

    public boolean isList() {
        return isList;
    }

}
