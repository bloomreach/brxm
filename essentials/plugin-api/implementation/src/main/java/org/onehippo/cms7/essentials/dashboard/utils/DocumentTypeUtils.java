package org.onehippo.cms7.essentials.dashboard.utils;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class DocumentTypeUtils {

    private static Logger log = LoggerFactory.getLogger(DocumentTypeUtils.class);


    /**
     * Register document type for given base type and namespace
     * @param documentName  name of the document e.g. {@code newsdocument}
     * @param baseType base type of document we are registering, e.g. {@code basedocument}
     * @param namespace namespace of document we are registering e.g. {@code myproject}
     * @param context plugin context
     */
    public static void registerDocumentType(final String documentName, final String baseType, final String namespace, final PluginContext context){


    }

}
