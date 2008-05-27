/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.menu.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadExportLink extends Link {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DownloadExportLink.class);

    public DownloadExportLink(String id, JcrNodeModel nodeModel) {
        super(id, nodeModel);
    }

    @Override
    public void onClick() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        JcrExportRequestTarget rsrt = new JcrExportRequestTarget(model.getNode());
        RequestCycle.get().setRequestTarget(rsrt);
    }

    private class JcrExportRequestTarget implements IRequestTarget {
        private static final long serialVersionUID = 1L;
        
        private File tempFile;
        private FileInputStream fis;
        private Node node;

        JcrExportRequestTarget(Node node) {
            this.node = node;
        }

        /**
         * @see org.apache.wicket.IRequestTarget#respond(org.apache.wicket.RequestCycle)
         */
        public void respond(RequestCycle requestCycle) {

            final Application app = Application.get();
            
            // Determine encoding
            final String encoding = app.getRequestCycleSettings().getResponseRequestEncoding();

            // Set content type based on markup type for page
            final WebResponse response = (WebResponse)requestCycle.getResponse();
            response.setCharacterEncoding(encoding);
            response.setContentType("text/xml; charset=" + encoding);
            
            // Make sure it is not cached by a client
            response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setLastModifiedTime(Time.now());

            // set filename
            response.setAttachmentHeader("export.xml");

            try {
                tempFile = File.createTempFile("export-" + Time.now().toString() + "-", ".xml");
                FileOutputStream fos = new FileOutputStream(tempFile);
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    try {
                        node.getSession().exportSystemView(node.getPath(), bos, false, false);
                        fis = new FileInputStream(tempFile);
                        response.write(fis);
                    } finally {
                        bos.close();
                    }
                } finally {
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                log.error("Tempfile missing during export", e);
            } catch (IOException e) {
                log.error("IOException during export", e);
            } catch (RepositoryException e) {
                log.error("Repository during export", e);
            }
        }

        /**
         * @see org.apache.wicket.IRequestTarget#detach(org.apache.wicket.RequestCycle)
         */
        public void detach(RequestCycle requestCycle) {
            try {
                fis.close();
            } catch (IOException e) {
                // ignore
            }
            tempFile.delete();
        }
    }
}
