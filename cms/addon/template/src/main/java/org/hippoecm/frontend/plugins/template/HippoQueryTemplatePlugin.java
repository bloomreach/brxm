/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionException;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoQueryTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;
   
    static final Logger log = LoggerFactory.getLogger(HippoQueryTemplatePlugin.class);

    private JcrNodeModel jcrNodeModel;
    private String language;    
    private String statement;
    
    public HippoQueryTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        
        jcrNodeModel = model.getJcrNodeModel();
        Node queryNode = jcrNodeModel.getNode();
        
        try {
            
            //if(!queryNode.isNodeType(JcrConstants.NT_QUERY))
            
            if(!queryNode.hasProperty("jcr:language")){
                queryNode.setProperty("jcr:language", "xpath");
            }
            if(!queryNode.hasProperty("jcr:statement")){
                queryNode.setProperty("jcr:statement", "//*");
            }
            
            QueryManager qrm = queryNode.getSession().getWorkspace().getQueryManager();
            Query query = qrm.getQuery(queryNode);

            language = query.getLanguage();
            statement = query.getStatement();
            
            add(new TextFieldWidget("language", new PropertyModel(this, "language") {
                @Override
                public void setObject(Object object) {
                    HippoQueryTemplatePlugin.this.language = (String) object;
                    storeQueryAsNode();
                }

            }));
            
            add(new TextFieldWidget("statement", new PropertyModel(this, "statement") {
                @Override
                public void setObject(Object object) {
                    HippoQueryTemplatePlugin.this.statement = (String) object;
                    storeQueryAsNode();
                }
            }));
              
        } catch (InvalidQueryException e){
             log.error("invalid query " + e.getMessage());
        } catch (ValueFormatException e) {
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        
        setOutputMarkupId(true);
    }
    
    private void storeQueryAsNode() {
        Node queryNode = jcrNodeModel.getNode();
        
        try {
            Node parentNode = queryNode.getParent();
            String nodeName = queryNode.getName();
            
            Session session = queryNode.getSession();

            queryNode.remove();
            /*
             * you cannot directly use storeAsNode again for with the same path, because
             * that result in an item exists exception. The only way is to keep the property
             * values in memory, remove the node, and store is again
             */ 
            QueryManager qrm = session.getWorkspace().getQueryManager();
            Query query = qrm.createQuery(statement, language);

            query.storeAsNode(parentNode.getPath()+"/"+nodeName);
            jcrNodeModel.detach();
            
        } catch (ValueFormatException e) {
            e.printStackTrace();
        } catch (VersionException e) {
            e.printStackTrace();
        } catch (LockException e) {
            e.printStackTrace();
        } catch (ConstraintViolationException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        
    }

}
