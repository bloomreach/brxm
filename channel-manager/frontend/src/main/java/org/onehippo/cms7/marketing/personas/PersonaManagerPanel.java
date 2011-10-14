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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass(PersonaManagerPanel.EXT_CLASS)
public class PersonaManagerPanel extends ExtPanel {

    public static final String CSS_FILE = "Hippo.PersonaManager.PersonaManagerPanel.css";
    public static final String EXT_CLASS = "Hippo.PersonaManager.PersonaManagerPanel";
    public static final String EXT_JS_FILE = "Hippo.PersonaManager.PersonaManagerPanel.js";

    private static final String CONFIG_PERSONA_LIST_WIDTH = "persona.list.width";
    private static final int DEFAULT_PERSONA_LIST_WIDTH = 216;

    // names of all available avatars; should exactly match a part of the file names of the icons
    private static enum AvatarName { black, blue, green, orange, pink, purple, red, yellow };

    // all available avatar sizes (in pixels), i.e. both width and height (all avatar icons are square)
    private static enum AvatarSize { large(120), small(20);

        private final int size;

        private AvatarSize(int size) {
            this.size = size;
        }

        final int getSize() {
            return size;
        }

    };

    private IPluginConfig config;

    public PersonaManagerPanel(final String id, IPluginConfig config) {
        super(id);

        this.config = config;

        add(CSSPackageResource.getHeaderContribution(PersonaManagerPanel.class, CSS_FILE, true));
        add(JavascriptPackageResource.getHeaderContribution(PersonaManagerPanel.class, EXT_JS_FILE));
    }

    @Override
    public void buildInstantiationJs(final StringBuilder js, final String extClass, final JSONObject properties) {
        js.append("try { ");
        super.buildInstantiationJs(js, extClass, properties);
        js.append("} catch(exception) { console.error('Error initializing persona manager. ', exception); } ");
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        properties.put("personaListWidth", config.getAsInteger(CONFIG_PERSONA_LIST_WIDTH, DEFAULT_PERSONA_LIST_WIDTH));

        final JSONObject avatarUrls = new JSONObject();
        avatarUrls.put("large", getAvatarUrls(AvatarSize.large));
        avatarUrls.put("small", getAvatarUrls(AvatarSize.small));
        properties.put("avatarUrls", avatarUrls);

        properties.put("largeAvatarWidth", AvatarSize.large.getSize());
        properties.put("smallAvatarWidth", AvatarSize.small.getSize());
    }

    private static JSONObject getAvatarUrls(AvatarSize size) throws JSONException {
        RequestCycle rc = RequestCycle.get();
        JSONObject result = new JSONObject();

        for (AvatarName name : AvatarName.values()) {
            String resourceName = avatarResourceName(name, size);
            ResourceReference personaAvatar = new ResourceReference(PersonaManagerPanel.class, resourceName);
            result.put(name.toString(), rc.urlFor(personaAvatar));
        }

        return result;
    }

    private static String avatarResourceName(AvatarName name, AvatarSize size) {
        StringBuilder b = new StringBuilder("persona-");
        b.append(name.toString());
        b.append('-');
        b.append(size.getSize());
        b.append(".png");
        return b.toString();
    }

}
