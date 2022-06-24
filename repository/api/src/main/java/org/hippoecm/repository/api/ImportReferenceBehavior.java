/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.repository.api;

/**
 * <b>This call is not (yet) part of the API, but under evaluation.</b><p/>
 * The possible actions specified by the <code>referenceBehavior</code>
 * parameter in {@link HippoSession#importEnhancedSystemViewXML}.
 * When a reference to another node by path or UUID occurs in the import, but the references node itself in not present in either the import data or in the repository
 * itself, then the value specified by the mergeBehavior governs what should the behavior during import.
 * The referenceBehavior parameter must be just one of these values.
 */
public interface ImportReferenceBehavior {

    /**
     * When a reference to occurs which cannot be resolved, remove the property containing the reference. In case the the lack of the property is not valid for the node definition in which the property occurred, a {@link javax.jcr.RepositoryException} will be thrown by either the import method or when the data in the session is being saved.
     */
    public static final int IMPORT_REFERENCE_NOT_FOUND_REMOVE = 0;

    /**
     * When a missing reference occurs, re-target the reference to reference the root of the JCR tree (<code>/jcr:root</code>).
     */
    public static final int IMPORT_REFERENCE_NOT_FOUND_TO_ROOT = 1;

    /**
     * A missing reference will cause the import to abort by throwing a @{link javax.jcr.RepositoryException} at some time during the import and leave a partial state in the transient state.
     */
    public static final int IMPORT_REFERENCE_NOT_FOUND_THROW = 2;
}
