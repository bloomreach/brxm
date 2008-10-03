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
package org.hippoecm.frontend.plugins.xinha.modal.linkpicker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerContentPanel extends XinhaContentPanel<XinhaLink> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(LinkPickerContentPanel.class);

    protected final static String DEFAULT_JCR_PATH = "/content";

    private final static String HTTP_PREFIX = "http://";
    private final static String HTTPS_PREFIX = "https://";
    private final static String MAILTO_PREFIX = "mailto:";
    private final static String FTP_PREFIX = "ftp://";
    private final static String[] EXTERNAL_LINK_PREFIXES = new String[] { HTTP_PREFIX, HTTPS_PREFIX, MAILTO_PREFIX,
            FTP_PREFIX };

    enum LinkType {
        INTERNAL, EXTERNAL;
    }

    private String linkTypeDropDownSelected;
    private AbstractLinkPicker linkPicker;
    private Map<String, AbstractLinkPicker> pickerCache = new HashMap<String, AbstractLinkPicker>();
    private InternalLinkDAO dao;

    public LinkPickerContentPanel(final XinhaModalWindow modal, final EnumMap<XinhaLink, String> values,
            final InternalLinkDAO dao) {
        super(modal, values);
        this.dao = dao;

        final LinkType initialType = getLinkType();
        final String initialHref = values.get(XinhaLink.HREF);

        if (initialHref != null && !"".equals(initialHref)) {
            form.replace(new RemoveButtonPanel() {
                private static final long serialVersionUID = 1L;

                @Override
                void remove(AjaxRequestTarget target, Form form) {
                    if (initialType == LinkType.INTERNAL) {
                        //TODO: remove facet
                        if (!dao.remove(initialHref)){
                            log.warn("Failed to remove internallink[" + initialHref + "]");
                        }
                    }
                    values.put(XinhaLink.HREF, "");
                    modal.onSelect(target, getSelectedValue());
                }

                @Override
                String getButtonValue() {
                    if (initialType == LinkType.INTERNAL) {
                        return "Remove internal link[" + initialHref + "]";
                    } else {
                        return "Remove external link";
                    }
                }
            });
        }
        setLinkPickerType(initialType);

        final DropDownChoice types = new DropDownChoice("linkTypeDropDown", new PropertyModel(this,
                "linkTypeDropDownSelected"), getChoicesModel());
        types.setOutputMarkupId(true);
        types.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                setLinkPickerType(LinkType.valueOf(linkTypeDropDownSelected));
                target.addComponent(linkPicker.getMarkupProvider());
            }
        });
        form.add(types);
    }

    private LinkType getLinkType() {
        if (linkTypeDropDownSelected != null)
            return LinkType.valueOf(linkTypeDropDownSelected);

        String href = values.get(XinhaLink.HREF);
        for (String prefix : EXTERNAL_LINK_PREFIXES) {
            if (href.startsWith(prefix)) {
                return LinkType.EXTERNAL;
            }
        }
        return LinkType.INTERNAL;
    }

    private IModel getChoicesModel() {
        return new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                List<String> list = new ArrayList<String>();
                for (LinkType type : LinkType.values()) {
                    list.add(type.toString());
                }
                return list;
            }
        };
    }

    private void setLinkPickerType(LinkType type) {
        String key = type.toString();
        if (!pickerCache.containsKey(key)) {
            switch (type) {
            case INTERNAL:
                pickerCache.put(key, new InternalLinkPicker());
                break;
            case EXTERNAL:
                pickerCache.put(key, new ExternalLinkPicker());
                break;
            }
        }
        form.addOrReplace(linkPicker = pickerCache.get(key));
        linkTypeDropDownSelected = key;
    }

    @Override
    protected void onOk() {
        IValidLinkProvider linkProvider = linkPicker;
        values.put(XinhaLink.HREF, linkProvider.createValidLink());
    }

    @Override
    protected String getXinhaParameterName(XinhaLink k) {
        return k.getValue();
    }

    interface IValidLinkProvider {
        String createValidLink();
    }

    abstract class AbstractLinkPicker extends Fragment implements IValidLinkProvider {
        private static final long serialVersionUID = 1L;

        public AbstractLinkPicker(String id, String markupId) {
            super(id, markupId, form);
            setOutputMarkupId(true);
        }
    }

    class ExternalLinkPicker extends AbstractLinkPicker {
        private static final long serialVersionUID = 1L;

        private String href;

        public ExternalLinkPicker() {
            super("linkPickerWidget", "externalLink");

            if (hasExternalLinkPrefix(values.get(XinhaLink.HREF))) {
                href = values.get(XinhaLink.HREF);
            }

            final TextField urlField = new TextField("externalLinkUrl", new PropertyModel(this, "href"));
            urlField.add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (href != null && href.length() > 0) {
                        target.addComponent(ok.setEnabled(true));
                    } else {
                        target.addComponent(ok.setEnabled(false));
                    }
                }
            });
            add(urlField);
        }

        public String createValidLink() {
            if (href == null || "".equals(href))
                return "";
            if (!hasExternalLinkPrefix(href))
                return HTTP_PREFIX + href;
            return href;
        }

        private boolean hasExternalLinkPrefix(String href) {
            for (String prefix : EXTERNAL_LINK_PREFIXES) {
                if (href.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void onBeforeRender() {
            if (href != null && href.length() > 0)
                ok.setEnabled(true);
            else
                ok.setEnabled(false);
            super.onBeforeRender();
        }
    }

    class InternalLinkPicker extends AbstractLinkPicker {
        private static final long serialVersionUID = 1L;

        private String jcrBrowsePath;
        private String uuid;
        private String link;

        public InternalLinkPicker() {
            super("linkPickerWidget", "internalLink");

            String jcrBrowseStartPath = DEFAULT_JCR_PATH;
            jcrBrowsePath = DEFAULT_JCR_PATH;

            String currentLink = values.get(XinhaLink.HREF);
            if (currentLink != null && !"".equals(currentLink)) {
                try {
                    uuid = dao.getUUID(currentLink);
                    Item derefItem = dao.getItem(uuid);
                    if (derefItem.isNode()) {
                        Node deref = (Node) derefItem;
                        if (deref.isNodeType(HippoNodeType.NT_HANDLE)) {
                            // get the parent node as start path
                            jcrBrowseStartPath = deref.getParent().getPath();
                            jcrBrowsePath = deref.getPath();
                        } else {
                            log
                                    .error("docbase uuid does not refer to node of type hippo:handle: resetting link. uuid="
                                            + uuid);
                            values.put(XinhaLink.HREF, "");
                        }
                    } else {
                        log.error("docbase uuid does not refer to node but property: resetting link. uuid="
                                + uuid);
                        values.put(XinhaLink.HREF, "");
                    }
                } catch (RepositoryException e) {
                    log.error("uuid in docbase not found: resetting link: " + currentLink);
                    values.put(XinhaLink.HREF, "");
                }
            }

            // path location display
            final Label pathLabel = new Label("pathLabel", new PropertyModel(this, "jcrBrowsePath"));
            add(pathLabel);
            pathLabel.setOutputMarkupId(true);

            // node listing
            final List<NodeItem> items = dao.getNodeItems(jcrBrowseStartPath);
            final WebMarkupContainer wrapper = new WebMarkupContainer("wrapper");
            ListView listing = new ListView("item", items) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem item) {
                    final NodeItem nodeItem = ((NodeItem) item.getModelObject());

                    final AjaxLink nodeLink = new AjaxLink("callback") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            NodeItem nodeItem = ((NodeItem) item.getModelObject());
                            if (nodeItem.isHandle()) {
                                link = nodeItem.getDisplayName();
                                uuid = nodeItem.getUuid();
                                if (!link.equals(values.get(XinhaLink.HREF)))
                                    target.addComponent(ok.setEnabled(true));
                            } else {
                                items.clear();
                                items.addAll(dao.getNodeItems(nodeItem.getPath()));
                                target.addComponent(ok.setEnabled(false));
                                target.addComponent(wrapper);
                                link = null;
                                uuid = null;
                            }
                            jcrBrowsePath = nodeItem.getPath();
                            target.addComponent(pathLabel);

                        }
                    };
                    nodeLink.add(new Label("linkname", nodeItem.getDisplayName()));

                    nodeLink.add(new Label("icon") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (nodeItem.isHandle()) {
                                tag.put("class", "icon-16 document-16");
                            } else {
                                tag.put("class", "icon-16 folder-16");
                            }
                            super.onComponentTag(tag);
                        }
                    });
                    item.add(nodeLink);
                }
            };

            wrapper.add(listing);
            wrapper.setOutputMarkupId(true);
            add(wrapper);
        }

        @Override
        protected void onBeforeRender() {
            if (uuid != null && link != null && !link.equals(values.get(XinhaLink.HREF))) {
                ok.setEnabled(true);
            } else {
                ok.setEnabled(false);
            }
            super.onBeforeRender();
        }

        public String createValidLink() {
            if (uuid == null) {
                log.error("uuid is null. Should never be possible for internal link");
                return "";
            }
            return dao.create(link, uuid);
        }
    }

    static class NodeItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        private String path;
        private String uuid;
        private String displayName;
        private boolean isHandle;

        public NodeItem(Node listNode) throws RepositoryException {
            this(listNode, null);
        }

        public NodeItem(Node listNode, String displayName) throws RepositoryException {
            this.path = listNode.getPath();
            this.displayName = (displayName == null) ? listNode.getName() : displayName;
            if (listNode.isNodeType("mix:referenceable")) {
                this.uuid = listNode.getUUID();
            }
            if (listNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                isHandle = true;
            }
        }

        public String getUuid() {
            return uuid;
        }

        public String getPath() {
            return path;
        }

        public boolean isHandle() {
            return isHandle;
        }

        public String getDisplayName() {
            return ISO9075Helper.decodeLocalName(displayName);
        }
    }
}
