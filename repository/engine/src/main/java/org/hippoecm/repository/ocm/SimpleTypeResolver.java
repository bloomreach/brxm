package org.hippoecm.repository.ocm;

import javax.jcr.RepositoryException;

public class SimpleTypeResolver implements TypeResolver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";
    
    @Override
    public String[] resolve(String className) throws RepositoryException {
        return new String[0];
    }
}
