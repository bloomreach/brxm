/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.diagnosis;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 */
class DefaultTaskImpl implements Task {

    private final String name;
    private Map<String, Object> attributes;

    private final Task parentTask;
    private List<Task> childTasks;

    private long startTimeMillis;
    private long durationTimeMillis = -1L;
    private boolean stopped;

    DefaultTaskImpl(final Task parentTask, final String name) {
        this.parentTask = parentTask;
        this.name = name;
        this.startTimeMillis = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        if (attributes == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (attributes != null) {
            return Collections.enumeration(attributes.keySet());
        } else {
            List<String> emptyAttrNames = Collections.emptyList();
            return Collections.enumeration(emptyAttrNames);
        }
    }

    @Override
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        }

        attributes.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        if (attributes == null) {
            return null;
        }

        return attributes.get(key);
    }

    @Override
    public Object removeAttribute(String key) {
        if (attributes != null) {
            return attributes.remove(key);
        }

        return null;
    }

    @Override
    public Task getParentTask() {
        return parentTask;
    }

    @Override
    public Task startSubtask(String name) {
        if (stopped) {
            throw new IllegalStateException("The task was already stopped.");
        }

        if (childTasks == null) {
            childTasks = new LinkedList<Task>();
        }

        Task childTask = new DefaultTaskImpl(this, name);
        childTasks.add(childTask);
        HDC.setCurrentTask(childTask);
        return childTask;
    }

    @Override
    public void stop() {
        if (stopped) {
            throw new IllegalStateException("The task was already stopped.");
        }

        stopped = true;
        durationTimeMillis = System.currentTimeMillis() - startTimeMillis;
        HDC.setCurrentTask(parentTask);
    }

    @Override
    public Collection<Task> getChildTasks() {
        if (childTasks == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(childTasks);
    }

    @Override
    public boolean isRunning() {
        return !stopped;
    }

    @Override
    public long getDurationTimeMillis() {
        return durationTimeMillis;
    }

}
