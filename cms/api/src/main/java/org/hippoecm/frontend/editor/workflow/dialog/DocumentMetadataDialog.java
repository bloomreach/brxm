/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.dialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.model.DocumentMetadataEntry;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.util.NodeIterable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that shows document metadata.
 */
public class DocumentMetadataDialog extends AbstractDialog<WorkflowDescriptor> {

    private static final long serialVersionUID = 1L;
    private static final String DATE_STYLE = "MS";

    static final Logger log = LoggerFactory.getLogger(DocumentMetadataDialog.class);

    public DocumentMetadataDialog(WorkflowDescriptorModel model) {
        super(model);

        setOkVisible(false);
        setCancelLabel(new StringResourceModel("close", this, null));
        setFocusOnCancel();

        ListView metaDataListView = getMetaDataListView();
        add(metaDataListView);

        // Show info of live variant if one found with hippostd:state = published and hippostd:stateSummary = live || changed
        List<DocumentMetadataEntry> publicationMetadata = getPublicationMetaData();
        ListView publicationDataList = new ListView<DocumentMetadataEntry>("publicationmetadatalist", publicationMetadata) {
            protected void populateItem(ListItem item) {
                final DocumentMetadataEntry entry = (DocumentMetadataEntry) item.getModelObject();
                item.add(new Label("key", entry.getKey()));
                item.add(new Label("value", entry.getValue()));
            }
        };
        final Label publicationheader = new Label("publicationheader", getString("publication-header"));
        add(publicationheader);
        add(publicationDataList);

        if(publicationMetadata.size() == 0) {
            publicationDataList.setVisible(false);
            publicationheader.setVisible(false);
        }

    }

    private Node getNode() throws RepositoryException {
        return ((WorkflowDescriptorModel) super.getModel()).getNode();
    }

    private ListView getMetaDataListView() {
        List<DocumentMetadataEntry> metaDataList = new ArrayList<>();

        try {
            final Node node = getNode();

                // add translation names
            final Map<String, String> names = getNames(node);
            String namesLabel;
            if (names.size() > 1) {
                namesLabel = getString("document-names");
            } else {
                namesLabel = getString("document-name");
            }
            for (Map.Entry<String, String> entry : names.entrySet()) {
                StringBuilder name = new StringBuilder(entry.getValue());
                if (StringUtils.isNotBlank(entry.getKey())) {
                    name.append(" (");
                    name.append(entry.getKey());
                    name.append(")");
                }
                metaDataList.add(new DocumentMetadataEntry(namesLabel, name.toString()));
                namesLabel = StringUtils.EMPTY;
            }

            // add url name
            metaDataList.add(new DocumentMetadataEntry(getString("url-name"), node.getName()));

        } catch (RepositoryException e) {
            log.warn("No document node present", e);
        }

        return getListView("metadatalist", metaDataList);
    }

    private ListView getListView(final String id, final List<DocumentMetadataEntry> metaDataList) {
        return new ListView<DocumentMetadataEntry>(id, metaDataList) {

            @Override
            protected void populateItem(ListItem item) {
                final DocumentMetadataEntry entry = (DocumentMetadataEntry) item.getModelObject();
                item.add(new Label("key", entry.getKey()));
                item.add(new Label("value", entry.getValue()));
            }
        };
    }

    private List<DocumentMetadataEntry> getPublicationMetaData() {
        List<DocumentMetadataEntry> publicationMetadata = new ArrayList<>();
        try {
            final Node node = getNode();
            for (Node variant : new NodeIterable(node.getNodes(node.getName()))) {
                String state = variant.getProperty("hippostd:state").getString();
                switch(state) {
                    case "unpublished":
                        publicationMetadata.add(new DocumentMetadataEntry(getString("created-by"),
                                variant.getProperty("hippostdpubwf:createdBy").getString()));
                        publicationMetadata.add(new DocumentMetadataEntry(getString("creationdate"),
                                formattedCalendarByStyle(variant.getProperty("hippostdpubwf:creationDate").getDate(), DATE_STYLE)));
                        publicationMetadata.add(new DocumentMetadataEntry(getString("lastmodifiedby"),
                                variant.getProperty("hippostdpubwf:lastModifiedBy").getString()));
                        publicationMetadata.add(new DocumentMetadataEntry(getString("lastmodificationdate"),
                                formattedCalendarByStyle(variant.getProperty("hippostdpubwf:lastModificationDate").getDate(), DATE_STYLE)));
                        break;

                    case "published":
                        publicationMetadata.add(new DocumentMetadataEntry(getString("publicationdate"),
                                formattedCalendarByStyle(variant.getProperty("hippostdpubwf:publicationDate").getDate(), DATE_STYLE)));
                        break;
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to find publication meta data", e);
        }

        return publicationMetadata;
    }

    private Map<String, String> getNames(final Node node) throws RepositoryException {
        Map<String, String> names = new HashMap<>();
        for (Node translationNode : new NodeIterable(node.getNodes("hippo:translation"))) {
            names.put(translationNode.getProperty("hippo:language").getString(),
                    translationNode.getProperty("hippo:message").getString());
        }
        return names;
    }

    private String formattedCalendarByStyle(Calendar calendar, String patternStyle) {
        if (calendar != null) {
            DateTimeFormatter dtf = DateTimeFormat.forStyle(patternStyle).withLocale(getLocale());
            return dtf.print(new DateTime(calendar));
        }
        return StringUtils.EMPTY;
    }


    public IModel<String> getTitle() {
        return new StringResourceModel("document-info", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

}
