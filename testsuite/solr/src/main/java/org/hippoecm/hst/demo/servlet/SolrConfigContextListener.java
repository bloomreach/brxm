package org.hippoecm.hst.demo.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SolrConfigContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(SolrConfigContextListener.class);

    private static final String SOLR_SOLR_HOME = "solr.solr.home";

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        String existingSolrHome = StringUtils.trimToNull(System.getProperty(SOLR_SOLR_HOME));
        if (existingSolrHome != null) {
            log.info("{} already exists: {}", SOLR_SOLR_HOME, existingSolrHome);
            return;
        }

        String solrHome = StringUtils.trimToNull(sce.getServletContext().getInitParameter(SOLR_SOLR_HOME));
        if (solrHome == null) {
            return;
        }

        String realSolrHomePath = solrHome;

        if (solrHome.startsWith("/")) {
            realSolrHomePath = sce.getServletContext().getRealPath(solrHome);
        } else if (solrHome.startsWith("file:")) {
            try {
                realSolrHomePath = new File(URI.create(solrHome)).getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("Invalid parameter for " + SOLR_SOLR_HOME + ": " + solrHome + ". " + e);
            }
        }
        System.setProperty(SOLR_SOLR_HOME, realSolrHomePath);
        log.info("{}={}", SOLR_SOLR_HOME, System.getProperty(SOLR_SOLR_HOME));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
