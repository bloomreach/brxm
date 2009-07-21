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

package org.hippoecm.hst.plugins.frontend.editor.description;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Resource;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.Descriptive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescriptionPicker extends Panel {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DescriptionPicker.class);

    public interface DescriptionProvider {
        List<Descriptive> load();
    }

    public static final class DescriptionProviderImpl implements DescriptionProvider {

        EditorDAO<Descriptive> dao;
        JcrNodeModel root;

        public DescriptionProviderImpl(EditorDAO<Descriptive> dao, JcrNodeModel root) {
            this.dao = dao;
            this.root = root;
        }

        public List<Descriptive> load() {
            List<Descriptive> descriptives = new ArrayList<Descriptive>();
            Node pages = root.getNode();
            try {
                if (pages.hasNodes()) {
                    for (NodeIterator it = pages.getNodes(); it.hasNext();) {
                        Node page = it.nextNode();
                        descriptives.add(dao.load(new JcrNodeModel(page.getPath())));
                    }
                }
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            return descriptives;
        }
    }

    private DescriptionProvider provider;
    private List<Descriptive> descriptives;
    private int current;

    public DescriptionPicker(String id, IModel model, DescriptionProvider provider) {
        super(id, model);
        setOutputMarkupId(true);

        this.provider = provider;

        init();

        AjaxLink prev = new AjaxLink("prev") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                current--;
                target.addComponent(DescriptionPicker.this);
            }

            @Override
            public boolean isEnabled() {
                return current > 0;
            }
        };
        prev.add(new Image("prevImg", new ResourceReference(DescriptionPicker.class, "prev.png")));
        add(prev);

        AjaxLink next = new AjaxLink("next") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                current++;
                target.addComponent(DescriptionPicker.this);
            }

            @Override
            public boolean isEnabled() {
                return DescriptionPicker.this.descriptives.size() > current + 1;
            }
        };
        next.add(new Image("nextImg", new ResourceReference(DescriptionPicker.class, "next.png")));
        add(next);

        WebMarkupContainer selector = new WebMarkupContainer("selector");
        selector.setOutputMarkupId(true);
        selector.add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                try {
                    DescriptionPicker.this.setModelObject(DescriptionPicker.this.descriptives.get(current).getModel()
                            .getNode().getName());
                } catch (RepositoryException e) {
                    log.error("Error setting new page name", e);
                }
                target.addComponent(DescriptionPicker.this);
            }
            
            @Override
            public boolean isEnabled(Component component) {
                return !isCurrentSelected();
            }
        });
        selector.add(new AttributeAppender("class", new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                if (isCurrentSelected()) {
                    return "selected";
                }
                return "";
            }

        }, " "));
        add(selector);

        AjaxLink select = new AjaxLink("select") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    DescriptionPicker.this.setModelObject(DescriptionPicker.this.descriptives.get(current).getModel()
                            .getNode().getName());
                } catch (RepositoryException e) {
                    log.error("Error setting new page name", e);
                }
                target.addComponent(DescriptionPicker.this);
            }

            @Override
            public boolean isEnabled() {
                return isCurrentSelected();
            }
        };
        select.add(new Label("selectLabel", new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                if (isCurrentSelected()) {
                    return DescriptionPicker.this.getString("selected");
                }
                return DescriptionPicker.this.getString("unselected");
            }
        }));
        selector.add(select);

        selector.add(new Label("name", new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                try {
                    return DescriptionPicker.this.descriptives.get(current).getModel().getNode().getName();
                } catch (RepositoryException e) {
                    log.error("Error retrieving nodename");
                }
                return "- Name not found -";
            }
        }));

        selector.add(new NonCachingImage("thumb", new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return DescriptionPicker.this.descriptives.get(current).getIconResource();
            }

        }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Resource getImageResource() {
                return DescriptionPicker.this.descriptives.get(current).getIconResource();
            }

            @Override
            protected ResourceReference getImageResourceReference() {
                if (DescriptionPicker.this.descriptives.get(current).getIconResource() == null) {
                    return new ResourceReference(DescriptionPicker.class, "no_thumb.jpg");
                }
                setImageResourceReference(null);
                return null;
            }

        });

        selector.add(new Label("description", new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return DescriptionPicker.this.descriptives.get(current).getDescription();
            }
        }));

    }

    private void init() {
        descriptives = provider.load();
        current = 0;
        String selected = getModelObjectAsString();
        if (selected != null) {
            int cnt = 0;
            for (Descriptive d : descriptives) {
                try {
                    if (d.getModel().getNode().getName().equals(selected)) {
                        current = cnt;
                        break;
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                cnt++;
            }
        }
    }

    public void refresh() {
        init();
    }

    private boolean isCurrentSelected() {
        String selectedName = getModelObjectAsString();
        String currentName;
        try {
            currentName = descriptives.get(current).getModel().getNode().getName();
            if (selectedName != null && selectedName.equals(currentName)) {
                return true;
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return false;
    }
}
