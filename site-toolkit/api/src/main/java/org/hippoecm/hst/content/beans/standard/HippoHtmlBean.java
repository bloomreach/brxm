/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.hst.content.beans.standard;


import org.hippoecm.hst.content.annotations.PageModelIgnore;

/**
 * Implementing classes represent a html node in the ecm repository. 
 */
public interface HippoHtmlBean extends HippoBean {

    /**
     * in the Page Model API we do not want to show uuid for nodes below a document (compound)
     */
    @PageModelIgnore
    @Override
    default String getRepresentationId() {
        return getIdentifier();
    }

    @PageModelIgnore
    @Override
    String getName();

    @PageModelIgnore
    @Override
    String getDisplayName();

    /**
     * <p>
     *     Since in the Page Model API (PMA) we do not want to serialize the raw String content (but instead a rewritten
     *     content resolving internal links), we ignore the {@link #getContent()} in PMA via {@link PageModelIgnore @PageModelIgnore}
     * </p>
     * @return the string value of the content for the html bean
     */
    @PageModelIgnore
    String getContent();
    
}
