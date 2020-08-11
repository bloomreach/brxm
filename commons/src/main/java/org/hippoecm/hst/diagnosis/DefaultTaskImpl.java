/**
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultTaskImpl
 */
public class DefaultTaskImpl implements Task {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskImpl.class);

    private final String name;
    private Map<String, Object> attributes;

    private final Task parentTask;
    private List<Task> childTasks;

    private long startTimeMillis;
    private long durationTimeMillis = -1L;
    private boolean stopped;

    protected DefaultTaskImpl(final Task parentTask, final String name) {
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
            // keep order of insertion thus Linked
            attributes = new LinkedHashMap<String, Object> ();
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

        Task childTask = createSubtask(this, name);
        childTasks.add(childTask);
        HDC.setCurrentTask(childTask);
        return childTask;
    }

    @Override
    public void stop() {
        if (stopped) {
            log.warn("Task '{}' was already stopped.", name);
            return;
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
        if (!stopped) {
            log.warn("Task '{}' was not stopped hence duration time unknown.", name);
        }
        return durationTimeMillis;
    }

    /**
     * Creates a real <code>Task</code> instance.
     * @param parentTask parent task
     * @param name task name
     * @return <code>Task</code> instance
     */
    protected Task createSubtask(final Task parentTask, final String name) {
        return new DefaultTaskImpl(parentTask, name);
    }
}
