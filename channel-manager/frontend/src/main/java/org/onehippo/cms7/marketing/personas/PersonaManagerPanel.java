/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.marketing.personas;

import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass(PersonaManagerPanel.EXT_CLASS)
public class PersonaManagerPanel extends ExtPanel {

    public static final String EXT_CLASS = "Hippo.PersonaManager.PersonaManagerPanel";
    public static final String EXT_JS_FILE = "Hippo.PersonaManager.PersonaManagerPanel.js";

    public PersonaManagerPanel(final String id) {
        super(id);
        add(JavascriptPackageResource.getHeaderContribution(PersonaManagerPanel.class, EXT_JS_FILE));
    }

    @Override
    public void buildInstantiationJs(final StringBuilder js, final String extClass, final JSONObject properties) {
        js.append("try { ");
        super.buildInstantiationJs(js, extClass, properties);
        js.append("} catch(exception) { console.error('Error initializing persona manager. ', exception); } ");
    }

}
