/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.contentbean;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.content.beans.Interceptor;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanInterceptor;
import org.hippoecm.hst.content.beans.dynamic.InterceptorEntity;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.bind.annotation.Super;

@Interceptor(cmsTypes = "TaxonomyClassification")
public class TaxonomyClassificationInterceptor extends DynamicBeanInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TaxonomyClassificationInterceptor.class);

    private static final String EDITOR_TEMPLATES_NODE_NAME = "editor:templates";
    private static final String PLUGIN_CLUSTER_NODE_NAME = "_default_";
    private static final String FIELD_PROPERTY_NAME = "field";
    private static final String CLUSTER_OPTIONS_PROPERTY_NAME = "cluster.options";
    private static final String TAXONOMY_NAME_CLUSTER_PROPERTY = "taxonomy.name";

    private final TaxonomyManager taxonomyManager;
    private final String taxonomyName;

    public TaxonomyClassificationInterceptor(final String propertyName, final Boolean multiple, final Node documentTypeNode) {
        super(propertyName, multiple, documentTypeNode);
        this.taxonomyManager = HstServices.getComponentManager().getComponent("TaxonomyManager", "org.onehippo.taxonomy.contentbean");
        this.taxonomyName = resolveTaxonomyName(documentTypeNode);
    }

    @Override
    public Class<? extends InterceptorEntity> getCmsType() {
        return TaxonomyClassification.class;
    }

    public TaxonomyClassification createTaxonomyClassification(@Super(proxyType = TargetType.class) Object superObject) {
        final HippoBean hippoBean = (HippoBean) superObject;

        //taxonomy manager provides caching and invalidation when taxonomy document changes
        try {
            Node documentNode = hippoBean.getNode();
            if(!documentNode.hasProperty(getPropertyName())) {
                return null;
            }
            
            final Property documentProperty = documentNode.getProperty(getPropertyName());
            final Taxonomy taxonomy = taxonomyManager.getTaxonomies().getTaxonomy(taxonomyName);
            return new TaxonomyClassification(documentProperty, taxonomy);
        } catch (Exception e) {
            log.error("Could not create TaxonomyField object for serialization", e);
            return null;
        }
    }

    
    private String resolveTaxonomyName(final Node documentTypeNode) {
        try {
            String taxonomyFieldName = getFieldName(documentTypeNode);
            Property taxonomyProperty = getClusterOptions(taxonomyFieldName, documentTypeNode).get(TAXONOMY_NAME_CLUSTER_PROPERTY);
            if (taxonomyProperty != null && StringUtils.isNotEmpty(taxonomyProperty.getString())) {
                return taxonomyProperty.getString();
            }
        } catch (RepositoryException e) {
            log.error("Error while retrieving taxonomy document", e);
        }
        //TODO Looking for trouble
        return null;
    }

    //TODO Would be nice if ContentTypeService could be used here
    private String getFieldName(final Node documentTypeNode) throws RepositoryException {
        if (documentTypeNode.hasNode(HippoNodeType.NT_NODETYPE + "/" + HippoNodeType.NT_NODETYPE)) {
            final Node handle = documentTypeNode.getNode(HippoNodeType.NT_NODETYPE);
            for (Node typeNode : new NodeIterable(handle.getNodes(HippoNodeType.NT_NODETYPE))) {
                if (typeNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    for (Node field : new NodeIterable(typeNode.getNodes())) {
                        if (field.hasProperty(HippoNodeType.HIPPO_PATH) &&
                                getPropertyName().equals(field.getProperty(HippoNodeType.HIPPO_PATH).getString())) {
                            return field.getName();
                        }
                    }
                }
            }
        }
        throw new RepositoryException("Document type node " + documentTypeNode.getPath() + " cannot be processed.");
    }


    //TODO Would be nice if ContentTypeService could be used here
    private Map<String, Property> getClusterOptions(final String fieldName, final Node documentTypeNode) throws RepositoryException {
        if (documentTypeNode.hasNode(EDITOR_TEMPLATES_NODE_NAME + "/" + PLUGIN_CLUSTER_NODE_NAME)) {
            for (Node template : new NodeIterable(documentTypeNode.getNode(EDITOR_TEMPLATES_NODE_NAME + "/" + PLUGIN_CLUSTER_NODE_NAME).getNodes())) {
                if (template.hasProperty(FIELD_PROPERTY_NAME) &&
                        fieldName.equals(template.getProperty(FIELD_PROPERTY_NAME).getString()) &&
                        template.hasNode(CLUSTER_OPTIONS_PROPERTY_NAME)) {

                    Map<String, Property> clusterOptions = new HashMap<>();
                    for (Property property : new PropertyIterable(template.getNode(CLUSTER_OPTIONS_PROPERTY_NAME).getProperties())) {
                        clusterOptions.put(property.getName(), property);
                    }
                    return clusterOptions;
                }
            }
        }
        throw new RepositoryException("Document type node " + documentTypeNode.getPath() + " cannot be processed.");
    }
}