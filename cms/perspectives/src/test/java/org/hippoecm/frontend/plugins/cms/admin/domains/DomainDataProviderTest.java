package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.junit.Test;

import junit.framework.Assert;

/**
 */
public class DomainDataProviderTest {

    @Test
    public void testIterator() throws Exception {
        final List<Domain> testDomains = new ArrayList<Domain>(10);
        for (int i = 0; i < 10; i++) {
            String name = "domain #" + i;
            testDomains.add(new MockDomain(name));
        }

        DomainDataProvider mockProvider = new DomainDataProvider() {
            @Override
            public List<Domain> getDomainList() {
                return testDomains;
            }
        };

        final int pageSize = 6;
        Iterator firstPage = mockProvider.iterator(0, pageSize);
        Iterator secondPage = mockProvider.iterator(pageSize, pageSize);

        int firstPageSize = 0;
        while (firstPage.hasNext()) {
            firstPage.next();
            firstPageSize++;
        }

        int secondPageSize = 0;
        while (secondPage.hasNext()) {
            secondPage.next();
            secondPageSize++;
        }

        Assert.assertTrue(firstPageSize == 6);
        Assert.assertTrue(secondPageSize == 4);
    }

    private class MockDomain extends Domain {

        final private String name;

        private MockDomain(String name) throws RepositoryException {
            // Using a Mock Node here, because Domains cannot be created via the CMS and thus there is no empty
            // constructor
            super(new MockNode());
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

}
