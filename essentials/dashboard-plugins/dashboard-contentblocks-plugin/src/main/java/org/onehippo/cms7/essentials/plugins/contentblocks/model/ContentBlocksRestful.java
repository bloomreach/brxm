package org.onehippo.cms7.essentials.plugins.contentblocks.model;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

import java.util.List;

public class ContentBlocksRestful implements Restful {
    private List<DocumentTypeRestful> documentTypes;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(DocumentTypeRestful.class)
    })
    public List<DocumentTypeRestful> getDocumentTypes() {
        return documentTypes;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(DocumentTypeRestful.class)
    })
    public void setDocumentTypes(final List<DocumentTypeRestful> documentTypes) {
        this.documentTypes = documentTypes;
    }
}
