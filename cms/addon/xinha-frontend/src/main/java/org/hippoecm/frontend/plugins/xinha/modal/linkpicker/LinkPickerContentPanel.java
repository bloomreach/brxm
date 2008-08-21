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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaContentPanel;
import org.hippoecm.frontend.plugins.xinha.modal.XinhaModalWindow;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerContentPanel extends XinhaContentPanel<XinhaLink> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static Logger log = LoggerFactory.getLogger(LinkPickerContentPanel.class);

    private final static String DEFAULT_JCR_PATH = "/content";

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

    public LinkPickerContentPanel(final XinhaModalWindow modal, final JcrNodeModel nodeModel,
            final EnumMap<XinhaLink, String> values) {
        super(modal, nodeModel, values);

        final LinkType initialType = getLinkType();
        final String initialHref = values.get(XinhaLink.HREF);

        if (initialHref != null && !"".equals(initialHref)) {
            form.replace(new RemoveButtonPanel() {
                private static final long serialVersionUID = 1L;

                @Override
                void remove(AjaxRequestTarget target, Form form) {
                    if (initialType == LinkType.INTERNAL) {
                        //TODO: remove facet
                        Node node = nodeModel.getNode();
                        try {
                            Node facet = node.getNode(initialHref);
                            facet.remove();
                            node.save();
                        } catch (PathNotFoundException e) {
                            log.warn("Internal link[" + initialHref + "] not found in node["
                                    + nodeModel.getItemModel().getPath() + "]");
                        } catch (RepositoryException e) {
                            log.error("An error occured while removing internal link[" + initialHref + "] from node["
                                    + nodeModel.getItemModel().getPath() + "]", e);
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
                    if (nodeModel.getNode().hasNode(currentLink)) {
                        Node currentLinkNode = nodeModel.getNode().getNode(currentLink);
                        if (currentLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                            try {
                                uuid = currentLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                                Item derefItem = currentLinkNode.getSession().getNodeByUUID(uuid);
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
                            } catch (ItemNotFoundException e) {
                                log.error("uuid in docbase not found: resetting link: " + currentLink);
                                values.put(XinhaLink.HREF, "");
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("error during nodetest for " + currentLink);
                }
            }

            // path location display
            final Label pathLabel = new Label("pathLabel", new PropertyModel(this, "jcrBrowsePath"));
            add(pathLabel);
            pathLabel.setOutputMarkupId(true);

            // node listing
            final List<NodeItem> items = getNodeItems(nodeModel, jcrBrowseStartPath);
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
                                items.addAll(getNodeItems(nodeModel, nodeItem.getPath()));
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
                                tag.put("src", "skin/images/icons/document-16.png");
                            } else {
                                tag.put("src", "skin/images/icons/folder-16.png");
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

        private List<NodeItem> getNodeItems(JcrNodeModel nodeModel, String startPath) {
            List<NodeItem> items = new ArrayList<NodeItem>();
            startPath = (startPath == null) ? DEFAULT_JCR_PATH : startPath;
            try {
                Session session = nodeModel.getNode().getSession();
                Node rootNode = session.getRootNode();
                Node startNode = (Node) session.getItem(startPath);
                if (!startNode.isSame(rootNode) && !startNode.getParent().isSame(rootNode)) {
                    items.add(new NodeItem(startNode.getParent(), "[..]"));
                }
                NodeIterator listingNodesIt = startNode.getNodes();
                while (listingNodesIt.hasNext()) {
                    HippoNode listNode = (HippoNode) listingNodesIt.nextNode();
                    // nextNode can return null
                    if (listNode == null) {
                        continue;
                    }
                    items.add(new NodeItem(listNode));
                }
            } catch (PathNotFoundException e) {
                // possible for old links
                log.warn("path not found : " + e.getMessage());
            } catch (RepositoryException e) {
                log.error("RepositoryException " + e.getMessage(), e);
            }
            return items;
        }

        public String createValidLink() {
            if (uuid == null) {
                log.error("uuid is null. Should never be possible for internal link");
                return "";
            }

            Node node = nodeModel.getNode();

            /* test whether link is already present as facetselect. If true then:
             * 1) if uuid also same, use this link
             * 2) if uuid is different, create a new link 
             */

            HtmlLinkValidator htmlLinkValidator = new HtmlLinkValidator(node, link, uuid);
            String validLink = htmlLinkValidator.getValidLink();
            if (!htmlLinkValidator.isAlreadyPresent()) {
                try {
                    Node facetselect = node.addNode(validLink, HippoNodeType.NT_FACETSELECT);
                    //todo fetch corresponding uuid of the chosen imageset
                    facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
                    facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
                    facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
                    facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
                    // need a node save (the draft so no problem) to visualize images
                    node.save();
                } catch (RepositoryException e) {
                    log.error("An error occured while trying to save new image facetSelect[" + uuid + "]", e);
                    validLink = "";
                }
            }
            return validLink;
        }
    }

    private static class NodeItem implements IClusterable {
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
            return displayName;
        }
    }
    
    private static class HtmlLinkValidator {

        private String validLink;
        private boolean alreadyPresent;

        public HtmlLinkValidator(Node node, String link, String uuid) {
            visit(node, link, uuid, 0);
        }

        private void visit(Node node, String link, String uuid, int postfix) {
            try {
                String testLink = link;
                if (postfix > 0) {
                    testLink += "_" + postfix;
                }
                if (node.hasNode(testLink)) {
                    Node htmlLinkNode = node.getNode(testLink);
                    if (htmlLinkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String docbase = htmlLinkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                        if (docbase.equals(uuid)) {
                            // we already have a link for this internal link, so reuse it
                            validLink = testLink;
                            alreadyPresent = true;
                        } else {
                            // we already have a link of this name, but points to different node, hence, try with another name
                            visit(node, testLink, uuid, ++postfix);
                            return;
                        }
                    } else {
                        // there is a node which is has the same name as the testLink, but is not a facetselect, try with another name
                        visit(node, testLink, uuid, ++postfix);
                        return;
                    }
                } else {
                    validLink = testLink;
                    alreadyPresent = false;
                }
            } catch (RepositoryException e) {
                log.error("error occured while saving internal link: ", e);
            }
        }

        public String getValidLink() {
            return validLink;
        }

        public boolean isAlreadyPresent() {
            return alreadyPresent;
        }
    }
}
