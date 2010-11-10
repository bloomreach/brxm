/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.container;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.container.AbstractValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.dependencies.DependencyManager;
import org.hippoecm.hst.pagecomposer.dependencies.HstLinkDependencyWriter;
import org.hippoecm.hst.pagecomposer.dependencies.ext.ExtApp;
import org.hippoecm.hst.pagecomposer.dependencies.ext.ExtAppBootstrap;

/**
 * InitializationValve
 * 
 * @version $Id: InitializationValve.java 23030 2010-06-07 06:50:46Z adouma $
 */
public class PageComposerBootstrapValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;

    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        
        HttpSession session = context.getServletRequest().getSession();
        session.setAttribute(ContainerConstants.COMPOSERMODE_ATTR_NAME, "true");
        session.setAttribute(ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW_ATTR_NAME, "true");
        
        HstRequestContext requestContext = (HstRequestContext)context.getRequestContext();
        Mount mount = requestContext.getResolvedMount().getMount();

        /*
        *  get the preview URL. Note, that the 'composer' mount always needs a parent mount of type 'composermode'.
        *  If either the parent is null, OR the parent is not of type 'composermode', we cannot instantiate the composer tool
        */
        Mount parentMount = mount.getParent();
        if (parentMount == null || !parentMount.isOfType("composermode")) {
            try {
                context.getServletResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Page Composer Tool can only be started if the composer Mount is a descendant of the Mount of type 'composermode'.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        HstLinkCreator creator = requestContext.getHstLinkCreator();
        try {
            PrintWriter writer = context.getServletResponse().getWriter();
            writer.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
            writer.append("\n<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">");

            boolean devMode = requestContext.getContainerConfiguration().isDevelopmentMode();
            DependencyManager manager = new DependencyManager(devMode);
            manager.add(new ExtApp());

            String editableUrl = creator.create("/", parentMount).toUrlForm(requestContext, false);
            manager.add(new ExtAppBootstrap(editableUrl));

            manager.write(new HstLinkDependencyWriter(requestContext, writer));

            writer.append("</head>");
            writer.append("</html>");
            writer.flush();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // continue
        context.invokeNext();
    }
}
