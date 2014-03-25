package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XmlUtils is used for manipulating XML files on a file system
 *
 * @version "$Id$"
 */
public final class XmlUtils {

    private static Logger log = LoggerFactory.getLogger(XmlUtils.class);

    private XmlUtils() {
    }

    public static List<XmlNode> findTemplateDocuments(final Path path, final PluginContext context) {
        // filter all XML documents
        final Collection<File> files = FileUtils.listFiles(path.toFile(), EssentialConst.XML_FILTER, true);
        final List<XmlNode> templateDocuments = new ArrayList<>();
        for (File file : files) {
            final XmlNode xmlNode = parseXml(file.toPath());
            if (xmlNode != null) {
                final XmlProperty property = xmlNode.getXmlPropertyByName(EssentialConst.NS_JCR_PRIMARY_TYPE);
                if (property != null && property.getSingleValue() != null && property.getSingleValue().equals(EssentialConst.NS_HIPPOSYSEDIT_TEMPLATETYPE)) {
                    templateDocuments.add(xmlNode);
                }
            }
        }

        return templateDocuments;
    }


    public static String xmlNodeToString(final XmlNode value) {
        try {
            final JAXBContext context = JAXBContext.newInstance(XmlNode.class);
            final Marshaller m = context.createMarshaller();
            //m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SvNodeNamespaceMapper());
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final StringWriter writer = new StringWriter();
            m.marshal(value, writer);
            return writer.toString();
        } catch (JAXBException e) {
            log.error("Error converting XML to string", e);
        }
        return null;

    }


    public static XmlNode parseXml(final InputStream content) {

        try {
            final JAXBContext context = JAXBContext.newInstance(XmlNode.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (XmlNode) unmarshaller.unmarshal(content);
        } catch (JAXBException e) {
            if (log.isDebugEnabled()) {
                log.error("Error parsing XmlNode document", e.getMessage());
            }
        }

        return null;
    }


    public static XmlNode parseXml(final Path path) {

        try {
            final JAXBContext context = JAXBContext.newInstance(XmlNode.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (XmlNode) unmarshaller.unmarshal(path.toFile());
        } catch (JAXBException e) {
            if (log.isDebugEnabled()) {
                log.error("Error parsing XmlNode document: " + path, e.getMessage());
            }
        }

        return null;
    }

}
