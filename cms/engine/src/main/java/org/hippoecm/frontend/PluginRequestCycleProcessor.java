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
package org.hippoecm.frontend;

import org.apache.wicket.Application;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.protocol.http.request.CryptedUrlWebRequestCodingStrategy;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.coding.IRequestTargetUrlCodingStrategy;
import org.apache.wicket.request.target.component.BookmarkablePageRequestTarget;
import org.apache.wicket.request.target.component.IPageRequestTarget;

public class PluginRequestCycleProcessor extends WebRequestCycleProcessor {

    @Override
    public void processEvents(RequestCycle requestCycle) {
        super.processEvents(requestCycle);

        IRequestTarget target = requestCycle.getRequestTarget();
        if (target instanceof IPageRequestTarget) {
            Page page = ((IPageRequestTarget) target).getPage();
            if (page instanceof Home) {
                ((Home) page).processEvents();
            }
        }
    }

    @Override
    public void respond(RequestCycle requestCycle) {
        IRequestTarget target = requestCycle.getRequestTarget();
        if (target instanceof IPageRequestTarget) {
            Page page = ((IPageRequestTarget) target).getPage();
            if (page instanceof Home) {
                if (target instanceof PluginRequestTarget) {
                    ((Home) page).render((PluginRequestTarget) target);
                } else {
                    ((Home) page).render((PluginRequestTarget) null);
                }
            }
        } else if (target instanceof BookmarkablePageRequestTarget) {
            BookmarkablePageRequestTarget bprt = (BookmarkablePageRequestTarget) target;
            Page page = bprt.getPage();

            // create the page instance
            if (page == null) {
                bprt.processEvents(requestCycle);
                page = bprt.getPage();
            }

            if (page instanceof Home) {
                ((Home) page).render((PluginRequestTarget) null);
            }
        }
        
        super.respond(requestCycle);
    }
    
    @Override
    protected IRequestCodingStrategy newRequestCodingStrategy() {
        Main main = (Main) Application.get();
        String encrypt = main.getInitParameter(Main.ENCRYPT_URLS);
        if ("true".equals(encrypt)) {
            return new CryptedUrlWebRequestCodingStrategy(super.newRequestCodingStrategy());
        } else {
            return super.newRequestCodingStrategy();
        }
    }

}
