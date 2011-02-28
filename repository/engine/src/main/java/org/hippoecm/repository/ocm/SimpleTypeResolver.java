package org.hippoecm.repository.ocm;

import javax.jcr.RepositoryException;

public class SimpleTypeResolver implements TypeResolver {
    @Override
    public String[] resolve(String className) throws RepositoryException {
        return new String[0];
    }
}
