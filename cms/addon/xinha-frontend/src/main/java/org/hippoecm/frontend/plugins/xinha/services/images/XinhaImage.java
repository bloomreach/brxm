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
package org.hippoecm.frontend.plugins.xinha.services.images;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.XinhaUtil;
import org.hippoecm.frontend.plugins.xinha.dialog.DocumentLink;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaImage extends DocumentLink {
    private static final long serialVersionUID = 1L;

    final static String SVN_ID = "$Id$";
    
    static final Logger log = LoggerFactory.getLogger(XinhaImage.class);
    
    final static String BINARIES_PREFIX = "binaries";

    public static final String BASE = "f_base";
    public static final String URL = "f_url";
    public static final String ALT = "f_alt";
    public static final String BORDER = "f_border";
    public static final String ALIGN = "f_align";
    public static final String VERTICAL_SPACE = "f_vert";
    public static final String HORIZONTAL_SPACE = "f_horiz";
    public static final String WIDTH = "f_width";
    public static final String HEIGHT = "f_height";

    public XinhaImage(Map<String, String> values, JcrNodeModel parentModel) {
        super(values, parentModel);
    }

    public void setUrl(String url) {
        put(URL, url);
    }

    public String getUrl() {
        return (String) get(URL);
    }
    
    @Override
    protected JcrNodeModel createInitialModel(JcrNodeModel parentModel) {
        String url = getUrl();
        if (url != null && url.startsWith(BINARIES_PREFIX)) {

            // find the nodename of the facetselect
            String path = XinhaUtil.decode(url.substring(BINARIES_PREFIX.length()));
            UserSession session = (UserSession) Session.get();
            try {
                HippoNode node = (HippoNode) session.getJcrSession().getRootNode().getNode(path).getParent();
                Node imageNode = node.getCanonicalNode();
                if (imageNode != null) {
                    return new JcrNodeModel(imageNode.getPath());
                }
            } catch (PathNotFoundException e) {
                log.error("Error retrieving canonical node for imageNode[" + path + "]", e);
            } catch (RepositoryException e) {
                log.error("Error retrieving canonical node for imageNode[" + path + "]", e);
            }
        }
        return null;
    }

}
