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
package org.hippoecm.frontend.plugins.yui.datetime;

import java.util.Date;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class DateFieldWidget extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: DateFieldWidget.java 19048 2009-07-28 16:08:33Z abogaart $";

    private static final long serialVersionUID = 1L;

    public DateFieldWidget(String id, IModel<Date> model, IPluginContext context, IPluginConfig config) {
        this(id, model, false, context, config);
    }
    
    public DateFieldWidget(String id, IModel<Date> model, boolean todayLinkVisible, IPluginContext context, IPluginConfig config) {
        super(id, model);

        add(new AjaxDateTimeField("widget", model, todayLinkVisible, context, config));
    }

}
