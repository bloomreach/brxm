/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.freemarker.HstClassTemplateLoader;
import org.hippoecm.hst.freemarker.RepositoryTemplateLoader;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;

public class HstFreemarkerServlet extends FreemarkerServlet {

    private static final long serialVersionUID = 1L;

    
    
    @Override
    public void init() throws ServletException {
        super.init();
        /*
         * we here need to inject our own template loader. We cannot do this in createConfiguration() as we would like,
         * because the FreemarkerServlet sets the template loader in the init() *after* createConfiguration() again, to the default
         * WebappTemplateLoader
         */  
        
        Configuration conf = super.getConfiguration();
        
        TemplateLoader classTemplateLoader =  new HstClassTemplateLoader(getClass());
        TemplateLoader defaultLoader = conf.getTemplateLoader();
        // repository template loader
        TemplateLoader repositoryLoader = new RepositoryTemplateLoader();
        TemplateLoader[] loaders = new TemplateLoader[] { defaultLoader, classTemplateLoader, repositoryLoader };
        TemplateLoader multiLoader = new MultiTemplateLoader(loaders);
        conf.setTemplateLoader(multiLoader);
        conf.setLocalizedLookup(false);
        
    }
    
    /**
     * Special dispatch info is included when the request contains the attribute {@link ContainerConstants#SPECIAL_DISPATCH_INFO}. For example
     * this value is classpath: or jcr: to load a template from a classpath or repository
     */
    @Override 
    protected String requestUrlToTemplatePath(HttpServletRequest request)
    {
        String path = super.requestUrlToTemplatePath(request);
        if(request.getAttribute(ContainerConstants.SPECIAL_DISPATCH_INFO) != null){            
            path = request.getAttribute(ContainerConstants.SPECIAL_DISPATCH_INFO) +  path;   
        }
        return path;
    }

}
