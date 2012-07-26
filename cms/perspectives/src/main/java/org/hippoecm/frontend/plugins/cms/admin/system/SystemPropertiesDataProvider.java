/*
 *  Copyright 2008 Hippo.
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
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

public class SystemPropertiesDataProvider extends SortableDataProvider {

    
    private static final long serialVersionUID = 1L;

    interface SystemProperty extends Map.Entry<String, String>, Serializable {
    }

//    SortedMap<String,String> info = new TreeMap<String,String>();
    List<Entry<String,String>> list = new ArrayList<Entry<String,String>>();
    
    public SystemPropertiesDataProvider() {
        Set<String> keys = new TreeSet<String>();
        for (Object o : System.getProperties().keySet()) {
            keys.add((String) o);
        }
        for (final String s : keys) {
            list.add(new SystemProperty()
            {
                private static final long serialVersionUID = 1L;

                public String getKey()
                {
                    return s;
                }
                public String getValue()
                {
                    return System.getProperty(s);
                }

                public String setValue(String value) {
                    throw new UnsupportedOperationException();
                }
            });
        }
//        for (Iterator< Entry<Object,Object>> iter = System.getProperties().entrySet().iterator(); iter.hasNext();) {
//            Entry<Object, Object> entry = iter.next();
//            props.put((String) entry.getKey(), (String) entry.getValue());
//        }
    }

    public Iterator<Entry<String, String>> iterator(int first, int count) {
        return list.subList(first, first + count).iterator();
    }

    public IModel model(final Object object) {
        return new AbstractReadOnlyModel() {
        private static final long serialVersionUID = 1L;
        public Object getObject() {
            return object;
        }};
    }

    public int size() {
        return list.size();
    }
}
