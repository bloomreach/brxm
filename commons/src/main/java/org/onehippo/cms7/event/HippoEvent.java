/*
 *  Copyright 2012 Hippo.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Event for Hippo.  Can be used in a fluent style to build.
 * <p>
 * When creating a subclass, it is possible to keep the fluent style by extending as
 * <code>
 *     public class MyEvent&lt;E extends MyEvent&gt; extends HippoEvent&lt;E&gt; {
 *
 *     }
 * </code>
 * </p>
 */
public class HippoEvent<E extends HippoEvent<E>> {

    private static final String ACTION = "action";
    private static final String APPLICATION = "application";
    private static final String CATEGORY = "category";
    private static final String MESSAGE = "message";
    private static final String RESULT = "result";
    private static final String USER = "user";

    private static final Set<String> BUILTIN_KEYS;

    static {
        BUILTIN_KEYS = new HashSet<String>(6);
        BUILTIN_KEYS.add(ACTION);
        BUILTIN_KEYS.add(APPLICATION);
        BUILTIN_KEYS.add(CATEGORY);
        BUILTIN_KEYS.add(MESSAGE);
        BUILTIN_KEYS.add(RESULT);
        BUILTIN_KEYS.add(USER);
    }

    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private boolean sealed;

    public HippoEvent(String application) {
        put(APPLICATION, application);
        put(RESULT, "success");
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

    public E set(String key, Object value) {
        if (isSealed()) {
            throw new IllegalStateException("Event cannot be modified after it has been sealed.");
        }
        if (BUILTIN_KEYS.contains(key)) {
            throw new UnsupportedOperationException(key + " is a builtin property.");
        }
        return put(key, value);
    }
    
    protected E put(String key, Object value) {
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
}
