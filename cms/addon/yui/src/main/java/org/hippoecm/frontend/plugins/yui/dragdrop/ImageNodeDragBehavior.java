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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class ImageNodeDragBehavior extends NodeDragBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private String parentNodePath;
    
    public ImageNodeDragBehavior(IPluginContext context, IPluginConfig config, String nodePath) {
        super(context, config, nodePath);
        
        JcrNodeModel model = new JcrNodeModel(nodePath);
        try {
            Node node = model.getNode().getParent();
            parentNodePath = node.getPath();
        } catch (ItemNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (AccessDeniedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    String getModelType() {
        return "YAHOO.hippo.DDImage";
    }

    @Override
    Map<String, Object> getHeaderContributorVariables() {
        Map<String, Object> original = super.getHeaderContributorVariables();
        Map<String, Object> nieuw = new MiniMap(original.size() + 1);
        nieuw.putAll(original);
        nieuw.put("customConfig", "{nodePath: '" + parentNodePath + "'}");
        return nieuw;
    }
  
    @Override
    protected String getHeaderContributorFilename() {
        return "DragImageNode.js"; 
    }
}
