/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;

public class BrowserSections implements IClusterable {

    private final List<IChangeListener> listeners;
    private final Map<String, IBrowserSection> sections;
    private String active;

    public BrowserSections() {
        sections = new LinkedHashMap<>();
        listeners = new LinkedList<>();
    }

    public Collection<String> getSections() {
        return Collections.unmodifiableCollection(sections.keySet());
    }

    public IBrowserSection getSection(final String name) {
        return sections.get(name);
    }

    public String getName(final IBrowserSection section) {
        for (final Entry<String, IBrowserSection> entry : sections.entrySet()) {
            if (entry.getValue().equals(section)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void addSection(final String name, final IBrowserSection section) {
        if (sections.size() == 0) {
            active = name;
        }
        sections.put(name, section);
        notifyListeners();
    }

    public void removeSection(final String name) {
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

    public String getActiveSectionName() {
        return active;
    }

    public IBrowserSection getActiveSection() {
        if (active != null) {
            return sections.get(active);
        }
        return null;
    }

    public void setActiveSectionByName(final String name) {
        if (!active.equals(name)) {
            active = name;
            notifyListeners();
        }
    }

    public boolean isActive(final IBrowserSection section) {
        if (section == null || active == null) {
            return false;
        }
        return sections.get(active).equals(section);
    }

    public boolean isActive(final String name) {
        if (name == null || active == null) {
            return false;
        }
        return name.equals(active);
    }

    public void addListener(final IChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final IChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(org.hippoecm.frontend.model.IChangeListener::onChange);
    }
}
