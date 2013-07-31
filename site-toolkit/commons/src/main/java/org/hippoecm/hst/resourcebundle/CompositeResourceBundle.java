/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.resourcebundle;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * CompositeResourceBundle
 *
 * <P>
 * Composite Resource Bundle which looks up the internal resource bundles
 * for the key as ordered.
 * </P>
 */
public class CompositeResourceBundle extends ResourceBundle {

    private ResourceBundle [] bundles;
    private Set<String> keys;

    public CompositeResourceBundle(ResourceBundle ... bundles) {
        this.bundles = bundles;
    }

    @Override
    protected Object handleGetObject(String key) {
        if (bundles == null) {
            return null;
        }

        for (ResourceBundle bundle : bundles) {
            if (bundle != null) {
                try {
                    Object value = bundle.getObject(key);

                    if (value != null) {
                        return value;
                    }
                } catch (MissingResourceException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    @Override
    public Enumeration<String> getKeys() {
        if (keys == null) {
            synchronized (this) {
                if (keys == null) {
                    keys = new LinkedHashSet<String>();

                    if (bundles != null) {
                        for (ResourceBundle bundle : bundles) {
                            for (Enumeration keyEnum = bundle.getKeys(); keyEnum.hasMoreElements(); ) {
                                keys.add((String) keyEnum.nextElement());
                            }
                        }
                    }
                }
            }
        }

        return createEnumeration(keys.iterator());
    }

    private Enumeration<String> createEnumeration(final Iterator<String> it) {
        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }
            @Override
            public String nextElement() {
                return it.next();
            }
        };
    }
}
