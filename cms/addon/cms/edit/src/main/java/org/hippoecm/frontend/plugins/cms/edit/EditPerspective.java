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
package org.hippoecm.frontend.plugins.cms.edit;

import java.text.MessageFormat;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(EditPerspective.class);
    
    private static final long serialVersionUID = 1L;

    public EditPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                JcrNodeModel nodeModel = (JcrNodeModel) EditPerspective.this.getDefaultModel();
                IModel<String> nodeName = new NodeTranslator(nodeModel).getNodeName();
                if (nodeModel != null) {
                    Node node = nodeModel.getNode();
                    if (node != null) {
                        try {
                            if (node.isNodeType("nt:frozenNode")) {
                                Node versionNode = node.getParent();
                                Calendar calendar = versionNode.getProperty("jcr:created").getDate();
                                MessageFormat format = new MessageFormat("{0} {1,date} {1,time}", getLocale());
                                return format.format(new Object[] { nodeName.getObject(), calendar.getTime() });
                            }
                        } catch (ValueFormatException e) {
                            log.error("Value is not a date", e);
                        } catch (PathNotFoundException e) {
                            log.error("Could not find node", e);
                        } catch (RepositoryException e) {
                            log.error("Repository error", e);
                        }
                    }
                }
                return nodeName.getObject();
            }

        };
    }

}
