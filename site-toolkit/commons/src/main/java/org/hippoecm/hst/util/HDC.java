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
package org.hippoecm.hst.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Hierarchical Diagnostic Context
 */
public class HDC {

    private static ThreadLocal<Task> tlRootTask = new ThreadLocal<Task>();
    private static ThreadLocal<Task> tlCurrentTask = new ThreadLocal<Task>();

    private HDC() {
    }

    public static Task start() {
        Task rootTask = tlRootTask.get();

        if (rootTask != null) {
            throw new IllegalStateException("The root task was already started.");
        }

        rootTask = new Task(null, "<root>");
        tlRootTask.set(rootTask);
        return rootTask;
    }

    public static Task getRootTask() {
        return tlRootTask.get();
    }

    public static Task getCurrentTask() {
        Task current = tlCurrentTask.get();

        if (current != null) {
            return current;
        }

        return getRootTask();
    }

    public static void cleanUp() {
        tlCurrentTask.remove();
        tlRootTask.remove();
    }

    public static final class Task {

        private final String name;
        private Map<String, Object> attributes;

        private final Task parentTask;
        private List<Task> childTasks;

        private long startTimeMillis;
        private long durationTimeMillis = -1L;
        private boolean stopped;

        Task(final Task parentTask, final String name) {
            this.parentTask = parentTask;
            this.name = name;
            this.startTimeMillis = System.currentTimeMillis();
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getAttributeMap() {
            if (attributes == null) {
                return Collections.emptyMap();
            }

            return Collections.unmodifiableMap(attributes);
        }

        public Enumeration<String> getAttributeNames() {
            if (attributes != null) {
                return Collections.enumeration(attributes.keySet());
            } else {
                List<String> emptyAttrNames = Collections.emptyList();
                return Collections.enumeration(emptyAttrNames);
            }
        }

        public void setAttribute(String key, Object value) {
            if (attributes == null) {
                attributes = new HashMap<String, Object>();
            }

            attributes.put(key, value);
        }

        public Object getAttribute(String key) {
            if (attributes == null) {
                return null;
            }

            return attributes.get(key);
        }

        public Object removeAttribute(String key) {
            if (attributes != null) {
                return attributes.remove(key);
            }

            return null;
        }

        public Task getParentTask() {
            return parentTask;
        }

        public Task startSubtask(String name) {
            if (stopped) {
                throw new IllegalStateException("The task was already stopped.");
            }

            if (childTasks == null) {
                childTasks = new LinkedList<Task>();
            }

            Task childTask = new Task(this, name);
            childTasks.add(childTask);
            tlCurrentTask.set(childTask);
            return childTask;
        }

        public void stop() {
            if (stopped) {
                throw new IllegalStateException("The task was already stopped.");
            }

            stopped = true;
            durationTimeMillis = System.currentTimeMillis() - startTimeMillis;
            tlCurrentTask.set(parentTask);
        }

        public Collection<Task> getChildTasks() {
            if (childTasks == null) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableCollection(childTasks);
        }

        public boolean isRunning() {
            return !stopped;
        }

        public long getDurationTimeMillis() {
            return durationTimeMillis;
        }
    }

}
