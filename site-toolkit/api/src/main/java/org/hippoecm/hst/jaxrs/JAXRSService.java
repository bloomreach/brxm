/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.jaxrs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * JAXRSService interface to be wired in the JAXRSServiceValve
 * @version $Id$
 *
 */
public interface JAXRSService {
	
	String REQUEST_CONTENT_PATH_KEY = "org.hippoecm.hst.jaxrs.request.contentPath";
	/**
	 * @deprecated We do not store the contentNode any more on the request, but refetch it instead
	 */
	@Deprecated
    String REQUEST_CONTENT_NODE_KEY = "org.hippoecm.hst.jaxrs.request.contentNode";
	/**
     * @deprecated We do not store the contentBean any more on the request, but refetch it instead
     */
	@Deprecated
    String REQUEST_CONTENT_BEAN_KEY = "org.hippoecm.hst.jaxrs.request.contentBean";
	@Deprecated
	/**
     * @deprecated We do not store the contentSiteBaseBean any more on the request, but refetch it instead
     */
    String REQUEST_CONTENT_SITE_CONTENT_BASE_BEAN_KEY = "org.hippoecm.hst.jaxrs.request.contentSiteBaseBean";
	
	void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException;
}
