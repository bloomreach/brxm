package org.hippoecm.repository.ocm.fieldmanager;

import javax.jcr.RepositoryException;

public interface TypeResolver {
    public String[] resolve(String className) throws RepositoryException;
}
