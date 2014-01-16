package org.onehippo.cms7.essentials.dashboard.wiki;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @version "$Id$"
 */
public class WikiPediaToJCRHandler extends DefaultHandler {

    private static Logger log = LoggerFactory.getLogger(WikiPediaToJCRHandler.class);


    // five year of seconds : 157680000
    private static final int NUMBER_OF_SECONDS_IN_TWO_YEARS = 63072000;
    private final Node wikiFolder;
    private Node doc;
    private Node finishedDoc;
    private Node currentFolder;
    private Node currentSubFolder;
    private int numberOfSubFolders = 1;
    private final int total;
    private final int offset;
    private final int maxDocsPerFolder;
    private final int maxSubFolders;

    private final String prefix;

    private WikiStrategy strategy;
    private StringBuilder fieldText;
    private boolean recording;
    int count = 0;
    int offsetcount = 0;
    long startTime = 0;

    private static final String[] users = {"ard", "bard", "arje", "artur", "reijn", "berry", "frank", "mathijs",
            "junaid", "ate", "tjeerd", "verberg", "simon", "jannis"};

    private final Random rand;

    public WikiPediaToJCRHandler(Node wikiFolder, int total, final int offset, final int maxDocsPerFolder,
                                 final int maxSubFolders, final String prefix, final WikiStrategy strategy) throws Exception {
        this.prefix = prefix;
        this.strategy = strategy;
        this.wikiFolder = wikiFolder;
        this.total = total;
        this.offset = offset;
        this.maxDocsPerFolder = maxDocsPerFolder;
        this.maxSubFolders = maxSubFolders;
        currentFolder = wikiFolder.addNode(prefix + System.currentTimeMillis(), "hippostd:folder");
        currentFolder.addMixin("hippo:harddocument");
        currentFolder.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
        currentFolder.addMixin("hippotranslation:translated");
        currentFolder.setProperty("hippotranslation:locale", "en");
        currentFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
        currentSubFolder = currentFolder.addNode(prefix + System.currentTimeMillis(), "hippostd:folder");
        currentSubFolder.addMixin("hippo:harddocument");
        currentSubFolder.addMixin("hippotranslation:translated");
        currentSubFolder.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
        currentSubFolder.setProperty("hippotranslation:locale", "en");
        currentSubFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
        rand = new Random(System.currentTimeMillis());
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        if (qName.equals("title")) {
            if (offsetcount < offset) {
                offsetcount++;
                if ((offsetcount % maxDocsPerFolder) == 0) {
                    log.info("Offset '" + offset + "' not yet reached. Currently at '" + offsetcount
                            + '\'');
                }
            }
            if (offsetcount == offset) {
                try {
                    if (count >= total) {
                        log.info("total: " + total);
                        wikiFolder.getSession().save();

                        log.info("Total added wiki docs = " + count + ". It took "
                                + (System.currentTimeMillis() - startTime) + " ms.");
                        throw new ForcedStopException();
                    }
                    if ((count % maxDocsPerFolder) == 0 && count != 0) {
                        wikiFolder.getSession().save();
                        if (numberOfSubFolders >= maxSubFolders) {
                            currentFolder = wikiFolder.addNode(prefix + System.currentTimeMillis(),
                                    "hippostd:folder");
                            currentFolder.addMixin("hippo:harddocument");
                            currentFolder.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
                            currentFolder.addMixin("hippotranslation:translated");
                            currentFolder.setProperty("hippotranslation:locale", "en");
                            currentFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                            numberOfSubFolders = 0;
                        }
                        currentSubFolder = currentFolder.addNode(prefix + System.currentTimeMillis(),
                                "hippostd:folder");
                        currentSubFolder.addMixin("hippo:harddocument");
                        currentSubFolder.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
                        currentSubFolder.addMixin("hippotranslation:translated");
                        currentSubFolder.setProperty("hippotranslation:locale", "en");
                        currentSubFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                        numberOfSubFolders++;
                        log.info("Counter = " + count);
                    }
                } catch (RepositoryException e) {
                    log.error("repository exception while trying to import document", e);
                }
                startRecording();
                count++;
            }
        }

        if (qName.equals("text") || qName.equals("timestamp") || qName.equals("username")) {
            if (offsetcount == offset) {
                startRecording();
            }
        }
        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (offsetcount == offset) {
            try {
                if (qName.equals("page")) {
                    checkCorrectDoc();
                    finishedDoc = doc;
                } else if (qName.equals("title") && recording) {
                        /**/
                    String docTitle = stopRecording();
                    String docName = docTitle.toLowerCase().replaceAll("[^a-z]", "-");

                    Node handle;
                    handle = currentSubFolder.addNode(docName, "hippo:handle");
                    handle.addMixin("hippo:hardhandle");
                    handle.addMixin("hippo:translated");

                    Node translation = handle.addNode("hippo:translation", "hippo:translation");
                    translation.setProperty("hippo:message", docTitle);
                    translation.setProperty("hippo:language", "");

                    doc = handle.addNode(docName, strategy.getType());
                    doc.addMixin("hippo:harddocument");
                    doc.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
                    doc.addMixin("hippotranslation:translated");

                    int creationDateSecondsAgo = rand.nextInt(NUMBER_OF_SECONDS_IN_TWO_YEARS);
                    // lastModifiedSecondsAgo = some random time after creationDateSecondsAgo
                    int lastModifiedSecondsAgo = rand.nextInt(creationDateSecondsAgo);
                    // publicaionDateSecondsAgo = some random time after lastModifiedSecondsAgo
                    int publicaionDateSecondsAgo = rand.nextInt(lastModifiedSecondsAgo);

                    final Calendar creationDate = Calendar.getInstance();
                    creationDate.add(Calendar.SECOND, -1 * creationDateSecondsAgo);
                    final Calendar lastModificationDate = Calendar.getInstance();
                    lastModificationDate.add(Calendar.SECOND, -1 * lastModifiedSecondsAgo);
                    final Calendar publicationDate = Calendar.getInstance();
                    publicationDate.add(Calendar.SECOND, -1 * publicaionDateSecondsAgo);

                    String[] availability = {"live", "preview"};
                    doc.setProperty("hippo:availability", availability);
                    doc.setProperty("hippostd:stateSummary", "live");
                    doc.setProperty("hippostd:state", "published");
                    doc.setProperty("hippostdpubwf:lastModifiedBy", users[rand.nextInt(users.length)]);
                    doc.setProperty("hippostdpubwf:createdBy", users[rand.nextInt(users.length)]);
                    doc.setProperty("hippostdpubwf:lastModificationDate", lastModificationDate);
                    doc.setProperty("hippostdpubwf:creationDate", creationDate);
                    doc.setProperty("hippostdpubwf:publicationDate", publicationDate);
                    doc.setProperty("hippotranslation:locale", "en");
                    doc.setProperty("hippotranslation:id", "" + UUID.randomUUID().toString());
                        /**/
                    strategy.onTitle(doc, currentSubFolder, docTitle);
                } else if (qName.equals("timestamp") && recording) {
                    checkCorrectDoc();
                    String time = stopRecording();
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    Calendar date = Calendar.getInstance();
                    try {
                        date.setTime(format.parse(time));
                        strategy.onTimeStamp(doc, currentSubFolder, date);
                    } catch (ParseException e) {
                        log.error("error parsing date in wikipedia importer", e);
                    }
                } else if (qName.equals("text") && recording) {
                    checkCorrectDoc();
                    String text = stopRecording();
                    strategy.onText(doc, currentSubFolder, text);
                } else if (qName.equals("username") && recording) {
                    checkCorrectDoc();
                    String username = stopRecording();
                    strategy.onUserName(doc, currentSubFolder, username);
                }
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }
        super.endElement(uri, localName, qName);
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (recording) {
            fieldText.append(ch, start, length);
        }
    }

    private void startRecording() {
        fieldText = new StringBuilder();
        recording = true;
    }

    private String stopRecording() {
        recording = false;
        return fieldText.toString().trim();
    }

    private void checkCorrectDoc() throws SAXException {
        if (doc == finishedDoc) {
            throw new SAXException("Doc is same instance as finished doc. This should never happen");
        }
    }


}
