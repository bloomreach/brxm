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
package org.hippoecm.hst.core;

public interface HSTHttpAttributes {
	//filter request attributes
	public static final String CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE = "current.contextbase.content";
	public static final String CURRENT_HSTCONFIGURATION_CONTEXTBASE_REQ_ATTRIBUTE = "current.contextbase.hstconfiguration";
	public static final String URI_PREFIX_REQ_ATTRIBUTE = "currentUrlbase";
	public static final String URL_MAPPING_ATTR = "hst.urlmapping";
	public static final String JCRSESSION_MAPPING_ATTR = "hst.jcrsession.attr";
	
	//request attributes
    public static final String CURRENT_PAGE_CONTAINER_NAME_REQ_ATTRIBUTE = "currentPageContainer";
    public static final String CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE = "currentPageModule";
    public static final String CURRENT_PAGE_NODE_REQ_ATTRIBUTE = "currentPageNode";
    
    public static final String ORIGINAL_REQUEST_URI_REQ_ATTRIBUTE = "original.requestURI";
    
    public static final String REQUEST_IGNORE_HSTPROCESSING_REQ_ATTRIBUTE = "ignore.hst.proccessing";
    
    //session attributes
    public static final String MODULE_RENDER_MAP_SESSION_ATTRIBUTE = "";
    
    
}
