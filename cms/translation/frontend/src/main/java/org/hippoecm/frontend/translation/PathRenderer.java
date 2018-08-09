/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtObservable;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass("Hippo.Translation.PathRenderer")
public final class PathRenderer extends ExtObservable {
    private static final long serialVersionUID = 1L;

    private final ILocaleProvider provider;
    private Component component;

    public PathRenderer(ILocaleProvider provider) {
        this.provider = provider;
    }

    @Override
    public void bind(Component component) {
        this.component = component;
        super.bind(component);
    }
    
    @Override
    protected JSONObject getProperties() throws JSONException {
        JSONObject properties = super.getProperties();

        JSONObject jsonLocales = new JSONObject();
        for (HippoLocale hippoLocale : provider.getLocales()) {
            JSONObject jsonLocale = new JSONObject();
            Locale locale = hippoLocale.getLocale();
            jsonLocale.put("name", hippoLocale.getDisplayName(Session.get().getLocale()));
            jsonLocale.put("country", locale.getCountry().toLowerCase());
            jsonLocales.put(hippoLocale.getName(), jsonLocale);
        }
        properties.put("locales", jsonLocales);

        JSONObject resources = new JSONObject();
        resources.put("language", new StringResourceModel("language", component).getString());
        properties.put("resources", resources);

        return properties;
    }

}
