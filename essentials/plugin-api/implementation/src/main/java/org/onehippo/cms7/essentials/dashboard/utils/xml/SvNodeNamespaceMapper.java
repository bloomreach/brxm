package org.onehippo.cms7.essentials.dashboard.utils.xml;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * @version "$Id: SvNodeNamespaceMapper.java 172185 2013-07-30 14:09:36Z mmilicevic $"
 */
public class SvNodeNamespaceMapper extends NamespacePrefixMapper {




    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (EssentialConst.URI_JCR_NAMESPACE.equals(namespaceUri)) {
            return "sv";
        }

        return suggestion;
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[]{EssentialConst.URI_JCR_NAMESPACE};
    }


}
