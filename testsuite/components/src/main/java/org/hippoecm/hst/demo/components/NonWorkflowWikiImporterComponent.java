/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.demo.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Wikipedia document importer creates documents from wikipedia dumps
 *
 * Note that this is <strong>not</strong> production code! Real code should use
 * workflow instead of the low-level jcr calls in this class. This just serves
 * to import many wiki documents as efficient as possible
 */
public class NonWorkflowWikiImporterComponent extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(NonWorkflowWikiImporterComponent.class);
    private static final List<String> VALID_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");

    // Matches headers in the wikipedia format having two or three equals-signs
    private static final String blockSeparator = "===?([^=]*?)===?";
    private static final Pattern blockSeparatorPattern = Pattern.compile(blockSeparator);

    // Matches the first word for each category (to keep the number of categories down)
    private final Pattern categoryPattern = Pattern.compile("\\[\\[Category:(\\w+).*?]]");

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        request.setAttribute("message", request.getParameter("message"));
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String numberStr = request.getParameter("number");
        long start = System.currentTimeMillis();
        if (numberStr != null) {
            SAXParserFactoryImpl impl = new SAXParserFactoryImpl();
            SAXParser parser;
            int numberOfWikiDocs = 0;
            try {
                numberOfWikiDocs = Integer.parseInt(numberStr);
            } catch (NumberFormatException e) {
                response.setRenderParameter("message", "number must be a number but was '" + numberStr + "'");
            }

            if (numberOfWikiDocs <= 0) {
                response.setRenderParameter("message", "number must be a number larger than 0 but was '" + numberStr
                        + "'");
            }

            String wikiContentFileSystem = request.getParameter("filesystemLocation");

            if (numberOfWikiDocs > 100 && StringUtils.isEmpty(wikiContentFileSystem)) {
                response.setRenderParameter(
                        "message",
                        "When number is larger than 100, you need to specify the filesystem location (for exmaple /home/use/Downloads/enwiki-20100622-pages-articles.xml) where wikipedia content can be found that "
                                + "has more than 100 docs. If you choose less than 100, a built in wikipedia xml is used that contains 100 items");
                return;
            }

            String offsetStr = request.getParameter("offset");
            int offset = 0;
            if (StringUtils.isNotBlank(offsetStr)) {
                try {
                    offset = Integer.parseInt(offsetStr);
                    if (offset < 0) {
                        offset = 0;
                    }
                } catch (NumberFormatException e) {
                    response.setRenderParameter("message", "offset must be a number but was '" + offsetStr + "'");
                    return;
                }
            }

            String maxDocsPerFolderStr = request.getParameter("maxDocsPerFolder");
            int maxDocsPerFolder = 200;
            if (StringUtils.isNotBlank(maxDocsPerFolderStr)) {
                try {
                    maxDocsPerFolder = Integer.parseInt(maxDocsPerFolderStr);
                    if (maxDocsPerFolder < 0) {
                        maxDocsPerFolder = 0;
                    }
                } catch (NumberFormatException e) {
                    response.setRenderParameter("message", "maxDocsPerFolder must be a number but was '"
                            + maxDocsPerFolderStr + "'");
                    return;
                }
            }

            String maxSubFolderStr = request.getParameter("maxSubFolder");
            int maxSubFolder = 50;
            if (StringUtils.isNotBlank(maxSubFolderStr)) {
                try {
                    maxSubFolder = Integer.parseInt(maxSubFolderStr);
                    if (maxSubFolder < 0) {
                        maxSubFolder = 0;
                    }
                } catch (NumberFormatException e) {
                    response.setRenderParameter("message", "maxSubFolder must be a number but was '" + maxSubFolderStr
                            + "'");
                    return;
                }
            }

            String imageStr = request.getParameter("images");
            boolean addImages = (imageStr != null);

            try {
                parser = impl.newSAXParser();
                InputStream wikiStream = null;
                File f = null;
                if (StringUtils.isEmpty(wikiContentFileSystem)) {
                    wikiStream = NonWorkflowWikiImporterComponent.class.getClassLoader().getResourceAsStream(
                            "enwiki-20081008-pages-articles.xml.100.top.xml");
                } else {
                    f = new File(wikiContentFileSystem);
                }

                WikiPediaToJCRHandler handler = null;

                try {
                    Session writableSession = this.getPersistableSession(request);
                    Node baseNode = writableSession.getNode("/" + getSiteContentBasePath(request));

                    Node wikiFolder;

                    if (!baseNode.hasNode("wikipedia")) {
                        wikiFolder = baseNode.addNode("wikipedia", "hippostd:folder");
                        wikiFolder.addMixin("mix:referenceable");
                        wikiFolder.addMixin("hippotranslation:translated");
                        wikiFolder.setProperty("hippotranslation:locale", "en");
                        wikiFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                    } else {
                        wikiFolder = baseNode.getNode("wikipedia");
                    }

                    handler = new WikiPediaToJCRHandler(wikiFolder, numberOfWikiDocs, offset, maxDocsPerFolder,
                            maxSubFolder, addImages);

                    if (wikiStream == null) {
                        parser.parse(f, handler);
                    } else {
                        parser.parse(wikiStream, handler);
                    }
                } catch (ForcedStopException e) {
                    // successful handler quits after numberOfWikiDocs has been achieved
                } catch (Exception e) {
                    log.warn("Exception during importing wikipedia docs", e);
                    response.setRenderParameter("message",
                            "An exception happened. Did not import wiki docs. " + e.toString());
                    return;
                }
            } catch (ParserConfigurationException e) {
                response.setRenderParameter("message", "Did not import wiki: " + e.toString());
                return;
            }
            String relateString = request.getParameter("relate");
            if (relateString != null) {
                int numberOfRelations = 0;
                try {
                    numberOfRelations = Integer.parseInt(relateString);
                } catch (NumberFormatException e) {
                    response.setRenderParameter("message", "number of relations must be a number but was '" + relateString
                            + "'");
                }
                relateDocuments(request, response, getRelateNodesOperation(), numberOfRelations, "uuid");
            }

            String linkString = request.getParameter("link");
            if (linkString != null) {
                int numberOfLinks = 0;
                try {
                    numberOfLinks = Integer.parseInt(linkString);
                } catch (NumberFormatException e) {
                    response.setRenderParameter("message", "number of links must be a number but was '" + linkString + "'");
                }
                relateDocuments(request, response, getLinkNodesOperation(), numberOfLinks, "versionHistory");
            }

            String translateString = request.getParameter("translate");
            if (translateString != null) {
                int numberOfTranslations = 0;
                try {
                    numberOfTranslations = Integer.parseInt(translateString);
                } catch (NumberFormatException e) {
                    response.setRenderParameter("message", "number of translations must be a number but was '"
                            + translateString + "'");
                }
                relateDocuments(request, response, getTranslateOperation(), numberOfTranslations, "uuid");
            }
        }


        response.setRenderParameter("message", "Successfully completed operation in "
                + (System.currentTimeMillis() - start) + "ms.");
    }

    class WikiPediaToJCRHandler extends DefaultHandler {

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
        private final boolean addImages;
        private StringBuilder fieldText;
        private boolean recording;
        int count = 0;
        int offsetcount = 0;
        long startTime = 0;
        private final String[] users = {"ard", "bard", "arje", "artur", "reijn", "berry", "frank", "mathijs",
                "junaid", "ate", "tjeerd", "verberg", "simon", "jannis"};
        private final Random rand;

        public WikiPediaToJCRHandler(Node wikiFolder, int total, final int offset, final int maxDocsPerFolder,
                final int maxSubFolders, final boolean addImages) throws Exception {
            this.wikiFolder = wikiFolder;
            this.total = total;
            this.offset = offset;
            this.maxDocsPerFolder = maxDocsPerFolder;
            this.maxSubFolders = maxSubFolders;
            this.addImages = addImages;
            currentFolder = wikiFolder.addNode("wiki-" + System.currentTimeMillis(), "hippostd:folder");
            currentFolder.addMixin("mix:referenceable");
            currentFolder.addMixin("hippotranslation:translated");
            currentFolder.setProperty("hippotranslation:locale", "en");
            currentFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
            currentSubFolder = currentFolder.addNode("wiki-" + System.currentTimeMillis(), "hippostd:folder");
            currentSubFolder.addMixin("mix:referenceable");
            currentSubFolder.addMixin("hippotranslation:translated");
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
                        System.out.println("Offset '" + offset + "' not yet reached. Currently at '" + offsetcount
                                + "'");
                    }
                }

                if (offsetcount == offset) {
                    try {
                        if (count >= total) {
                            System.out.println(total);
                            wikiFolder.getSession().save();

                            System.out.println("Total added wiki docs = " + count + ". It took "
                                    + (System.currentTimeMillis() - startTime) + " ms.");
                            throw new ForcedStopException();
                        }
                        if ((count % maxDocsPerFolder) == 0 && count != 0) {
                            wikiFolder.getSession().save();
                            if (numberOfSubFolders >= maxSubFolders) {
                                currentFolder = wikiFolder.addNode("wiki-" + System.currentTimeMillis(),
                                        "hippostd:folder");
                                currentFolder.addMixin("mix:referenceable");
                                currentFolder.addMixin("hippotranslation:translated");
                                currentFolder.setProperty("hippotranslation:locale", "en");
                                currentFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                                numberOfSubFolders = 0;
                            }
                            currentSubFolder = currentFolder.addNode("wiki-" + System.currentTimeMillis(),
                                    "hippostd:folder");
                            currentSubFolder.addMixin("mix:referenceable");
                            currentSubFolder.addMixin("hippotranslation:translated");
                            currentSubFolder.setProperty("hippotranslation:locale", "en");
                            currentSubFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                            numberOfSubFolders++;
                            System.out.println("Counter = " + count);
                        }
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                    startRecording();
                    count++;
                }
            }

            if (qName.equals("text") || qName.equals("timestamp")) {
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
                        handle.addMixin("mix:referenceable");
                        handle.addMixin("hippo:translated");

                        Node translation = handle.addNode("hippo:translation", "hippo:translation");
                        translation.setProperty("hippo:message", docTitle);
                        translation.setProperty("hippo:language", "");

                        doc = handle.addNode(docName, "demosite:wikidocument");
                        doc.addMixin("mix:referenceable");
                        doc.addMixin("hippotranslation:translated");

                        int creationDateSecondsAgo = new Random().nextInt(NUMBER_OF_SECONDS_IN_TWO_YEARS);
                        // lastModifiedSecondsAgo = some random time after creationDateSecondsAgo
                        int lastModifiedSecondsAgo = new Random().nextInt(creationDateSecondsAgo);
                        // publicaionDateSecondsAgo = some random time after lastModifiedSecondsAgo
                        int publicaionDateSecondsAgo = new Random().nextInt(lastModifiedSecondsAgo);

                        final Calendar creationDate = Calendar.getInstance();
                        creationDate.add(Calendar.SECOND, (-1 * creationDateSecondsAgo));
                        final Calendar lastModificationDate = Calendar.getInstance();
                        lastModificationDate.add(Calendar.SECOND, (-1 * lastModifiedSecondsAgo));
                        final Calendar publicationDate = Calendar.getInstance();
                        publicationDate.add(Calendar.SECOND, (-1 * publicaionDateSecondsAgo));

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
                        doc.setProperty("demosite:title", docTitle);
                    } else if (qName.equals("timestamp") && recording) {
                        checkCorrectDoc();
                        String time = stopRecording();
                        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        try {
                            Node placetime = doc.addNode("demosite:placetime", "demosite:placetimecompound");
                            Calendar date = Calendar.getInstance();
                            date.setTime(format.parse(time));

                            placetime.setProperty("demosite:date", date);

                            Node place = placetime.addNode("demosite:demosite_placecompound", "demosite:placecompound");
                            place.setProperty("demosite:city", "City");
                            place.setProperty("demosite:country", "Country");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else if (qName.equals("text") && recording) {
                        checkCorrectDoc();
                        String text = stopRecording();

                        createBlocks(text);
                        addCategories(text);
                    }
                } catch (RepositoryException e) {
                    throw new SAXException(e);
                }
            }
            super.endElement(uri, localName, qName);
        }

        private void addCategories(String text) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            Matcher m = categoryPattern.matcher(text);
            List<String> categories = new ArrayList<String>();
            while (m.find()) {
                categories.add(m.group(1));
            }
            doc.setProperty("demosite:categories", categories.toArray(new String[categories.size()]));
        }

        private void createBlocks(String text) throws ItemExistsException, PathNotFoundException,
                NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
                RepositoryException {
            //Divide text into blocks
            Matcher m = blockSeparatorPattern.matcher(text);
            int textProcessedIndex = 0;

            while (m.find()) {
                // New block found. Create the previous block
                createBlock(text.substring(textProcessedIndex, m.start()));
                textProcessedIndex = m.start();
            }

            // create the last block
            createBlock(text.substring(textProcessedIndex));

        }

        private void createBlock(String textBlock) throws ItemExistsException, PathNotFoundException,
                NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
                RepositoryException {
            Matcher m = blockSeparatorPattern.matcher(textBlock);
            Node block = doc.addNode("demosite:block", "demosite:contentblockcompound");
            String textBody = textBlock;

            if (m.find()) {
                String textHeader = m.group(1);
                if (m.end() == textBody.length()) {
                    // There is nothing more
                    textBody = "";
                } else {
                    textBody = textBlock.substring(m.end());
                }
                block.setProperty("demosite:header", textHeader.trim());
            }

            Node body = block.addNode("demosite:body", "hippostd:html");

            if (addImages) {
                textBody = createImages(body, textBody);
            }

            body.setProperty("hippostd:content", textBody.trim());
        }

        private String createImages(Node body, String text) throws ItemExistsException, PathNotFoundException,
                NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
                RepositoryException {
            final Pattern pattern = Pattern.compile("\\[\\[(?:Image|File):([^|\\]]*)");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String imageName = matcher.group(1).trim();
                String imageUuid = createImage(imageName);
                if (imageUuid != null) {
                    text = text.substring(0, matcher.start()) + "<img src=\"" + imageName
                            + "/{_document}/hippogallery:original\"/>" + text.substring(matcher.end());
                    Node imageRef;
                    if (body.hasNode(imageName)) {
                        imageRef = body.getNode(imageName);
                    } else {
                        imageRef = body.addNode(imageName, "hippo:facetselect");
                        imageRef.setProperty("hippo:docbase", imageUuid);
                        String[] esa = {};
                        imageRef.setProperty("hippo:facets", esa);
                        imageRef.setProperty("hippo:modes", esa);
                        imageRef.setProperty("hippo:values", esa);
                    }
                } else {
                    text = text.substring(0, matcher.start()) + imageName + text.substring(matcher.end());
                }
                matcher = pattern.matcher(text);
            }
            return text;
        }

        /**
         * @param name The name of the image
         * @return The UUID of the created image
         */
        private String createImage(String name) {
            try {
                String imgExt = name.substring(name.lastIndexOf('.') + 1);
                if (!VALID_IMAGE_EXTENSIONS.contains(imgExt)) {
                    return null;
                }

                Node images = doc.getSession().getRootNode().getNode("content/gallery/images");

                // Create wikipedia folder
                Node wikiImages;
                if (!images.hasNode("wikipedia")) {
                    wikiImages = images.addNode("wikipedia", "hippogallery:stdImageGallery");
                    wikiImages.addMixin("mix:referenceable");
                    String[] foldertype = {"new-image-folder"};
                    wikiImages.setProperty("hippostd:foldertype", foldertype);
                    String[] gallerytype = {"hippogallery:imageset"};
                    wikiImages.setProperty("hippostd:gallerytype", gallerytype);
                } else {
                    wikiImages = images.getNode("wikipedia");
                }

                // Create document subfolder
                Node imgSubFolder;
                if (!wikiImages.hasNode(currentSubFolder.getName())) {
                    imgSubFolder = wikiImages.addNode(currentSubFolder.getName(), "hippogallery:stdImageGallery");
                    imgSubFolder.addMixin("mix:referenceable");
                    String[] foldertype = {"new-image-folder"};
                    imgSubFolder.setProperty("hippostd:foldertype", foldertype);
                    String[] gallerytype = {"hippogallery:imageset"};
                    imgSubFolder.setProperty("hippostd:gallerytype", gallerytype);
                } else {
                    imgSubFolder = wikiImages.getNode(currentSubFolder.getName());
                }

                // Create document folder
                Node imgFolder;
                if (!imgSubFolder.hasNode(doc.getName())) {
                    imgFolder = imgSubFolder.addNode(doc.getName(), "hippogallery:stdImageGallery");
                    imgFolder.addMixin("mix:referenceable");
                    String[] foldertype = {"new-image-folder"};
                    imgFolder.setProperty("hippostd:foldertype", foldertype);
                    String[] gallerytype = {"hippogallery:imageset"};
                    imgFolder.setProperty("hippostd:gallerytype", gallerytype);
                } else {
                    imgFolder = imgSubFolder.getNode(doc.getName());
                }

                // Create handle
                Node imgHandle;
                if (imgFolder.hasNode(name)) {
                    imgHandle = imgFolder.getNode(name);
                } else {
                    imgHandle = imgFolder.addNode(name, "hippo:handle");
                    imgHandle.addMixin("mix:referenceable");
                }

                // Create image set (if it doesn't exist)
                if (!imgHandle.hasNode(name)) {
                    Node imgDoc = imgHandle.addNode(name, "hippogallery:imageset");
                    imgDoc.addMixin("mix:referenceable");
                    String[] availability = {"live", "preview"};
                    imgDoc.setProperty("hippo:availability", availability);
                    imgDoc.setProperty("hippogallery:filename", name);

                    //Thumbnail node might already exist
                    Node imgThumb;
                    if (imgDoc.hasNode("hippogallery:thumbnail")) {
                        imgThumb = imgDoc.getNode("hippogallery:thumbnail");
                    } else {
                        imgThumb = imgDoc.addNode("hippogallery:thumbnail", "hippogallery:image");
                    }

                    imgThumb.setProperty("jcr:lastModified", Calendar.getInstance());
                    imgThumb.setProperty("jcr:mimeType", "image/" + imgExt);
                    imgThumb.setProperty("hippogallery:height", 50L);
                    imgThumb.setProperty("hippogallery:width", 300L);

                    Node imgOrig = imgDoc.addNode("hippogallery:original", "hippogallery:image");
                    imgOrig.setProperty("jcr:lastModified", Calendar.getInstance());
                    imgOrig.setProperty("jcr:mimeType", "image/" + imgExt);
                    imgOrig.setProperty("hippogallery:height", 50L);
                    imgOrig.setProperty("hippogallery:width", 300L);

                    BufferedImage image = new BufferedImage(300, 50, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = image.createGraphics();
                    g2d.setPaint(Color.blue);
                    g2d.setFont(new Font("Serif", Font.BOLD, 32));
                    g2d.drawString(name, 5, 35);
                    g2d.dispose();

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(image, imgExt, os);
                    InputStream is = new ByteArrayInputStream(os.toByteArray());
                    imgThumb.setProperty("jcr:data", imgThumb.getSession().getValueFactory().createBinary(is));
                    is = new ByteArrayInputStream(os.toByteArray());
                    imgOrig.setProperty("jcr:data", imgThumb.getSession().getValueFactory().createBinary(is));
                }

                return imgHandle.getIdentifier();
            } catch (PathNotFoundException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
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

    /**
     * Relates the nodes to the previous nodes (in order of UUID)
     *
     * @param request
     * @param response
     */
    private void relateDocuments(HstRequest request, HstResponse response, Operation op, final int relations,
            String orderByProperty) {
        if (relations < 1) {
            return;
        }

        try {
            Session writableSession = this.getPersistableSession(request);
            Node wikipedia = writableSession.getNode("/" + request.getRequestContext().getSiteContentBasePath() + "/wikipedia");
            @SuppressWarnings("deprecation")
            Query q = writableSession
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(
                            "//element(*,demosite:wikidocument)[@hippo:paths='" + wikipedia.getIdentifier() + "'] order by @jcr:uuid",
                            Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator it = result.getNodes();

            // Fill first queue with elements, which can't be fully linked yet
            Node current;
            LinkedList<Node> firsts = new LinkedList<Node>();
            LinkedList<Node> previous = new LinkedList<Node>();
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
            response.setRenderParameter("message", "An exception happened. Did not relate wiki docs. " + e.toString());
        }
    }

    private Operation getRelateNodesOperation() {
        return new Operation() {
            @Override
            public void perform(Node from, Node to) {
                Node relateddocs;
                try {
                    // Don't do anything if the node is checked in
                    if (!from.isCheckedOut()) {
                        return;
                    }

                    if (from.hasNode("relateddocs:docs")) {
                        relateddocs = from.getNode("relateddocs:docs");
                    } else {
                        relateddocs = from.addNode("relateddocs:docs", "relateddocs:docs");
                    }

                    Node reldoc = relateddocs.addNode("relateddocs:reldoc", "hippo:facetselect");
                    String[] empty = {};
                    reldoc.setProperty("hippo:docbase", to.getParent().getIdentifier());
                    reldoc.setProperty("hippo:facets", empty);
                    reldoc.setProperty("hippo:modes", empty);
                    reldoc.setProperty("hippo:values", empty);
                } catch (RepositoryException e) {
                    log.warn("Couldn't relate documents", e);
                }
            }
        };
    }

    private Operation getLinkNodesOperation() {
        return new Operation() {
            @Override
            public void perform(Node from, Node to) {
                try {
                    // Don't do anything if the node is checked in
                    if (!from.isCheckedOut()) {
                        return;
                    }

                    // Add the facetselect node
                    Node blockBody = from.getNode("demosite:block/demosite:body");
                    Node facetselect;
                    if (!blockBody.hasNode(to.getName())) {
                        facetselect = blockBody.addNode(to.getName(), "hippo:facetselect");

                        String[] empty = {};
                        facetselect.setProperty("hippo:docbase", to.getParent().getIdentifier());
                        facetselect.setProperty("hippo:facets", empty);
                        facetselect.setProperty("hippo:modes", empty);
                        facetselect.setProperty("hippo:values", empty);
                    }

                    // Put the link in the block contents, after the <body> tag
                    String link = "<a href=\"" + to.getName() + "\">" + to.getName() + "</a><br />";
                    String text = blockBody.getProperty("hippostd:content").getString();
                    text = link + text;
                    blockBody.setProperty("hippostd:content", text);
                } catch (RepositoryException e) {
                    log.warn("Couldn't link documents", e);
                }
            }
        };
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
                    tHandle.addMixin("mix:referenceable");
                    tHandle.addMixin("hippo:translated");

                    // Create translated document
                    tDoc = tHandle.addNode(handle.getName(), "demosite:wikidocument");
                    tDoc.addMixin("mix:referenceable");
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
            translatedFolder.addMixin("mix:referenceable");
            translatedFolder.addMixin("hippotranslation:translated");
            translatedFolder.setProperty("hippotranslation:locale", locale);
            translatedFolder.setProperty("hippotranslation:id", folderToTranslate.getProperty("hippotranslation:id")
                    .getString());
        }
        return translatedFolder;
    }

    class ForcedStopException extends RuntimeException {
        private static final long serialVersionUID = 1L;

    }

    interface Operation {
        public void perform(Node from, Node to);
    }

}
