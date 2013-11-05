package org.onehippo.cms7.essentials.dashboard.wiki;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
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
public class NonWorkflowWikiImporter {

    private static Logger log = LoggerFactory.getLogger(NonWorkflowWikiImporter.class);

    // Matches headers in the wikipedia format having two or three equals-signs
    private static final String blockSeparator = "===?([^=]*?)===?";
    // private static final Pattern blockSeparatorPattern = Pattern.compile(blockSeparator);
    private String type ;

    public void importAction(final Session session, int amount, int offset, int maxDocsPerFolder, int maxSubFolder, int numberOfTranslations, String filesystemLocation, String siteContentBasePath, boolean addImages, final String type) {
        // String numberStr = "10" ;
       // long start = System.currentTimeMillis();
        this.type = type;
        if (amount != 0) {
            SAXParserFactoryImpl impl = new SAXParserFactoryImpl();
            SAXParser parser;
            int numberOfWikiDocs = 0;
            try {
                numberOfWikiDocs = amount;
            } catch (NumberFormatException e) {
                //response.setRenderParameter("message", "number must be a number but was '" + numberStr + "'");
            }

            if (numberOfWikiDocs <= 0) {
                //response.setRenderParameter("message", "number must be a number larger than 0 but was '" + numberStr
                //        + "'");
            }

            String wikiContentFileSystem = filesystemLocation;

            if (numberOfWikiDocs > 100 && StringUtils.isEmpty(wikiContentFileSystem)) {
                return;
            }

            if (offset < 0) {
                offset = 0;
            }

            if (maxDocsPerFolder < 0) {
                maxDocsPerFolder = 0;
            }

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
                    Session writableSession = session;
                    Node baseNode = writableSession.getNode(siteContentBasePath);

                    Node wikiFolder;

                    if (!baseNode.hasNode("wikipedia")) {
                        wikiFolder = baseNode.addNode("wikipedia", "hippostd:folder");
                        wikiFolder.addMixin("hippo:harddocument");
                        wikiFolder.setProperty("hippo:paths", new String[]{});
                        wikiFolder.addMixin("hippotranslation:translated");
                        wikiFolder.setProperty("hippotranslation:locale", "en");
                        wikiFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                    } else {
                        wikiFolder = baseNode.getNode("wikipedia");
                    }

                    handler = new WikiPediaToJCRHandler(wikiFolder, numberOfWikiDocs, offset, maxDocsPerFolder,
                            maxSubFolder, addImages, type);

                    //handler = getHandler();

                    if (wikiStream == null) {
                        parser.parse(f, handler);
                    } else {
                        parser.parse(wikiStream, handler);
                    }
                } catch (ForcedStopException e) {
                    // successful handler quits after numberOfWikiDocs has been achieved
                } catch (Exception e) {
                    log.warn("Exception during importing wikipedia docs", e);
                    //response.setRenderParameter("message",
                    //          "An exception happened. Did not import wiki docs. " + e.toString());
                    return;
                }
            } catch (ParserConfigurationException e) {
                //response.setRenderParameter("message", "Did not import wiki: " + e.toString());
                return;
            }
        }

        //relateDocuments(session,siteContentBasePath, getRelateNodesOperation(), numberOfRelations, "uuid");

        ///relateDocuments(session, siteContentBasePath, getLinkNodesOperation(), numberOfLinks, "versionHistory");

        relateDocuments(session, siteContentBasePath, getTranslateOperation(), numberOfTranslations);
    }


    /**
     * Relates the nodes to the previous nodes (in order of UUID)
     */
    private void relateDocuments(Session session, String siteContentBasePath, Operation op, final int relations) {
        if (relations < 1) {
            return;
        }

        try {
            Session writableSession = session;
            Node wikipedia = writableSession.getNode(siteContentBasePath + "/wikipedia");
            @SuppressWarnings("deprecation")
            Query q = writableSession
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(
                            String.format("//element(*,mytestproject:newsdocument)[@hippo:paths='" + wikipedia.getIdentifier() + "'] order by @jcr:uuid", type),
                            Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator it = result.getNodes();

            // Fill first queue with elements, which can't be fully linked yet
            Node current;
            LinkedList<Node> firsts = new LinkedList();
            LinkedList<Node> previous = new LinkedList();
            while (it.hasNext() && firsts.size() != relations) {
                current = it.nextNode();
                firsts.add(current);
                previous.add(current);
            }

            // Link to previous documents, update previous documents queue, occasionally save
            int count = 1;
            while (it.hasNext()) {
                current = it.nextNode();
                Iterator<Node> qit = previous.listIterator();

                while (qit.hasNext()) {
                    op.perform(current, qit.next());
                }

                previous.remove();
                previous.add(current);

                if (count++ % 200 == 0) {
                    writableSession.save();
                }
            }

            // Finally, link the first queue with elements
            Iterator<Node> fit = firsts.listIterator();
            while (fit.hasNext()) {
                current = fit.next();
                Iterator<Node> qit = previous.listIterator();

                while (qit.hasNext()) {
                    op.perform(current, qit.next());
                }

                previous.remove();
                previous.add(current);
            }

            writableSession.save();
        } catch (RepositoryException e) {
            log.warn("Exception during relating wiki docs", e);
        }
    }

    private Operation getTranslateOperation() {
        return new Operation() {
            private final List<String> locales = Arrays.asList("de", "it", "fr", "nl");
            private int localeIndex = 0;
            private Node lastNode;

            @Override
            public void perform(Node from, Node to) {
                if (from == lastNode) {
                    localeIndex++;
                } else {
                    localeIndex = 0;
                }
                lastNode = from;

                if (localeIndex >= locales.size()) {
                    return;
                }

                try {
                    Node handle = from.getParent();
                    Node subFolder = handle.getParent();
                    Node folder = subFolder.getParent();

                    Node tHandle;
                    Node tFolder;
                    Node tSubFolder;
                    Node tDoc;

                    // Get the wikipedia folder for the translated site
                    Node tContentRoot = folder.getParent().getParent().getParent().getNode("demosite_" + locales.get(localeIndex));
                    Node tWikipedia = tContentRoot.getNode("wikipedia");


                    String tFolderName = folder.getName() + "-" + locales.get(localeIndex);
                    tFolder = getTranslatedFolder(tWikipedia, tFolderName, locales.get(localeIndex), folder);

                    String tSubFolderName = subFolder.getName() + "-" + locales.get(localeIndex);
                    tSubFolder = getTranslatedFolder(tFolder, tSubFolderName, locales.get(localeIndex), subFolder);

                    // Create handle for translated document
                    tHandle = tSubFolder.addNode(handle.getName(), "hippo:handle");
                    tHandle.addMixin("hippo:hardhandle");
                    tHandle.addMixin("hippo:translated");

                    // Create translated document
                    tDoc = tHandle.addNode(handle.getName(), "mytestproject:newsdocument");
                    tDoc.addMixin("hippo:harddocument");
                    tDoc.setProperty("hippo:paths", new String[]{});
                    tDoc.addMixin("hippotranslation:translated");

                    String[] availability = {"live", "preview"};
                    tDoc.setProperty("hippo:availability", availability);
                    tDoc.setProperty("hippostd:stateSummary", "live");
                    tDoc.setProperty("hippostd:state", "published");
                    tDoc.setProperty("hippostdpubwf:lastModifiedBy", from.getProperty("hippostdpubwf:lastModifiedBy")
                            .getString());
                    tDoc.setProperty("hippostdpubwf:createdBy", from.getProperty("hippostdpubwf:createdBy").getString());
                    tDoc.setProperty("hippostdpubwf:lastModificationDate", Calendar.getInstance());
                    tDoc.setProperty("hippostdpubwf:creationDate", Calendar.getInstance());
                    tDoc.setProperty("hippostdpubwf:publicationDate", Calendar.getInstance());
                    tDoc.setProperty("hippotranslation:locale", locales.get(localeIndex));
                    tDoc.setProperty("hippotranslation:id", from.getProperty("hippotranslation:id").getString());
                } catch (RepositoryException e) {
                    log.warn("Couldn't translate document", e);
                }
            }
        };
    }

    private static Node getTranslatedFolder(Node parent, String name, String locale, Node folderToTranslate) throws RepositoryException {
        Node translatedFolder;
        if (parent.hasNode(name)) {
            translatedFolder = parent.getNode(name);
        } else {
            translatedFolder = parent.addNode(name, "hippostd:folder");
            translatedFolder.addMixin("hippo:harddocument");
            translatedFolder.setProperty("hippo:paths", new String[]{});
            translatedFolder.addMixin("hippotranslation:translated");
            translatedFolder.setProperty("hippotranslation:locale", locale);
            translatedFolder.setProperty("hippotranslation:id", folderToTranslate.getProperty("hippotranslation:id")
                    .getString());
        }
        return translatedFolder;
    }

    interface Operation {
        public void perform(Node from, Node to);
    }

}
