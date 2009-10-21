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
package org.hippoecm.hst.utils;

import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Class containing utility methods for bean properties
 */
public class PageContextPropertyUtils {
    
    private PageContextPropertyUtils() {
        
    }
    
    public static Object getProperty(PageContext pageContext, String beanPath) {
        Object bean = null;
        
        if (!StringUtils.isBlank(beanPath)) {
            String [] paths = StringUtils.split(beanPath, ".", 2);
            bean = pageContext.findAttribute(paths[0]);
            
            if (bean != null && paths.length > 1) {
                try {
                    bean = PropertyUtils.getProperty(bean, paths[1]);
                } catch (NestedNullException ignore) {
                    bean = null;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot find a bean by the path: " + beanPath, e);
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot find a bean by the path: " + beanPath);
        }
        
        return bean;
    }
    
}
