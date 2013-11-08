package org.onehippo.cms7.essentials.dashboard.wiki;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @version "$Id$"
 */
public class NonWorkflowWikiImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(NonWorkflowWikiImporter.class);

    private WikiStrategy strategy;

    @Override
    public void importAction(final Session session, final WikiStrategy strategy, final Properties properties) {
        this.strategy = strategy;
        int amount = (int) properties.get("amount");
        if (amount != 0) {
            SAXParserFactoryImpl impl = new SAXParserFactoryImpl();
            SAXParser parser;
            int numberOfWikiDocs = amount;

            if (numberOfWikiDocs <= 0) {
                log.error("message", "number must be a number larger than 0 but was '" + amount);
            }

            String wikiContentFileSystem = properties.containsKey("filesystemLocation") ? (String) properties.get("filesystemLocation") : null;

            if (numberOfWikiDocs > 100 && StringUtils.isEmpty(wikiContentFileSystem)) {
                return;
            }

            int offset = (int) properties.get("offset");
            if (offset < 0) {
                offset = 0;
            }

            int maxDocsPerFolder = (int) properties.get("maxDocsPerFolder");
            if (maxDocsPerFolder < 0) {
                maxDocsPerFolder = 0;
            }

            int maxSubFolder = (int) properties.get("maxSubFolder");
            if (maxSubFolder < 0) {
                maxSubFolder = 0;
            }

            try {
                parser = impl.newSAXParser();
                InputStream wikiStream = null;
                File f = null;
                if (StringUtils.isEmpty(wikiContentFileSystem)) {
                    wikiStream = NonWorkflowWikiImporter.class.getClassLoader().getResourceAsStream(
                            "enwiki-20081008-pages-articles.xml.100.top.xml");
                } else {
                    f = new File(wikiContentFileSystem);
                }

                DefaultHandler handler = null;

                try {
                    String siteContentBasePath = properties.getProperty("siteContentBasePath");
                    Session writableSession = session;
                    Node baseNode = writableSession.getNode(siteContentBasePath);

                    Node wikiFolder;

                    String containerName = properties.containsKey("container") ? properties.getProperty("container") : "wikipedia";

                    if (!baseNode.hasNode(containerName)) {
                        wikiFolder = baseNode.addNode(containerName, "hippostd:folder");
                        wikiFolder.addMixin("hippo:harddocument");
                        wikiFolder.setProperty("hippo:paths", new String[]{});
                        wikiFolder.addMixin("hippotranslation:translated");
                        wikiFolder.setProperty("hippotranslation:locale", "en");
                        wikiFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                    } else {
                        wikiFolder = baseNode.getNode(containerName);
                    }

                    String prefix = properties.containsKey("prefix") ? properties.getProperty("prefix") : "wiki-";

                    handler = new WikiPediaToJCRHandler(wikiFolder, numberOfWikiDocs, offset, maxDocsPerFolder,
                            maxSubFolder, prefix, strategy);

                    if (wikiStream == null) {
                        parser.parse(f, handler);
                    } else {
                        parser.parse(wikiStream, handler);
                    }
                } catch (ForcedStopException e) {
                    log.info("successful handler quits after numberOfWikiDocs has been achieved");
                } catch (Exception e) {
                    log.error("Exception during importing wikipedia docs, it did notimport wiki docs.", e);
                    return;
                }
            } catch (ParserConfigurationException e) {
                log.error("Exception during importing wikipedia docs, it did notimport wiki docs.", e);
                return;
            }
        }

    }

}
