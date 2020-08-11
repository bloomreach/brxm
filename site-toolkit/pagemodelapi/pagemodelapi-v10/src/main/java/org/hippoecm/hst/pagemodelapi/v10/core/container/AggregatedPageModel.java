/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10.core.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ChannelModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ComponentWindowModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.IdentifiableLinkableMetadataBaseModel;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Aggregated page model which represents the whole output in the page model pipeline request processing.
 */
@JsonPropertyOrder({ "meta", "links", "channel", "root", "document", "page"})
@ApiModel(description = "Aggregated page model from Page Model JSON API requests.")
class AggregatedPageModel extends IdentifiableLinkableMetadataBaseModel {

    private ChannelModel channelModel;
    private ComponentWindowModel pageWindowModel;
    private Map<String, ComponentWindowModel> flattened = new HashMap<>();
    private HippoDocumentBean primaryRequestDocument;

    @JsonIgnore
    @Override
    public String getId() {
        return super.getId();
    }

    public AggregatedPageModel(final String id) {
        super(id);
    }

    @JsonProperty("channel")
    @JsonInclude(Include.NON_NULL)
    @ApiModelProperty(
            value = "Channel property in JSON object. For details, look up the online documentation about Page Model JSON API.",
            dataType = "object"
            )
    public ChannelModel getChannelModel() {
        return channelModel;
    }

    public void setChannelModel(ChannelModel channelModel) {
        this.channelModel = channelModel;
    }

    public void setPageWindowModel(final ComponentWindowModel pageWindowModel) {
        this.pageWindowModel = pageWindowModel;
        populateFlattened(pageWindowModel);
    }

    /**
     * Sets the primaryRequestDocument if requestBean is not null and of type {@link HippoDocumentBean}
     */
    public void setDocument(final HippoBean requestBean) {
        if (requestBean instanceof HippoDocumentBean) {
            this.primaryRequestDocument = (HippoDocumentBean)requestBean;
        }
    }

    private void populateFlattened(final ComponentWindowModel pageWindowModel) {
        flattened.put(pageWindowModel.getId(), pageWindowModel);

        final Set<ComponentWindowModel> components = pageWindowModel.getChildren();

        if (components != null) {
            components.forEach(this::populateFlattened);
        }
    }

    public Optional<ComponentWindowModel> getModel(final String referenceNamespace) {
        return Optional.ofNullable(flattened.get(referenceNamespace));
    }

    public ComponentWindowModel getPage() {
        return pageWindowModel;
    }

    public RootReference getRoot() {
        return new RootReference(pageWindowModel);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public RootReference getDocument() {
        if (primaryRequestDocument == null) {
            return null;
        }
        return new RootReference(primaryRequestDocument);
    }

    public static class RootReference {
        private Object object;

        public RootReference(final Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }
    }

}
