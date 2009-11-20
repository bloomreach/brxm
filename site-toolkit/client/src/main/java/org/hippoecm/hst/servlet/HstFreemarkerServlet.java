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
        
        TemplateLoader defaultLoader = conf.getTemplateLoader();
        
        // repository template loader
        TemplateLoader repositoryLoader = new RepositoryTemplateLoader();
        
        TemplateLoader[] loaders = new TemplateLoader[] { defaultLoader, repositoryLoader };
        TemplateLoader multiLoader = new MultiTemplateLoader(loaders);
        conf.setTemplateLoader(multiLoader);
        conf.setLocalizedLookup(false);
        
    }

}
