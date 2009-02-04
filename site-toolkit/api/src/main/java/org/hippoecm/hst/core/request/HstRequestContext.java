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
package org.hippoecm.hst.core.request;

import javax.jcr.Repository;

import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.domain.RepositoryMapping;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.mapping.URLMappingManager;
import org.hippoecm.hst.core.template.node.PageNode;

public interface HstRequestContext {
    
    public Repository getRepository();
    
    public String getUserID();

    public RepositoryMapping getRepositoryMapping();

    public PageNode getPageNode();
    
    public URLMappingManager getURLMappingManager();
    
    public URLMapping getUrlMapping();
    
    public URLMapping getAbsoluteUrlMapping();

    public URLMapping getRelativeUrlMapping();
    
    public ContextBase getContentContextBase();

    public ContextBase getHstConfigurationContextBase();
    
    // TODO: remove this.
    public String getServerName();
    
    // TODO: remove this.
    public String getRequestURI();
    
    // TODO: remove this.
    public String getHstRequestUri();
    
}
