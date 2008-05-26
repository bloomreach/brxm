/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.template.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTemplateConfig extends JavaClusterConfig implements IClusterConfig {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(JavaTemplateConfig.class);

    private IClusterConfig upstream;
    private Map<String, String> variables;

    JavaTemplateConfig(IClusterConfig upstream, String serviceId, String templateId) {
        this.upstream = upstream;
        
        List<IPluginConfig> configs = upstream.getPlugins();
        for (final IPluginConfig conf : configs) {
            addPlugin(new JavaPluginConfig() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object get(Object key) {
                    Object obj = conf.get(key);
                    if ((obj != null) && (obj instanceof String)) {
                        // values of the form scope + ":" + ... refer to keys in other scopes 
                        // Only the "template" scope is recognized.
                        String value = (String) obj;
                        if (value.indexOf(':') > 0) {
                            String scope = value.substring(0, value.indexOf(':'));
                            if ("template".equals(scope)) {
                                return JavaTemplateConfig.this.get(value.substring(value.indexOf(':') + 1));
                            } else {
                                log.warn("Unknown scope {} used in key {}", scope, key);
                            }
                        }
                    }
                    return obj;
                }

                @Override
                public Object put(Object key, Object value) {
                    return conf.put(key, value);
                }
            });
        }

        variables = new HashMap<String, String>();
        variables.put(ITemplateEngine.TEMPLATE, templateId);

        put(ITemplateEngine.ENGINE, serviceId);
    }

    public List<String> getPropertyKeys() {
        return upstream.getPropertyKeys();
    }

    @Override
    public Object get(Object key) {
        Object obj = super.get(key);
        if (obj != null) {
            return obj;
        }

        obj = upstream.get(key);
        if ((obj != null) && (obj instanceof String)) {
            // Intercept values of the form "{" + variable + "}" + ...
            // These values are rewritten using the variables
            String value = (String) obj;
            if (value.charAt(0) == '{') {
                String variable = value.substring(1, value.indexOf('}'));
                Object origValue = variables.get(variable);
                if (origValue != null) {
                    String result = origValue + value.substring(value.indexOf('}') + 1);
                    log.debug("Rewriting value {} to {}", value, result);
                    return result;
                } else {
                    log.warn("Unknown variable {} used in key {}", variable, key);
                }
            }
        }
        return obj;
    }

}
