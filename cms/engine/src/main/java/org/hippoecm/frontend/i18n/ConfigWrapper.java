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
package org.hippoecm.frontend.i18n;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigWrapper implements ITranslation<IModel> {

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(ConfigWrapper.class);

    private IValueMap config;
    private Set<String> matches;

    ConfigWrapper(IValueMap config, Map<String, String> keys) {
        this.config = config;

        matches = new HashSet<String>();
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            if (config.containsKey(entry.getKey())) {
                String value = config.getString(entry.getKey());
                if (value.equals(entry.getValue())) {
                    matches.add(entry.getKey());
                }
            }
        }
    }

    public IModel getModel() {
        return new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                return config.getString(HippoNodeType.HIPPO_MESSAGE);
            }

            public void setObject(Object object) {
                config.put(HippoNodeType.HIPPO_MESSAGE, object);
            }

            public void detach() {
            }
        };
    }

    public Set<String> getMatchingCriteria() {
        return matches;
    }

}
