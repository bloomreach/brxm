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
package org.hippoecm.hst.component.support.ocm;

import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class TestBaseHstComponent {

    protected ServletContext servletContext;
    protected ServletConfig servletConfig;
    protected ComponentConfiguration compConfig;
    
    @Before
    public void setUp() throws Exception {
        this.servletContext = new MockServletContext() {
            @Override
            public InputStream getResourceAsStream(String path) {
                if (BaseHstComponent.DEFAULT_OCM_ANNOTATED_CLASSES_CONF.equals(path)) {
                    return getClass().getResourceAsStream("ocm-annotated-classes.xml");
                } else {
                    return super.getResourceAsStream(path);
                }
            }
        };
        
        this.servletConfig = new MockServletConfig(servletContext);
        
        this.compConfig = new ComponentConfiguration() {
            public Object getResolvedProperty(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
                return null;
            }
        };
    }
    
    @Test
    public void testBaseHstComponent() {
        BaseHstComponent baseHstComponent = new BaseHstComponent();
        baseHstComponent.init(this.servletConfig, this.compConfig);
    }
    
}
