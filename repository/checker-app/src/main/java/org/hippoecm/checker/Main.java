package org.hippoecm.checker;

import javax.jcr.RepositoryException;
import org.hippoecm.repository.LocalHippoRepository;

public class Main {
    public static void main(String[] args) {
        try {
            LocalHippoRepository.check(null, true);
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
