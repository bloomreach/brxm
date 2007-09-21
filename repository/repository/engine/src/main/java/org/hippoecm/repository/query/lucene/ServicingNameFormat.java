package org.hippoecm.repository.query.lucene;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.ParsingNameResolver;
import org.apache.jackrabbit.name.QName;

public class ServicingNameFormat {
    private ServicingNameFormat(){
        // private constructor: only static methods
    }

    public static String getInternalFacetName(QName nodeName, NamespaceMappings nsMappings) throws NoPrefixDeclaredException {
        String internalName = NameFormat.format(nodeName,nsMappings);
        int idx = internalName.indexOf(':');
        internalName = internalName.substring(0, idx + 1)
                + ServicingFieldNames.HIPPO_FACET + internalName.substring(idx + 1);
        return internalName;
    }
    
    public static String getInternalFacetName(String facet, NamespaceMappings nsMappings) throws NameException, NamespaceException {
        ParsingNameResolver pnr = new ParsingNameResolver(nsMappings);
        QName nodeName = pnr.getQName(facet);
        String internalName = NameFormat.format(nodeName,nsMappings);
        int idx = internalName.indexOf(':');
        internalName = internalName.substring(0, idx + 1)
                + ServicingFieldNames.HIPPO_FACET + internalName.substring(idx + 1);
        return internalName;
    }
    
    
}
