/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.editor.workflow.model.DocumentMetadataEntry;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that shows document metadata.
 */
public class DocumentMetadataDialog extends Dialog<WorkflowDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(DocumentMetadataDialog.class);

    private static final FormatStyle DATE_STYLE = FormatStyle.LONG;
    private final Node node;

    public DocumentMetadataDialog(final WorkflowDescriptorModel model, final Node node) {
        super(model);
        this.node = node;

        setTitleKey("document-info");
        setSize(DialogConstants.LARGE_AUTO);

        setOkVisible(false);
        setCancelLabel(new StringResourceModel("close", this));
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

        if (publicationMetadata.size() == 0) {
            publicationDataList.setVisible(false);
            publicationheader.setVisible(false);
        }
    }



    private ListView getMetaDataListView() {
        List<DocumentMetadataEntry> metaDataList = new ArrayList<>();

        try {
            final Optional<String> displayName = DocumentUtils.getDisplayName(node);
            if (displayName.isPresent() && !displayName.get().isEmpty()) {
                metaDataList.add(new DocumentMetadataEntry(getString("document-name"), displayName.get()));
            }
            metaDataList.add(new DocumentMetadataEntry(getString("url-name"), node.getName()));
            metaDataList.add(new DocumentMetadataEntry(getString("document-path"), node.getPath()));

        } catch (RepositoryException e) {
            log.warn("No document node present", e);
        }

        return getListView(metaDataList);
    }

    private ListView getListView(final List<DocumentMetadataEntry> metaDataList) {
        return new ListView<DocumentMetadataEntry>("metadatalist", metaDataList) {

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
            for (Node variant : new NodeIterable(node.getNodes(node.getName()))) {
                String state = variant.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                switch (state) {
                    case HippoStdNodeType.UNPUBLISHED:
                        final String createdBy =
                                variant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY).getString();
                        publicationMetadata.add(new DocumentMetadataEntry(getString("created-by"), createdBy));

                        final String creationDate =
                                printDateProperty(variant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE));
                        publicationMetadata.add(new DocumentMetadataEntry(getString("creationdate"), creationDate));

                        final String lastModifiedBy =
                                variant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString();
                        publicationMetadata.add(new DocumentMetadataEntry(getString("lastmodifiedby"), lastModifiedBy));

                        final String lastModifiedDate =
                                printDateProperty(variant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE));
                        publicationMetadata.add(new DocumentMetadataEntry(getString("lastmodificationdate"), lastModifiedDate));
                        break;

                    case HippoStdNodeType.PUBLISHED:
                        if (isLive(variant)) {
                            final String publicationDate =
                                    printDateProperty(variant.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE));
                            publicationMetadata.add(new DocumentMetadataEntry(getString("publicationdate"), publicationDate));
                        }
                        break;
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to find publication meta data", e);
        }

        return publicationMetadata;
    }

    private String printDateProperty(final Property property) throws RepositoryException {
        final Calendar date = property.getDate();
        return DateTimePrinter.of(date).appendDST().print(DATE_STYLE);
    }

    private boolean isLive(Node variant) throws RepositoryException {
        if (variant.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)) {
            Property property = variant.getProperty(HippoNodeType.HIPPO_AVAILABILITY);
            for (Value value : property.getValues()) {
                if ("live".equals(value.getString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
