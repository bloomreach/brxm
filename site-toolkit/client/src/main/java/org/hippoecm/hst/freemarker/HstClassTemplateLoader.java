/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;

public class HstClassTemplateLoader extends ClassTemplateLoader {

    private static Logger log = LoggerFactory.getLogger(HstClassTemplateLoader.class);
    
    @SuppressWarnings("unchecked")
    public HstClassTemplateLoader(Class clazz) {
        super(clazz, "");
    }

    public Object findTemplateSource(String name) throws IOException
    {
        if(name == null || !name.startsWith("classpath:")) {
            // hst class template loaded only loads template when location (=name) starts with 'classpath:'
            return null;
        }
        Object template = super.findTemplateSource(name.substring("classpath:".length()));
        if(template == null) {
            log.warn("Did not find classpath template '{}'", name);
        }
        return template;
    }
}
