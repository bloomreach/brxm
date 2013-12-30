/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.columns.modify;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class MimeTypeAttributeModifier extends AbstractNodeAttributeModifier {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MimeTypeAttributeModifier.class);

    private static final CssResourceReference CSS_RESOURCE_REFERENCE = new CssResourceReference(MimeTypeAttributeModifier.class, "mimetypes.css");

    static class MimeTypeAttributeModel extends LoadableDetachableModel<String> implements IObservable {
        private static final long serialVersionUID = 1L;

        private JcrNodeModel nodeModel;
        private Observable observable;

        public MimeTypeAttributeModel(JcrNodeModel model) {
            this.nodeModel = model;
            observable = new Observable(model);
        }

        @Override
        public void detach() {
            super.detach();
            nodeModel.detach();
            observable.detach();
        }

        @Override
        protected String load() {
            Node node = nodeModel.getNode();
            observable.setTarget(null);
            if (node != null) {
                try {
                    String cssClass;
                    if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                        Node imageSet = node.getNode(node.getName());
                        try {
                            Item primItem = JcrHelper.getPrimaryItem(imageSet);
                            if (primItem.isNode() && ((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                observable.setTarget(new JcrNodeModel((Node) primItem));
                                if (!((Node) primItem).hasProperty("jcr:mimeType")) {
                                    log.warn("Unset mime type of document");
                                    return null;
                                }
                                String mimeType = ((Node) primItem).getProperty("jcr:mimeType").getString();
                                if (mimeType.startsWith("application")) {
                                    cssClass = mimeType;
                                    cssClass = StringUtils.replace(cssClass, "/", "-");
                                    cssClass = StringUtils.replace(cssClass, ".", "-");
                                } else {
                                    cssClass = StringUtils.substringBefore(mimeType, "/");
                                }
                                return "mimetype-" + cssClass + "-16";
                            } else {
                                log.warn("primary item of image set must be of type " + HippoNodeType.NT_RESOURCE);
                            }
                        } catch (ItemNotFoundException e) {
                            log.warn("ImageSet must have a primary item. " + node.getPath()
                                    + " probably not of correct image set type");
                        }
                    } else if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
                        return "folder-16";
                    } else {
                        log.warn("Node " + node.getPath() + " is not a handle or a folder");
                    }
                } catch (RepositoryException ex) {
                    log.error("Unable to determine mime type of document", ex);
                }
            }
            return null;
        }

        public void setObservationContext(IObservationContext<? extends IObservable> context) {
            observable.setObservationContext(context);
        }

        public void startObservation() {
            observable.startObservation();
        }

        public void stopObservation() {
            observable.stopObservation();
        }
    }

    @Override
    public AttributeModifier getCellAttributeModifier(Node node) {
        return new CssClassAppender(new MimeTypeAttributeModel(new JcrNodeModel(node))) {
            private static final long serialVersionUID = 1L;

            @Override
            public void bind(Component hostComponent) {
                super.bind(hostComponent);
                hostComponent.add(new Behavior() {

                    @Override
                    public void renderHead(Component component, final IHeaderResponse response) {
                        response.render(CssHeaderItem.forReference(CSS_RESOURCE_REFERENCE));
                    }
                });
            }
        };
    }

    @Override
    public AttributeModifier getColumnAttributeModifier() {
        return new CssClassAppender(new Model("icon-16"));
    }
}
