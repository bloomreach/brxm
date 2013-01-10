/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.wicketexamples;

import org.apache.portals.messaging.PortletMessaging;
import org.apache.wicket.RequestContext;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.portlet.PortletRequestContext;

/**
 * Simple Wicket example to show JCR Node Properties.
 * 
 * @version $Id: WicketNodePropertiesViewerPage.java 18497 2009-06-11 15:50:47Z wko $
 */
public class WicketNodePropertiesViewerPage extends WebPage {
    
    protected NodeBean nodeBean = new NodeBean();

    public WicketNodePropertiesViewerPage() {
        NodeBean currentNodeBean = (NodeBean) consumeEvent("contentbrowser.current.node");
        
        if (currentNodeBean != null) {
            nodeBean = currentNodeBean;
        }
        
        add(new Label("name", new PropertyModel(nodeBean, "name")));
        add(new Label("path", new PropertyModel(nodeBean, "path")));
        add(new Label("primaryNodeTypeName", new PropertyModel(nodeBean, "primaryNodeTypeName")));
        add(new Label("uuid", new PropertyModel(nodeBean, "uuid")));
        add(new MultiLineLabel("propertiesAsString", new PropertyModel(nodeBean, "propertiesAsString")));
    }
    
    private Object consumeEvent(String name) {
        Object message = null;
        
        RequestContext requestContext = RequestContext.get();
        
        if (requestContext.isPortletRequest()) {
            message = PortletMessaging.consume(((PortletRequestContext) requestContext).getPortletRequest(), getClass().getPackage().getName(), name);
        }
        
        return message;
    }
    
}
