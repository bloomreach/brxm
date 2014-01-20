package org.onehippo.cms7.essentials.dashboard.wiki;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public abstract class DefaultStrategy implements WikiStrategy {

    private static Logger log = LoggerFactory.getLogger(DefaultStrategy.class);

    // Matches headers in the wikipedia format having two or three equals-signs
//    private static final String blockSeparator = "===?([^=]*?)===?";
//    // private static final Pattern blockSeparatorPattern = Pattern.compile(blockSeparator);
//
//
//    // Matches the first word for each category (to keep the number of categories down)
//    private static final Pattern categoryPattern = Pattern.compile("\\[\\[Category:(\\w+).*?]]");


    private static final List<String> VALID_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");

    private Properties properties;

    protected DefaultStrategy(final Properties properties) {
        this.properties = properties;
    }

    public Node getOrAddNode(Node document, String path, String type) throws RepositoryException {
        if (document.hasNode(path)) {
            return document.getNode(path);
        } else {
            final Node body = document.addNode(path, type);
            return body;
        }
    }

    /**
     * @param doc              the current document
     * @param currentSubFolder current subfolder
     * @param body             is the image type example doc - <body/> node
     * @param text             wiki text
     * @return
     * @throws RepositoryException
     */
    protected String createImages(Node doc, Node currentSubFolder, Node body, String text) throws RepositoryException {
        final Pattern pattern = Pattern.compile("\\[\\[(?:Image|File):([^|\\]]*)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String imageName = matcher.group(1).trim();
            String imageUuid = createImage(doc, currentSubFolder, imageName);
            if (imageUuid != null) {
                text = MessageFormat.format("{0}<img src=\"{1}/'{'_document'}'/hippogallery:original\"/>{2}", text.substring(0, matcher.start()), imageName, text.substring(matcher.end()));
                if (!body.hasNode(imageName)) {
                    Node imageRef = body.addNode(imageName, "hippo:facetselect");
                    imageRef.setProperty("hippo:docbase", imageUuid);
                    String[] esa = ArrayUtils.EMPTY_STRING_ARRAY;
                    imageRef.setProperty("hippo:facets", esa);
                    imageRef.setProperty("hippo:modes", esa);
                    imageRef.setProperty("hippo:values", esa);
                }
            } else {
                text = MessageFormat.format("{0}{1}{2}", text.substring(0, matcher.start()), imageName, text.substring(matcher.end()));
            }
            matcher = pattern.matcher(text);
        }
        return new StringBuffer().append("<html><body>").append(text).append("</body></html>").toString();
    }

    /**
     * @param name The name of the image
     * @return The UUID of the created image
     */
    protected String createImage(Node doc, Node currentSubFolder, String name) {
        try {
            String imgExt = name.substring(name.lastIndexOf('.') + 1);
            if (!VALID_IMAGE_EXTENSIONS.contains(imgExt)) {
                return null;
            }

            Node images = doc.getSession().getNode(properties.getProperty("imageContentBasePath"));

            String containerName = properties.containsKey("container") ? properties.getProperty("container") : "wikipedia";

            // Create wikipedia folder
            Node wikiImages;
            if (!images.hasNode(containerName)) {
                wikiImages = images.addNode(containerName, "hippogallery:stdImageGallery");
                wikiImages.addMixin("hippo:harddocument");
                wikiImages.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
                String[] foldertype = {"new-image-folder"};
                wikiImages.setProperty("hippostd:foldertype", foldertype);
                String[] gallerytype = {"hippogallery:imageset"};
                wikiImages.setProperty("hippostd:gallerytype", gallerytype);
            } else {
                wikiImages = images.getNode(containerName);
            }

            // Create document subfolder
            Node imgSubFolder;
            if (!wikiImages.hasNode(currentSubFolder.getName())) {
                imgSubFolder = wikiImages.addNode(currentSubFolder.getName(), "hippogallery:stdImageGallery");
                imgSubFolder.addMixin("hippo:harddocument");
                imgSubFolder.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
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
                imgFolder.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
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
                imgDoc.setProperty("hippo:paths", ArrayUtils.EMPTY_STRING_ARRAY);
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


                if (isNotSimulation()) {
                    imgThumb.setProperty("jcr:lastModified", Calendar.getInstance());
                    imgThumb.setProperty("jcr:mimeType", "image/" + imgExt);
                }

                imgThumb.setProperty("hippogallery:height", 50L);
                imgThumb.setProperty("hippogallery:width", 300L);

                Node imgOrig = imgDoc.addNode("hippogallery:original", "hippogallery:image");
                if (isNotSimulation()) {
                    imgOrig.setProperty("jcr:lastModified", Calendar.getInstance());
                    imgOrig.setProperty("jcr:mimeType", "image/" + imgExt);
                }
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
                if (isNotSimulation()) {
                    imgThumb.setProperty("jcr:data", imgThumb.getSession().getValueFactory().createBinary(is));
                }
                is = new ByteArrayInputStream(os.toByteArray());
                if (isNotSimulation()) {
                    imgOrig.setProperty("jcr:data", imgThumb.getSession().getValueFactory().createBinary(is));
                }

            }

            return imgHandle.getIdentifier();
        } catch (Exception e) {
            log.error("Exception while trying to convert import wiki document", e);
        }
        return null;
    }

    private boolean isNotSimulation() {
        return !Boolean.valueOf(properties.getProperty("simulation"));
    }

}
