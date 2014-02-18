package org.onehippo.cms7.essentials.dashboard.model;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Removes namespace prefix from plugin nodes
 *
 * @version "$Id: PluginNamespaceMapper.java 172296 2013-07-31 09:32:49Z mmilicevic $"
 */
public class PluginNamespaceMapper extends NamespacePrefixMapper {

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (EssentialConst.URI_ESSENTIALS_PLUGIN.equals(namespaceUri)) {
            return "";
        }

        return suggestion;
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[]{EssentialConst.URI_ESSENTIALS_PLUGIN};
    }

}
