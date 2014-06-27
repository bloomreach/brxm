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

import java.util.Collections;
import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleListResourceBundle
 */
public class SimpleListResourceBundle extends ListResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(SimpleListResourceBundle.class);

    private static final String MISSING_VALUE = "[<missing>]";
    private Object[][] contents;

    public SimpleListResourceBundle(final Map<String,String> contents) {
        super();
        final Map<String,String> filteredContents = filter(contents);
        this.contents = new Object[filteredContents.size()][];
        int i = 0;
        for (Map.Entry<String, String> entry : filteredContents.entrySet()) {
            this.contents[i] = new Object[]{entry.getKey(), entry.getValue()};
            i++;
        }
    }

    private Map<String, String> filter(final Map<String, String> contents) {
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : contents.entrySet()) {
            if (MISSING_VALUE.equals(entry.getValue())) {
                log.debug("Skipping key '{}' because message is '{}'", entry.getKey(), MISSING_VALUE);
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }
        return filtered;
    }

    public void setParent(ResourceBundle parent) {
        super.setParent(parent);
    }

    @Override
    protected Object[][] getContents() {
        return contents;
    }

}
