/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.system;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;

public class SystemPropertiesDataProvider extends SortableDataProvider {

    interface SystemProperty extends Map.Entry<String, String>, Serializable {
    }

    private final List<Entry<String,String>> list = new ArrayList<>();

    public SystemPropertiesDataProvider() {
        final Set<String> keys = new TreeSet<>();
        for (final Object o : System.getProperties().keySet()) {
            keys.add((String) o);
        }
        for (final String s : keys) {
            list.add(new SystemProperty()
            {

                public String getKey()
                {
                    return s;
                }

                public String getValue()
                {
                    return System.getProperty(s);
                }

                public String setValue(final String value) {
                    throw new UnsupportedOperationException();
                }
            });
        }
    }

    @Override
    public Iterator<Entry<String, String>> iterator(final long first, final long count) {
        return list.subList((int) first, (int) (first + count)).iterator();
    }

    @Override
    public IModel model(final Object object) {
        return () -> object;
    }

    @Override
    public long size() {
        return list.size();
    }
}
