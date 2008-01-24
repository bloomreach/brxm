/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URLDecoder;

import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.ISO9075Helper;

public class DirectAccessServlet extends HttpServlet {
    static HippoRepository repository;
    Session session;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String location = config.getServletContext().getInitParameter("repository-address");

        HippoRepositoryFactory.setDefaultRepository(location);
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            if (session == null) {
                session = repository.login("admin", "admin".toCharArray());
            }
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.startsWith(req.getContextPath())) {
            path = path.substring(req.getContextPath().length());
        }
        if (path.startsWith(req.getServletPath())) {
            path = path.substring(req.getServletPath().length());
        }

        path = URLDecoder.decode(path, "UTF-8");

        String pathElt = "";
        String pathEltName = "";
        String currentPath = "";
        StringTokenizer pathElts = new StringTokenizer(path, "/");
        while (pathElts.hasMoreTokens()) {
            pathElt = pathElts.nextToken();
            pathEltName = ISO9075Helper.decodeLocalName(pathElt);
            currentPath += "/" + pathElt;
        }
        path = currentPath;

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try {
            Item item = Context.getItem(session, path);
            if (!item.isNode()) {
                res.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
            Node node = (Node) item;

            String mimeType = node.getProperty("mime-type").getString();
            InputStream istream = node.getProperty("data").getStream();

            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType(mimeType);

            OutputStream ostream = res.getOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = istream.read(buffer)) >= 0) {
                ostream.write(buffer, 0, len);
            }
        } catch(RepositoryException ex) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
