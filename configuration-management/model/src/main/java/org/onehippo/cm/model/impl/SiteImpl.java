/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Site;

public class SiteImpl implements Site {

    private static final OrderableByNameListSorter<Group> groupSorter =
            new OrderableByNameListSorter<>(Group.class);

    private final String name;

    private final List<GroupImpl> modifiableGroups = new ArrayList<>();
    private final List<GroupImpl> groups = Collections.unmodifiableList(modifiableGroups);
    private final Map<String, GroupImpl> groupMap = new HashMap<>();

    public SiteImpl(final String name) {
        this.name = (name==null)? CORE_NAME: name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<GroupImpl> getGroups() {
        return groups;
    }

    public GroupImpl addGroup(final String name) {
        final GroupImpl group = new GroupImpl(name, this);
        groupMap.put(name, group);
        modifiableGroups.add(group);
        return group;
    }

    public void sortGroups(Set<String> coreGroupNames) {
        groupSorter.sort(modifiableGroups, coreGroupNames);
        modifiableGroups.forEach(GroupImpl::sortProjects);
    }

    public GroupImpl getOrAddGroup(final String name) {
        return groupMap.containsKey(name) ? groupMap.get(name) : addGroup(name);
    }

    @Override
    public int compareTo(final Site o) {
        return name.compareTo(o.getName());
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Site) {
            final Site otherSite = (Site) other;
            return Objects.equals(name, otherSite.getName());
        }
        return false;
    }

    // hashCode() and equals() should be consistent!
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "HcmSiteImpl{" +
                "name='" + name + '\'' +
                '}';
    }
}
