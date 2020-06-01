/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;

/**
 * Implementations of this interface are a request flyweight instance of the {@link VirtualHost} object, where possible wildcard property placeholders have been filled in, similar
 * to the {@link ResolvedMount} and {@link Mount}
 */
public interface ResolvedVirtualHost {

    /**
     * @return the backing virtual host of this ResolvedVirtualHost
     */
    VirtualHost getVirtualHost();

    /**
     * @param contextPath the contextPath of the {@link javax.servlet.http.HttpServletRequest}
     * @param requestPath
     * @return the {@link ResolvedMount} for this hstContainerUrl or <code>null</code> when it can not be matched to a
     *         {@link Mount}
     * @throws MatchException
     * @deprecated since 13.0.0. Use {@link #matchMount(String)} instead. The {@code contextPath} is not used any more
     */
    ResolvedMount matchMount(String contextPath, String requestPath)  throws MatchException;


    /**
     * <p> This method tries to match the current {@link ResolvedVirtualHost} for the requestPath to a
     * flyweight {@link ResolvedMount}.
     *
     * @param requestPath
     * @return the {@link ResolvedMount} for this hstContainerUrl or <code>null</code> when it can not be matched to a
     *         {@link Mount}
     * @throws MatchException
     */
    ResolvedMount matchMount(String requestPath)  throws MatchException;
    
}
