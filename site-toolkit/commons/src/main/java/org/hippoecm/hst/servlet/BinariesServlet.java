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
package org.hippoecm.hst.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.repository.api.HippoNodeType;

public class BinariesServlet extends HttpServlet {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HstRequest hstRequest = null;
        String path = null;
        
        if (req instanceof HstRequest) {
            hstRequest = (HstRequest) req;
            path = hstRequest.getResourceID();
            
            if (path.indexOf(':') > 0) {
                path = path.substring(path.indexOf(':') + 1);
            }
        } else {
            return;
        }
        
        try {
            Session session = hstRequest.getRequestContext().getSession();
            
            Item item = session.getItem(path);

            if (item == null) {
                log("item at path " + path + " not found, response status = 404)");
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!item.isNode()) {
                log("item at path " + path + " is not a node, response status = 415)");
                res.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            Node node = (Node) item;

            if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                try {
                    Item resource = node.getPrimaryItem();
                    if (resource.isNode() && ((Node) resource).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        node = (Node) resource;
                    } else {
                        log("expected a hippo:resoource node as primary item.");
                    }
                } catch (ItemNotFoundException e) {
                    log("No primary item found for binary");
                }

            }

            if (!node.hasProperty("jcr:mimeType")) {
                log("item at path " + path + " has no property jcr:mimeType, response status = 415)");
                res.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            String mimeType = node.getProperty("jcr:mimeType").getString();

            if (!node.hasProperty("jcr:data")) {
                log("item at path " + path + " has no property jcr:data, response status = 404)");
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Property data = node.getProperty("jcr:data");
            InputStream istream = data.getStream();

            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType(mimeType);

            // TODO add a configurable factor + default minimum for expires. Ideally, this value is
            // stored in the repository
            if(node.hasProperty("jcr:lastModified")) {
                long lastModified = 0;
                try {
                    lastModified = node.getProperty("jcr:lastModified").getDate().getTimeInMillis();
                } catch (ValueFormatException e) {
                    log("jcr:lastModified not of type Date");
                }
                
                long expires = 0;
                if(lastModified > 0) {
                    expires = (System.currentTimeMillis() - lastModified);
                }
                res.setDateHeader("Expires", expires + System.currentTimeMillis());
                res.setDateHeader("Last-Modified", lastModified); 
                res.setHeader("Cache-Control", "max-age="+(expires/1000));
            } else {
                res.setDateHeader("Expires", 0);
                res.setHeader("Cache-Control", "max-age=0");
            }
            
            
            OutputStream ostream = res.getOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = istream.read(buffer)) >= 0) {
                ostream.write(buffer, 0, len);
            }
        } catch (PathNotFoundException ex) {
            log("PathNotFoundException with message " + ex.getMessage() + " while getting binary data stream item "
                    + "at path " + path + ", response status = 404)");
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException ex) {
            log("Repository exception while resolving binaries request '" + req.getRequestURI() + "' : " + ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
