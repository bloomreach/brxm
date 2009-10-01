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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.time.Time;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceRequestTarget implements IRequestTarget {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrResourceRequestTarget.class);

    private JcrNodeModel nodeModel;

    public JcrResourceRequestTarget(JcrNodeModel node) {
        this.nodeModel = node;
    }
    protected void finalize() throws Throwable {
       try {
           nodeModel.getNode().getSession().logout();
       } catch(NullPointerException ex) {
           // deliberate ignore
       } catch(RepositoryException ex) {
           // deliberate ignore
       }
       super.finalize();
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
            Node node = nodeModel.getNode();

            // if node is facetselect, check the referenced node
            // TODO now by default return primary item. If facetselect has filter, there might be no
            // primary item visible and another resource needs to be shown
            if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                log.debug("binary links to facetselect");
                if(node.hasNodes()){
                    Node firstChild = node.getNodes().nextNode();
                    try {
                        if(firstChild.getPrimaryItem().isNode() && ((Node)firstChild.getPrimaryItem()).isNodeType(HippoNodeType.NT_RESOURCE)) {
                            log.debug("fetching hippo:resource from: " +  firstChild.getPrimaryItem().getPath());
                            node = (Node)firstChild.getPrimaryItem();
                        } else {
                            log.error("Primary item is not of type " + HippoNodeType.NT_RESOURCE + " : " + firstChild.getPrimaryItem().getPath());
                        }
                    } catch (ItemNotFoundException e) {
                        // TODO if there is no primary type, look for the first property of type HippoNodeType.NT_RESOURCE
                        // to display. Normally, the facetselect has logic to display the correct image
                        log.error("No primary item found for : " + firstChild.getPath());
                        throw (e);
                    }
                }
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
        nodeModel.detach();
    }
}
