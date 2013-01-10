/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.container;

import javax.servlet.http.HttpServletRequest;

/**
 * HstContainerRequest
 * <P>
 * Marker interface for the container request wrapper
 * </P>
 * @version $Id$
 */
public interface HstContainerRequest extends HttpServletRequest {
    
    /**
     * Returns path suffix splitted from the request URI by the specified path suffix delimiter. 
     * @return
     */
    String getPathSuffix();
    
}
