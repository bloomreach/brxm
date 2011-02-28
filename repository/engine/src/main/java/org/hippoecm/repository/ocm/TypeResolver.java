package org.hippoecm.repository.ocm;

import javax.jcr.RepositoryException;

public interface TypeResolver {
    public String[] resolve(String className) throws RepositoryException;
}
