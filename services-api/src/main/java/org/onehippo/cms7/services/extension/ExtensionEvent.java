/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.extension;

import org.onehippo.cms7.event.HippoEvent;

/**
 * Registration info for a Hippo CMS extension.
 */
public class ExtensionEvent extends HippoEvent<ExtensionEvent> {

    private final ClassLoader classLoader;
    private final String hstRoot;

    public ExtensionEvent(final String application, final String hstRoot, final ClassLoader classLoader) {
        super(application);
        this.hstRoot = hstRoot;
        this.classLoader = classLoader;
    }

    public String getExtensionName() {
        return (String) getValues().get("application");
    }

    public String getHstRoot() {
        return hstRoot;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
