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
import java.util.Enumeration;
import java.util.Map;

/**
 * A unit of execution.
 * A task may start a subtask, and may contain multiple child subtasks.
 * Each task may have attributes map of necessary data needed for diagnostics.
 * By the way, the root task should be given by the container.
 */
public interface Task {

    /**
     * returns the task name
     * @return
     */
    public String getName();

    /**
     * Returns attribute map which is unmodifiable. So, do not try to put or remove items directly from the returned map.
     * @return
     */
    public Map<String, Object> getAttributeMap();

    /**
     * Enumerates the attribute names
     */
    public Enumeration<String> getAttributeNames();

    /**
     * Set an attribute for the task.
     * @param name attribute name
     * @param object attribute value
     */
    public void setAttribute(String key, Object value);

    /**
     * Retrieve the attribute value by the attribute name.
     */
    public Object getAttribute(String key);

    /**
     * Removes the attribute by the attribute name.
     */
    public Object removeAttribute(String key);

    /**
     * Returns the parent task
     * @return
     */
    public Task getParentTask();

    /**
     * Starts and returns a child subtask with the name.
     * @param name
     * @return
     */
    public Task startSubtask(String name);

    /**
     * Stops the task
     */
    public void stop();

    /**
     * Returns the child tasks collection
     * @return
     */
    public Collection<Task> getChildTasks();

    /**
     * Returns true if the task was started but not stopped.
     * @return
     */
    public boolean isRunning();

    /**
     * Returns the task execution duration time in milliseconds
     * @return
     */
    public long getDurationTimeMillis();
}
