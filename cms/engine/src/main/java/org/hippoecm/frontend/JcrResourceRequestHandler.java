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
package org.hippoecm.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.io.Streams;
import org.apache.wicket.util.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceRequestHandler implements IRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(JcrResourceRequestHandler.class);

    private Node node;

    public JcrResourceRequestHandler(Node node) {
        this.node = node;
    }

    /*
     [hippo:resource]
    - jcr:encoding (string)
    - jcr:mimeType (string) mandatory
    - jcr:data (binary) primary mandatory
    - jcr:lastModified (date) mandatory ignore
     */

    /**
     * @see IRequestHandler#respond(IRequestCycle)
     */
    public void respond(IRequestCycle requestCycle) {
        InputStream stream = null;
        try {
            if (node == null) {
                HttpServletResponse response = (HttpServletResponse) requestCycle.getResponse().getContainerResponse();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                // Make sure it is not cached by a client
                response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                response.setHeader("Cache-Control", "no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                return;
            }

            // Determine encoding
            String encoding = null;
            if (node.hasProperty("jcr:encoding")) {
                encoding = node.getProperty("jcr:encoding").getString();
            }
            String mimeType = node.getProperty("jcr:mimeType").getString();
            Calendar lastModified = node.getProperty("jcr:lastModified").getDate();
            stream = node.getProperty("jcr:data").getStream();

            // Set content type based on markup type for page
            WebResponse response = (WebResponse) requestCycle.getResponse();
            if (encoding != null) {
                response.setContentType(mimeType + "; charset=" + encoding);
            } else {
                response.setContentType(mimeType);
            }

            if (!mimeType.toLowerCase().startsWith("image/")) {
                response.setHeader("Content-Disposition", "attachment; filename=" + node.getName());
            }

            // Make sure it is not cached by a client
            response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setLastModifiedTime(Time.valueOf(lastModified.getTime()));
            try {
                Streams.copy(stream, response.getOutputStream());
            } catch (IOException ioe) {
                throw new WicketRuntimeException(ioe);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * @see IRequestHandler#detach(IRequestCycle)
     */
    public void detach(IRequestCycle requestCycle) {
        node = null;
    }

}
