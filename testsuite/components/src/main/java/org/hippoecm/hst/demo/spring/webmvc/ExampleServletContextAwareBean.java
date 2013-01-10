/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.spring.webmvc;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

public class ExampleServletContextAwareBean implements ServletContextAware, InitializingBean {
    
    private ServletContext servletContext;
    private String greeting;
    private String binariesPrefixPath;
    
    public ExampleServletContextAwareBean() {
    }
    
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
    
    public void setBinariesPrefixPath(String binariesPrefixPath) {
        this.binariesPrefixPath = binariesPrefixPath;
    }

    public void afterPropertiesSet() throws Exception {
        System.out.println("[TEST] ExampleServletContextAwareBean is aware of servlet context: " + servletContext);
        System.out.println("[TEST] ExampleServletContextAwareBean's greeting: " + greeting);
        System.out.println("[TEST] ExampleServletContextAwareBean's binariesPrefixPath: " + binariesPrefixPath);
    }

}
