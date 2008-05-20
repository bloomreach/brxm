/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.core.impl;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.apache.wicket.Session;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.hippoecm.frontend.sa.core.Plugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginFactory implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginFactory.class);

    public PluginFactory() {
    }

    public Plugin createPlugin(IPluginConfig config) {
        String className = config.getString(Plugin.CLASSNAME);
        if (className != null) {
            try {
                ClassLoader loader = ((UserSession) Session.get()).getClassLoader();
                Class clazz = Class.forName(className, true, loader);
                Class[] formalArgs = new Class[] {};
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] {};
                return (Plugin) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }
}
