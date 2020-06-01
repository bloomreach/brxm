/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.wicket.util.value.IValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConfigMapper implements Serializable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PluginConfigMapper.class);
    
    public static void populate(Object bean, IValueMap config) throws MappingException {
        try {
            Map<String, String> entries = BeanUtils.describe(bean);
            BeanUtilsBean bub = BeanUtilsBean.getInstance();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if ("class".equals(entry.getKey())) {
                    continue;
                }
                String configKey = toConfigKey(entry.getKey());
                if (config.containsKey(configKey)) {
                    bub.setProperty(bean, entry.getKey(), config.get(configKey));
                }
            }
        } catch (IllegalAccessException e) {
            throw new MappingException(e);
        } catch (InvocationTargetException e) {
            throw new MappingException(e);
        } catch (NoSuchMethodException e) {
            throw new MappingException(e);
        }
    }

    private static String toConfigKey(String camelKey) {
        StringBuilder b = new StringBuilder(camelKey.length() + 4);
        for (char ch : camelKey.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                b.append('.').append(Character.toLowerCase(ch));
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }
}
