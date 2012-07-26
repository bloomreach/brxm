/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.cms.browse.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;

public class BrowserSections implements IClusterable {

    private static final long serialVersionUID = 1L;

    private List<IChangeListener> listeners;
    private Map<String, IBrowserSection> sections;
    private String active;

    public BrowserSections() {
        this.sections = new LinkedHashMap<String, IBrowserSection>();
        this.listeners = new LinkedList<IChangeListener>();
    }

    public Collection<String> getSections() {
        return Collections.unmodifiableCollection(sections.keySet());
    }

    public IBrowserSection getSection(String name) {
        return sections.get(name);
    }
    
    public void addSection(String name, IBrowserSection section) {
        if (sections.size() == 0) {
            active = name;
        }
        sections.put(name, section);
        notifyListeners();
    }

    public void removeSection(String name) {
        sections.remove(name);
        if (name.equals(active)) {
            if (sections.size() > 0) {
                active = sections.keySet().iterator().next();
            } else {
                active = null;
            }
        }
        notifyListeners();
    }

    public String getActiveSection() {
        return active;
    }

    public void setActiveSection(String name) {
        if (active != name) {
            this.active = name;
            notifyListeners();
        }
    }

    public void addListener(IChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(IChangeListener listener) {
        this.listeners.remove(listener);
    }

    private void notifyListeners() {
        for (IChangeListener listener : new ArrayList<IChangeListener>(listeners)) {
            listener.onChange();
        }
    }

}
