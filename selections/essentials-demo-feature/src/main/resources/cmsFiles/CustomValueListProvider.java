/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.{{namespace}}.provider;


import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomValueListProvider extends Plugin implements IValueListProvider {

    static final Logger log = LoggerFactory.getLogger(CustomValueListProvider.class);

    public CustomValueListProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(IValueListProvider.SERVICE));

        if (log.isDebugEnabled()) {
            log.debug(this.getClass().getName() + " registered under " + IValueListProvider.SERVICE);
        }
    }

    public ValueList getValueList(IPluginConfig config) {
        return getValueList(config.getString("source", "values"));
    }

    public ValueList getValueList(String name) {
        return getValueList(name, null/*locale*/);
    }

    public ValueList getValueList(String name, Locale locale) {
        if (!"values".equals(name)) {
            log.warn("unknown value list name " + name + " was requested, using 'values'");
        }

        ValueList valuelist = new ValueList();
        if ((locale != null) && "nl".equals(locale.getLanguage())) {
            valuelist.add(new ListItem("custom1", "Custom Waarde 1"));
            valuelist.add(new ListItem("custom2", "Custom Waarde 2"));
            valuelist.add(new ListItem("custom3", "Custom Waarde 3"));
        } else {
            valuelist.add(new ListItem("custom1", "Custom Value 1"));
            valuelist.add(new ListItem("custom2", "Custom Value 2"));
            valuelist.add(new ListItem("custom3", "Custom Value 3"));
        }

        return valuelist;
    }

    public List<String> getValueListNames() {
        ArrayList<String> list = new ArrayList<>(1);
        list.add("values");
        return list;
    }

}
