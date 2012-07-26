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
package org.hippoecm.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceRequestTarget implements IRequestTarget {

    private static final Logger log = LoggerFactory.getLogger(JcrResourceRequestTarget.class);

    private Node node;

    public JcrResourceRequestTarget(Node node) {
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
     * @see org.apache.wicket.IRequestTarget#respond(org.apache.wicket.RequestCycle)
     */
    public void respond(RequestCycle requestCycle) {
        InputStream stream = null;
        try {
            if (node == null) {
                HttpServletResponse response = ((WebResponse) requestCycle.getResponse()).getHttpServletResponse();
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
                response.setCharacterEncoding(encoding);
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

            response.write(stream);
            stream = null;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                }
            }
        }
    }

    /**
     * @see org.apache.wicket.IRequestTarget#detach(org.apache.wicket.RequestCycle)
     */
    public void detach(RequestCycle requestCycle) {
        node = null;
    }

}
