package org.onehippo.cms7.essentials.dashboard.wiki;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @version "$Id$"
 */
public class WikiPediaToJCRHandler extends DefaultHandler {

    private static final List<String> VALID_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");

    // Matches the first word for each category (to keep the number of categories down)
    private static final Pattern categoryPattern = Pattern.compile("\\[\\[Category:(\\w+).*?]]");

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

    private String type;
    private StringBuilder fieldText;
    private boolean recording;
    int count = 0;
    int offsetcount = 0;
    long startTime = 0;
    private final String[] users = {"ard", "bard", "arje", "artur", "reijn", "berry", "frank", "mathijs",
            "junaid", "ate", "tjeerd", "verberg", "simon", "jannis"};
    private final Random rand;

    public WikiPediaToJCRHandler(Node wikiFolder, int total, final int offset, final int maxDocsPerFolder,
                                 final int maxSubFolders, final boolean addImages, final String type) throws Exception {
        this.type = type;
        this.wikiFolder = wikiFolder;
        this.total = total;
        this.offset = offset;
        this.maxDocsPerFolder = maxDocsPerFolder;
        this.maxSubFolders = maxSubFolders;
        this.addImages = addImages;
        currentFolder = wikiFolder.addNode("wiki-" + System.currentTimeMillis(), "hippostd:folder");
        currentFolder.addMixin("hippo:harddocument");
        currentFolder.setProperty("hippo:paths", new String[]{});
        currentFolder.addMixin("hippotranslation:translated");
        currentFolder.setProperty("hippotranslation:locale", "en");
        currentFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
        currentSubFolder = currentFolder.addNode("wiki-" + System.currentTimeMillis(), "hippostd:folder");
        currentSubFolder.addMixin("hippo:harddocument");
        currentSubFolder.addMixin("hippotranslation:translated");
        currentSubFolder.setProperty("hippo:paths", new String[]{});
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
                            currentFolder.addMixin("hippo:harddocument");
                            currentFolder.setProperty("hippo:paths", new String[]{});
                            currentFolder.addMixin("hippotranslation:translated");
                            currentFolder.setProperty("hippotranslation:locale", "en");
                            currentFolder.setProperty("hippotranslation:id", UUID.randomUUID().toString());
                            numberOfSubFolders = 0;
                        }
                        currentSubFolder = currentFolder.addNode("wiki-" + System.currentTimeMillis(),
                                "hippostd:folder");
                        currentSubFolder.addMixin("hippo:harddocument");
                        currentSubFolder.setProperty("hippo:paths", new String[]{});
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
                    handle.addMixin("hippo:hardhandle");
                    handle.addMixin("hippo:translated");

                    Node translation = handle.addNode("hippo:translation", "hippo:translation");
                    translation.setProperty("hippo:message", docTitle);
                    translation.setProperty("hippo:language", "");

                    doc = handle.addNode(docName, type);
                    doc.addMixin("hippo:harddocument");
                    doc.setProperty("hippo:paths", new String[]{});
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
                    doc.setProperty("mytestproject:title", docTitle);
                } else if (qName.equals("timestamp") && recording) {
                    checkCorrectDoc();
                    String time = stopRecording();
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//                        try {
//                            todo
//                            Node placetime = doc.addNode("mytestproject:placetime", "mytestproject:placetimecompound");
//                            Calendar date = Calendar.getInstance();
//                            date.setTime(format.parse(time));
//
//                            placetime.setProperty("mytestproject:date", date);
//
//                            Node place = placetime.addNode("mytestproject:demosite_placecompound", "mytestproject:placecompound");
//                            place.setProperty("mytestproject:city", "City");
//                            place.setProperty("mytestproject:country", "Country");
//                        } catch (ParseException e) {
//                            e.printStackTrace();
//                        }
                } else if (qName.equals("text") && recording) {
                    checkCorrectDoc();
                    String text = stopRecording();
                    //createBlocks(text);
                    addCategories(text);
                }
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }
        super.endElement(uri, localName, qName);
    }

    private void addCategories(String text) throws RepositoryException {
        Matcher m = categoryPattern.matcher(text);
        List<String> categories = new ArrayList<String>();
        while (m.find()) {
            categories.add(m.group(1));
        }
        doc.setProperty("mytestproject:categories", categories.toArray(new String[categories.size()]));
    }
//todo
//        private void createBlocks(String text) throws RepositoryException {
//            //Divide text into blocks
//            Matcher m = blockSeparatorPattern.matcher(text);
//            int textProcessedIndex = 0;
//
//            while (m.find()) {
//                // New block found. Create the previous block
//                createBlock(text.substring(textProcessedIndex, m.start()));
//                textProcessedIndex = m.start();
//            }
//
//            // create the last block
//            createBlock(text.substring(textProcessedIndex));
//
//        }

//        private void createBlock(String textBlock) throws RepositoryException {
//            Matcher m = blockSeparatorPattern.matcher(textBlock);
//            Node block = doc.addNode("mytestproject:block", "mytestproject:contentblockcompound");
//            String textBody = textBlock;
//
//            if (m.find()) {
//                String textHeader = m.group(1);
//                if (m.end() == textBody.length()) {
//                    // There is nothing more
//                    textBody = "";
//                } else {
//                    textBody = textBlock.substring(m.end());
//                }
//                block.setProperty("mytestproject:header", textHeader.trim());
//            }
//
//            Node body = block.addNode("mytestproject:body", "hippostd:html");
//
//            if (addImages) {
//                textBody = createImages(body, textBody);
//            }
//
//            body.setProperty("hippostd:content", textBody.trim());
//        }

    private String createImages(Node body, String text) throws RepositoryException {
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
                wikiImages.addMixin("hippo:harddocument");
                wikiImages.setProperty("hippo:paths", new String[]{});
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
                imgSubFolder.addMixin("hippo:harddocument");
                imgSubFolder.setProperty("hippo:paths", new String[]{});
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
                imgFolder.addMixin("hippo:harddocument");
                imgFolder.setProperty("hippo:paths", new String[]{});
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
                imgHandle.addMixin("hippo:hardhandle");
            }

            // Create image set (if it doesn't exist)
            if (!imgHandle.hasNode(name)) {
                Node imgDoc = imgHandle.addNode(name, "hippogallery:imageset");
                imgDoc.addMixin("hippo:harddocument");
                imgDoc.setProperty("hippo:paths", new String[]{});
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
