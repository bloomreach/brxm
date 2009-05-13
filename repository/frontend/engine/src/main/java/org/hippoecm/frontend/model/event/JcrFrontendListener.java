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
package org.hippoecm.frontend.model.event;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.FrontendNodeTypes;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrFrontendListener extends NodeModelWrapper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrFrontendListener.class);

    private JcrEventListener listener;
    private IObservationContext obContext;

    public JcrFrontendListener(IObservationContext obContext, JcrNodeModel nodeModel) {
        super(nodeModel);
        this.obContext = obContext;
    }

    private String[] getMultiString(String name) throws RepositoryException {
        String[] result = null;
        Node node = getNodeModel().getNode();
        if (node.hasProperty(FrontendNodeTypes.UUIDS)) {
            Value[] values = node.getProperty(FrontendNodeTypes.UUIDS).getValues();
            result = new String[values.length];
            int i = 0;
            for (Value value : values) {
                result[i++] = value.getString();
            }
        }
        return result;
    }

    public void start() {
        Node node = getNodeModel().getNode();
        if (node != null) {
            try {
                boolean deep = false;
                if (node.hasProperty(FrontendNodeTypes.DEEP)) {
                    deep = node.getProperty(FrontendNodeTypes.DEEP).getBoolean();
                }

                listener = new JcrEventListener(obContext, new Long(node.getProperty(FrontendNodeTypes.EVENTS)
                        .getLong()).intValue(), node.getProperty(FrontendNodeTypes.PATH).getString(), deep,
                        getMultiString(FrontendNodeTypes.UUIDS), getMultiString(FrontendNodeTypes.NODETYPES));

                listener.start();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public void stop() {
        if (listener != null) {
            listener.stop();
        }
    }

}
