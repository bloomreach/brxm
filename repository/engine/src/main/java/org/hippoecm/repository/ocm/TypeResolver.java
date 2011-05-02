package org.hippoecm.repository.ocm;

import javax.jcr.RepositoryException;

public interface TypeResolver {
    @SuppressWarnings("unused")
    final String SVN_ID = "$Id: ";
    
    public String[] resolve(String className) throws RepositoryException;
}
