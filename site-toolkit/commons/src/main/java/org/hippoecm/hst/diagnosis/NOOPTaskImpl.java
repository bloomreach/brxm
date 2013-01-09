/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;

/**
 * NOOPTaskImpl
 */
class NOOPTaskImpl implements Task {

    private boolean stopped;

    NOOPTaskImpl() {
    }

    @Override
    public String getName() {
        return "<noop/>";
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        return Collections.emptyMap();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        List<String> emptyAttrNames = Collections.emptyList();
        return Collections.enumeration(emptyAttrNames);
    }

    @Override
    public void setAttribute(String key, Object value) {
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public Object removeAttribute(String key) {
        return null;
    }

    @Override
    public Task getParentTask() {
        return null;
    }

    @Override
    public Task startSubtask(String name) {
        HDC.setCurrentTask(this);
        return this;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public Collection<Task> getChildTasks() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRunning() {
        return !stopped;
    }

    @Override
    public long getDurationTimeMillis() {
        return 0L;
    }

}
