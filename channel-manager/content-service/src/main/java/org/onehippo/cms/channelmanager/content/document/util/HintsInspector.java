package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.error.ErrorInfo;

public interface HintsInspector {

    /**
     * Check if a document draft can be created, given its workflow hints.
     *
     * @param hints workflow hints
     * @return true if a draft can be created for the document.
     */
    boolean canCreateDraft(Map<String, Serializable> hints);

    /**
     * Check if a document can be updated, given its workflow hints.
     *
     * @param hints workflow hints
     * @return true if document can be updated, false otherwise
     */
    boolean canUpdateDraft(Map<String, Serializable> hints);

    /**
     * Check if a document can be updated, given its workflow hints.
     *
     * @param hints workflow hints
     * @return true if document can be updated, false otherwise
     */
    boolean canDeleteDraft(Map<String, Serializable> hints);

    /**
     * Determine the reason why editing failed for the present workflow hints.
     *
     * @param hints   workflow hints
     * @param session current user's JCR session
     * @return Specific reason or nothing (unknown), wrapped in an Optional
     */
    Optional<ErrorInfo> determineEditingFailure(Map<String, Serializable> hints, Session session);

}
