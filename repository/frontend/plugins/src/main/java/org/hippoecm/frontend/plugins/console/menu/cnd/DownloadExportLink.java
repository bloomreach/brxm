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
package org.hippoecm.frontend.plugins.console.menu.cnd;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.Application;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadExportLink extends Link {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DownloadExportLink.class);

    private final Model selectedModel;

    public DownloadExportLink(String id, JcrNodeModel nodeModel, Model selectedModel) {
        super(id, nodeModel);
        this.selectedModel = selectedModel;
    }

    @Override
    public void onClick() {
        // sanity check
        if (selectedModel.getObject() == null || "".equals(selectedModel.getObject())) {
            return;
        }
        
        JcrNodeModel model = (JcrNodeModel) getModel();
        JcrExportRequestTarget rsrt = new JcrExportRequestTarget(model.getNode());
        RequestCycle.get().setRequestTarget(rsrt);
    }

    private class JcrExportRequestTarget implements IRequestTarget {
        private static final long serialVersionUID = 1L;

        private final Node node;

        JcrExportRequestTarget(Node node) {
            this.node = node;
        }

        /**
         * @see org.apache.wicket.IRequestTarget#respond(org.apache.wicket.RequestCycle)
         */
        public void respond(RequestCycle requestCycle) {

            // get namespace
            String selectedNs = (String) selectedModel.getObject();
            
            final Application app = Application.get();

            // Determine encoding
            final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();

            // Set content type based on markup type for page
            final WebResponse response = (WebResponse) requestCycle.getResponse();
            response.setCharacterEncoding(encoding);
            response.setContentType("text/plain; charset=" + encoding);

            // Make sure it is not cached by a client
            response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setLastModifiedTime(Time.now());

            // set filename
            response.setAttachmentHeader(selectedNs + ".cnd");

            try {
                // write file contents
                Session session = node.getSession();
                LinkedHashSet<NodeType> types = CndExportDialog.sort(CndExportDialog.getNodeTypes(session, selectedNs.concat(":")));
                Writer out = new JcrCompactNodeTypeDefWriter(session).write(types, true);
                response.write(out.toString());
            } catch (IOException e) {
                log.error("RepositoryException while exporting NodeType Definitions of namespace : " + selectedNs, e);
            } catch (RepositoryException e) {
                log.error("IOException while exporting NodeType Definitions of namespace : " + selectedNs, e);
            }
        }

        public void detach(RequestCycle requestCycle) {
            
        }
    }
}
