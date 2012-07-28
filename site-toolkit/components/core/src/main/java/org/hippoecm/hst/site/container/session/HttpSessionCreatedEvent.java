/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.container.session;

import javax.servlet.http.HttpSession;

import org.springframework.context.ApplicationEvent;

/**
 * Published by the {@link HttpSessionEventPublisher} when an <CODE>HttpSession</CODE> is created by the container
 */
public class HttpSessionCreatedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public HttpSessionCreatedEvent(HttpSession httpSession) {
        super(httpSession);
    }

    public HttpSession getSession() {
        return (HttpSession) getSource();
    }
}