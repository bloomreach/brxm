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
 * 
 */
public interface Task {

    public String getName();

    public Map<String, Object> getAttributeMap();

    public Enumeration<String> getAttributeNames();

    public void setAttribute(String key, Object value);

    public Object getAttribute(String key);

    public Object removeAttribute(String key);

    public Task getParentTask();

    public Task startSubtask(String name);

    public void stop();

    public Collection<Task> getChildTasks();

    public boolean isRunning();

    public long getDurationTimeMillis();
}
