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

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.container.AbstractValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;

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
        HstRequestContext requestContext = (HstRequestContext)context.getRequestContext();
        SiteMount siteMount = requestContext.getResolvedSiteMount().getSiteMount();

        HstLinkCreator creator = requestContext.getHstLinkCreator();
        try {
            PrintWriter writer = context.getServletResponse().getWriter();
            writer.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
            writer.append("\n<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">");
            
            // ext-css
            HstLink css = creator.create("/js/ext/resources/css/ext-all.css", siteMount, true);
            writer.append("\n<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\""+css.toUrlForm(requestContext, false)+"\"/>");
            
            // Application dependencies 
            // ext Base
            HstLink base = creator.create("/js/ext/adapter/ext/ext-base.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+base.toUrlForm(requestContext, false)+"\"></script>");

            // ext AllDebug
            HstLink allDebug = creator.create("/js/ext/ext-all-debug-w-comments.js", siteMount, true);            
            writer.append("\n<script type=\"text/javascript\" src=\""+allDebug.toUrlForm(requestContext, false)+"\"></script>");

            // extBlankImageUrl
            HstLink blankImageUrl = creator.create("/js/ext/s.gif", siteMount, true);            
            writer.append("\n<script type=\"text/javascript\">Ext.BLANK_IMAGE_URL= '"+blankImageUrl.toUrlForm(requestContext, false)+"';</script>");

            // baseApp
            HstLink baseApp = creator.create("/js/hippo-ext/app/BaseApp.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+baseApp.toUrlForm(requestContext, false)+ "\"></script>");

            // baseGrid
            HstLink baseGrid = creator.create("/js/hippo-ext/app/BaseGrid.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+baseGrid.toUrlForm(requestContext, false)+ "\"></script>");
          
            // baseGrid
            HstLink floatingWindow = creator.create("/js/hippo-ext/ux/FloatingWindow.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+floatingWindow.toUrlForm(requestContext, false)+ "\"></script>");
          
            // propsPanel
            HstLink propsPanel = creator.create("/js/hippo-ext/app/PropertiesPanel.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+propsPanel.toUrlForm(requestContext, false)+ "\"></script>");
          
            // pageModel
            HstLink pageModel = creator.create("/js/hippo-ext/app/PageModel.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+pageModel.toUrlForm(requestContext, false)+ "\"></script>");
            
            // miframe
            HstLink miframe = creator.create("/js/ext-plugins/miframe/miframe-debug.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+miframe.toUrlForm(requestContext, false)+ "\"></script>");
           
            // mifmsg
            HstLink mifmsg = creator.create("/js/ext-plugins/miframe/mifmsg.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+mifmsg.toUrlForm(requestContext, false)+ "\"></script>");
            
            // theme
            HstLink theme = creator.create("/js/ext/resources/css/xtheme-slate.css", siteMount, true);
            writer.append("\n<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" title=\"gray\" href=\""+theme.toUrlForm(requestContext, false)+"\">");
           

            // Application files 
            HstLink pageEditorStyle = creator.create("/css/hippo/PageEditor.css", siteMount, true);
            writer.append("\n<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\""+pageEditorStyle.toUrlForm(requestContext, false)+"\">");
           
            // editor
            HstLink editor = creator.create("/js/hippo-ext/PageEditor.js", siteMount, true);
            writer.append("\n<script type=\"text/javascript\" src=\""+editor.toUrlForm(requestContext, false)+ "\"></script>");

            // Application bootstrap 
            
            /*
             *  get the preview URL. Note, that the 'composer' sitemount always needs a parent sitemount of type 'composermode'. 
             *  If either the parent is null, OR the parent is not of type 'composermode', we cannot instantiate the composer tool 
             */
            
            SiteMount parentMount = siteMount.getParent();
            if(parentMount == null || !parentMount.isOfType("composermode")) {
                context.getServletResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Page Composer Tool can only be started if the composer SiteMount is 'below' the SiteMount of type 'composermode'.");
                return;
            }

            String editableUrl = creator.create("/", parentMount).toUrlForm(requestContext, false);
            
            writer.append("\n<script type=\"text/javascript\">");

                writer.append("\n\t Ext.onReady(function() {");
                    writer.append("\n\t\t //clear DOM");
                    writer.append("\n\t\t Ext.getBody().update('');");
                    writer.append("\n\t\t var config = {");
                        writer.append("\n\t\t\t iframeUrl: '"+editableUrl+"', ");
                        writer.append("\n\t\t\t rootComponentName: 'home'");
                    writer.append("\n\t\t };");
                    writer.append("\n\t\t Ext.namespace('Hippo.App');");
                    writer.append("\n\t\t Hippo.App.Main = new Hippo.App.PageEditor(config);");
                writer.append("\n\t });");
            writer.append("\n </script>");
           
            
            writer.append("</meta>");
            writer.append("</html>");
            writer.flush();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // continue
        context.invokeNext();
    }
}
