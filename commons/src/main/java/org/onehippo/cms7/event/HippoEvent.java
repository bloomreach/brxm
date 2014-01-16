/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Event for Hippo.  A bag of properties with a number of pre-defined keys.
 * Can be used in a fluent style to build; sub-classes are encouraged to follow the same pattern.
 * The event can be sealed when no further changes need to be made.  It is immutable after sealing, so it
 * is safe to access concurrently.
 * <p>
 * When creating a subclass, it is possible to keep the fluent style by extending as
 * <code>
 *     public class MyEvent&lt;E extends MyEvent&gt; extends HippoEvent&lt;E&gt; {
 *
 *     }
 * </code>
 * Note that subclasses should not introduce any fields of their own.
 */
public class HippoEvent<E extends HippoEvent<E>> implements Cloneable {

    private static final String ACTION = "action";
    private static final String APPLICATION = "application";
    private static final String CATEGORY = "category";
    private static final String MESSAGE = "message";
    private static final String RESULT = "result";
    private static final String TIMESTAMP = "timestamp";
    private static final String USER = "user";
    private static final String SYSTEM = "system";

    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private boolean sealed;

    public HippoEvent(String application) {
        put(APPLICATION, application);
        put(TIMESTAMP, System.currentTimeMillis());
    }

    /**
     * Copy constructor
     *
     * @param event the to-be-copied event
     */
    public HippoEvent(HippoEvent<?> event) {
        for (Map.Entry<String, Object> entry : event.getValues().entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void sealEvent() {
        this.sealed = true;
    }

    public boolean isSealed() {
        return sealed;
    }

    public E application(String application) {
        return put(APPLICATION, application);
    }

    public String application() {
        return get(APPLICATION);
    }

    public E user(String user) {
        return put(USER, user);
    }

    public String user() {
        return get(USER);
    }

    public E action(String action) {
        return put(ACTION, action);
    }

    public String action() {
        return get(ACTION);
    }

    public E category(String category) {
        return put(CATEGORY, category);
    }

    public String category() {
        return get(CATEGORY);
    }

    public E timestamp(long timestamp) {
        return put(TIMESTAMP, Long.valueOf(timestamp));
    }

    public long timestamp() {
        return (Long) get(TIMESTAMP);
    }

    public E result(String result) {
        return put(RESULT, result);
    }

    public String result() {
        return get(RESULT);
    }

    public E message(String message) {
        return put(MESSAGE, message);
    }

    public String message() {
        return get(MESSAGE);
    }

    public Boolean system() {
        return get(SYSTEM);
    }

    public E system(Boolean system) {
        return put(SYSTEM, system);
    }

    public E set(String key, Object value) {
        return put(key, value);
    }
    
    protected E put(String key, Object value) {
        if (isSealed()) {
            throw new IllegalStateException("Event cannot be modified after it has been sealed.");
        }
        if (key == null) {
            return (E) this;
        } else if (value == null) {
            this.attributes.remove(key);
        } else {
            this.attributes.put(key, value);
        }
        return (E) this;
    }
    
    public <T> T get(String key) {
        return (T) this.attributes.get(key);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(this.attributes);
    }

    @Override
    public E clone() {
        try {
            final E clone = (E) super.clone();
            final HippoEvent created = clone;
            created.sealed = false;
            return clone;
        } catch (CloneNotSupportedException e) {
            // not possible by Object definition
            throw new IllegalStateException(e);
        }
    }
}
