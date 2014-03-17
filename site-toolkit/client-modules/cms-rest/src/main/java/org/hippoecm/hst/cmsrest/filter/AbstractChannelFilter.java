/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.cmsrest.filter;

import javax.jcr.Session;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.core.internal.CmsJcrSessionThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractChannelFilter implements Predicate<Channel> {

    private static final Logger log = LoggerFactory.getLogger(AbstractChannelFilter.class);

    protected Session getCmsSession() {
        final Session session = CmsJcrSessionThreadLocal.getJcrSession();
        if (session == null) {
            log.debug("Could not find a JCR session object instance when expected to have one already instantiated");
            throw new IllegalStateException("Could not find a JCR session object instance when expected to have one already instantiated");
        }

        return session;
    }
}
