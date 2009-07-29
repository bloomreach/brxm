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
package org.hippoecm.frontend.plugins.cms.browse.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;

public class TemplateTypeRenderer extends AbstractNodeRenderer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private JcrTypeStore typeStore;

    public TemplateTypeRenderer() {
        typeStore = new JcrTypeStore();
        IStore<ITypeDescriptor> builtin = new BuiltinTypeStore();
        TypeLocator locator = new TypeLocator(new IStore[] { typeStore, builtin });
        typeStore.setTypeLocator(locator);
    }

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        Node ntNode = node.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
        if (!ntNode.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
            return new Label(id, new ResourceModel("type-unknown"));
        }

        ntNode = ntNode.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
        boolean mixin = false;
        if (ntNode.hasProperty(HippoNodeType.HIPPO_MIXIN)) {
            mixin = ntNode.getProperty(HippoNodeType.HIPPO_MIXIN).getBoolean();
            if (mixin) {
                return new Label(id, new ResourceModel("type-mixin"));
            }
        }

        if (ntNode.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
            String type = ntNode.getProperty(HippoNodeType.HIPPOSYSEDIT_TYPE).getString();
            if (type.indexOf(':') < 0) {
                return new Label(id, new ResourceModel("type-primitive"));
            }
            ITypeDescriptor descriptor = typeStore.getTypeDescriptor(type);
            if (descriptor != null) {
                if (descriptor.isType(HippoNodeType.NT_DOCUMENT)) {
                    return new Label(id, new ResourceModel("type-document"));
                }
            }
        }

        if (ntNode.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
            Value[] values = ntNode.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                ITypeDescriptor descriptor = typeStore.getTypeDescriptor(value.getString());
                if (descriptor != null) {
                    if (descriptor.isType(HippoNodeType.NT_DOCUMENT)) {
                        return new Label(id, new ResourceModel("type-document"));
                    }
                }
            }
        }

        return new Label(id, new ResourceModel("type-compound"));
    }

}
