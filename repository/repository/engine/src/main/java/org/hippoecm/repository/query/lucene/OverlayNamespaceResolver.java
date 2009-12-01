package org.hippoecm.repository.query.lucene;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

public class OverlayNamespaceResolver implements NamespaceResolver {
    private final Map<String,String> overlayPrefixToURI = new HashMap<String,String>();

    private final Map<String,String> overlayUriToPrefix = new HashMap<String,String>();

    private NamespaceRegistry upstream;

    public OverlayNamespaceResolver(NamespaceRegistry nsMappings, Properties namespaces) {
        this.upstream = nsMappings;
        for(Enumeration prefixes = namespaces.propertyNames(); prefixes.hasMoreElements(); ) {
            String prefix = (String) prefixes.nextElement();
            addOverlayNamespace(prefix, namespaces.getProperty(prefix));
        }
    }

    /**
     * Adds the given namespace declaration to this resolver.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     */
    private void addOverlayNamespace(String prefix, String uri) {
        overlayPrefixToURI.put(prefix, uri);
        overlayUriToPrefix.put(uri, prefix);
    }

    /** {@inheritDoc} */
    public String getURI(String prefix) throws NamespaceException {
        String uri = overlayPrefixToURI.get(prefix);
        if (uri != null) {
            return uri;
        } else {
            try {
                return upstream.getURI(prefix);
            } catch (RepositoryException ex) {
                throw new NamespaceException("namespace "+prefix+" not defined", ex);
            }
        }
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri) throws NamespaceException {
        String prefix = overlayUriToPrefix.get(uri);
        if (prefix != null) {
            return prefix;
        } else {
            try {
                return upstream.getPrefix(uri);
            } catch (RepositoryException ex) {
                throw new NamespaceException("namespace "+prefix+" not defined", ex);
            }
         }
    }
}
