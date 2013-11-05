package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class DocumentTemplateUtils {

    public static final String NAMESPACE_ROOT = "/hippo:namespaces";
    private static Logger log = LoggerFactory.getLogger(DocumentTemplateUtils.class);


    private DocumentTemplateUtils() {
    }

    /**
     * Imports XML template into repository
     *
     * @param context      plugin context instance
     * @param content      XML content to import
     * @param documentName name of the document
     * @param namespace    document namespace
     * @param overwrite    overwrite existing template
     * @throws RepositoryException on repo error
     */
    public static void importTemplate(final PluginContext context, final String content, final String documentName, final String namespace, final boolean overwrite) throws RepositoryException {
        final Session session = context.getSession();
        final String docNamespaceRoot = NAMESPACE_ROOT + '/' + namespace;
        final String path = docNamespaceRoot + '/' + documentName;
        try {
            if (session.nodeExists(path)) {
                if (overwrite) {
                    session.removeItem(path);
                } else {
                    log.info("Template already exists {}", path);
                    return;
                }
            }
            session.importXML(docNamespaceRoot, new ByteArrayInputStream(content.getBytes()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            session.save();
        } catch (IOException e) {
            log.error("Error importing template", e);
        }

    }

    /**
     * Check if document template exists
     * @param context      plugin context instance
     * @param documentName name of the document
     * @param namespace    document namespace
     * @return  true if template exists, false otherwise
     * @throws RepositoryException
     */
    public static boolean templateExists(final PluginContext context, final String documentName, final String namespace) throws RepositoryException {
        final Session session = context.getSession();
        final String path = NAMESPACE_ROOT + '/' + namespace + '/' + documentName;
        log.debug("Checking if template exists: {}", path);
        return session.nodeExists(path);
    }

}
