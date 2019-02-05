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

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example {@link org.onehippo.forge.selection.frontend.provider.IValueListProvider} showing the use of grouped list items. Grouped
 * list items can be used inside a dropdown. Grouped items will be rendered as &lt;option&gt; elements inside an &lt;optgroup&gt; element.
 *
 * @author Dennis Dam
 */
public class CarsValueListProvider extends Plugin implements IValueListProvider {

    static final Logger log = LoggerFactory.getLogger(CarsValueListProvider.class);

    public CarsValueListProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(IValueListProvider.SERVICE));

        if (log.isDebugEnabled()) {
            log.debug(this.getClass().getName() + " registered under " + IValueListProvider.SERVICE);
        }
    }

    public ValueList getValueList(IPluginConfig config) {
        return getValueList("values");
    }

    public ValueList getValueList(String name) {
        return getValueList(name, null/*locale*/);
    }

    public ValueList getValueList(String name, Locale locale) {
        ValueList valuelist = new ValueList();

        // NB locale is unused in this example

        // 3 Cars from Ford : group them under the Ford header
        valuelist.add(new ListItem("fordfiesta", "Ford Fiesta", "Ford"));
        valuelist.add(new ListItem("fordtaurus", "Ford Taurus", "Ford"));
        valuelist.add(new ListItem("fordfusion", "Ford Fusion", "Ford"));

        //  1 Car from Porsche: don't use a group since it is only one item
        valuelist.add(new ListItem("porschecarrera", "Porsche Carrera"));

        // 3 Cars from Volkswagen : group them under the Volkswagen header
        valuelist.add(new ListItem("vwgolf", "Volkswagen Golf", "Volkswagen"));
        valuelist.add(new ListItem("vwbeetle", "Volkswagen Beetle", "Volkswagen"));
        valuelist.add(new ListItem("vwtouareg", "Volkswagen Touareg", "Volkswagen"));

        return valuelist;
    }

    public List<String> getValueListNames() {
        ArrayList<String> list = new ArrayList<>();
        list.add("values");
        return list;
    }

}
