package org.hippoecm.tools.migration;

/*
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nl.hippo.webdav.batchprocessor.Node;
import nl.hippo.webdav.batchprocessor.OperationOnDeletedNodeException;
import nl.hippo.webdav.batchprocessor.ProcessingException;

import org.apache.webdav.lib.Property;
import org.hippoecm.tools.migration.webdav.WebdavHelper;
import org.hippoecm.tools.migration.webdav.WebdavNode;
import org.hippoecm.tools.migration.xml.ExtractorException;
import org.hippoecm.tools.migration.xml.ExtractorInstruction;
import org.hippoecm.tools.migration.xml.XmlExtractor;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
*/

/**
 * DISABLED
 */
public class PublisherDocumentConverter /*extends AbstractDocumentConverter*/ {

    /*
    private static final String CONTENTTYPE_PROPERTY = "hippo:contentType";
    private static final String SIZE_PROPERTY = "hippo:size";
    private static final String SRC_PROPERTY = "hippo:src";
    private static final String LINK_PROPERTY = "hippo:link";
    private static final String HREF_PROPERTY = "hippo:href";
    private static final String TEXT_PROPERTY = "hippo:text";
    private static final String PUBLICATIONDATE_PROPERTY = "hippo:publicationDate";
    private static final String NEWSDATE_PROPERTY = "hippo:newsDate";
    private static final String REVIEW_PROPERTY = "hippo:review";
    private static final String SUMMARY_PROPERTY = "hippo:summary";
    private static final String PAGE_PROPERTY = "hippo:page";
    private static final String TITLE_PROPERTY = "hippo:title";
    private static final String LOCALE_PROPERTY = "hippo:locale";
    private static final String PUBLISHED_PROPERTY = "hippo:published";
    private static final String HIPPO_ID_PROPERTY = "hippo:id";
    
    // The main newsarticle node
    private static final String NEWSARTICLE_NODETYPE = "hippo:newsArticle";

    // subnode for holding localized altText
    private static final String ALTTEXT_NODE = "hippo:altText";
    private static final String ALTTEXT_NODETYPE = "hippo:altText";
    
    // subnode for holding the localized newsarticle body
    private static final String BODY_NODE = "hippo:body";
    private static final String BODY_NODETYPE = "hippo:body";
    
    // Default locale
    private static final String DEFAULT_LOCALE = "en-gb";
    
    // subnode of the rootnode that contains the authors
    private static final String AUTHOR_NODE = "authors";
    private static final String AUTHOR_NODETYPE = "hippo:author";
    private static final String AUTHOR_ID_PROPERTY = "hippo:authorId";

    // subnode of the rootnode that contains the sections
    private static final String SECTION_NODE = "sections";
    private static final String SECTION_NODETYPE = "hippo:section";
    private static final String SECTION_ID_PROPERTY = "hippo:sectionId";

    // TODO: subnode of the rootnode that contains the categories
    //private static final String CATEGORY_NODE = "catagories";
    //private static final String CATEGORY_NODETYPE = "hippo:catagory";
    //private static final String CATEGORY_ID_PROPERTY = "hippo:catagoryId";

    // subnode of the rootnode that contains the magazines
    private static final String MAGAZINE_NODE = "magazines";
    private static final String MAGAZINE_NODETYPE = "hippo:magazine";
    private static final String MAGAZINE_ID_PROPERTY = "hippo:magzineId";

    // subnode of the rootnode that contains the imagesets
    private static final String IMAGESET_NODE = "imagesets";
    private static final String IMAGESET_NODETYPE = "hippo:imageSet";
    private static final String IMAGESET_ID_PROPERTY = "hippo:imageSetId";

    private static final String IMAGE_NODETYPE = "hippo:image";

    // hippo::documentdate = 20040124 
    private static final SimpleDateFormat DOCUMENTDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    // The location of the imagesets in the webdav repository
    private static final String IMAGESET_LOCATION_CONFIG = "imagesetlocation";
    private String imageSetLocation;

    private List instructions = new ArrayList();
    private XmlExtractor xmlExtractor;

    private void setExtractors() {
        instructions.add(new ExtractorInstruction("summary", "string(/article/summary)"));
        instructions.add(new ExtractorInstruction("content", "string(/article/content)"));
        //instructions.add(new ExtractorInstruction("categorization", "string(/article/categorization)"));
        instructions.add(new ExtractorInstruction("links", "string(/article/links)"));
        instructions.add(new ExtractorInstruction("review", "string(/article/review)"));
        instructions.add(new ExtractorInstruction("tags", "string(/article/tags)"));
        instructions.add(new ExtractorInstruction("imageset", "string(/article/images/@id)"));
        instructions.add(new ExtractorInstruction("imagealt", "string(/article/images/@alt)"));
        xmlExtractor = new XmlExtractor(instructions);
    }

    public void postSetupHook() {
        setExtractors();
        imageSetLocation = getPluginConfiguration().getValue(IMAGESET_LOCATION_CONFIG);
    }

    public void convertNodeToJCR(Node webDAVNode, String nodeName, javax.jcr.Node parent) throws RepositoryException,
            ProcessingException, OperationOnDeletedNodeException, IOException {

        Property prop;
        Map properties = new HashMap();
        long id;

        //System.out.println("|");
        //System.out.println("Node     : " + webDAVNode.getUri());

        // Overwrite existing nodes
        if (parent.hasNode(nodeName)) {
            parent.getNode(nodeName).remove();
        }

        try {
            properties = xmlExtractor.extract(new ByteArrayInputStream(webDAVNode.getContents()));
        } catch (ExtractorException e) {
            System.err.println("Error while extracting " + webDAVNode.getUri() + " : " + e.getMessage());
        }

        // Create the new JCR node
        javax.jcr.Node newsArticle = parent.addNode(nodeName, NEWSARTICLE_NODETYPE);
        id = 1 + getMaxIdForNodeType(NEWSARTICLE_NODETYPE);
        newsArticle.setProperty(HIPPO_ID_PROPERTY, id);
        newsArticle.setProperty(PUBLISHED_PROPERTY, false);

        // Body
        String contentLengthAsString = webDAVNode.getProperty(DAV_NAMESPACE, "getcontentlength").getPropertyAsString();
        int contentLength = Integer.parseInt(contentLengthAsString);
        if (contentLength > 0) {
            // Create the new JCR body node
            javax.jcr.Node body = newsArticle.addNode(BODY_NODE, BODY_NODETYPE);
            Value[] locales = new Value[1];
            locales[0] = jcrSession.getValueFactory().createValue(DEFAULT_LOCALE);
            body.setProperty(LOCALE_PROPERTY, locales);

            Property captionProp = webDAVNode.getProperty(HIPPO_NAMESPACE, "caption");
            body.setProperty(TITLE_PROPERTY, captionProp.getPropertyAsString());

            if (properties.containsKey("content")) {
                if (!"".equals((String) properties.get("content"))) {
                    body.setProperty(PAGE_PROPERTY, (String) properties.get("content"));
                }
            }
            if (properties.containsKey("summary")) {
                if (!"".equals((String) properties.get("summary"))) {
                    body.setProperty(SUMMARY_PROPERTY, (String) properties.get("summary"));
                }
            }
            if (properties.containsKey("review")) {
                if (!"".equals((String) properties.get("review"))) {
                    body.setProperty(REVIEW_PROPERTY, (String) properties.get("review"));
                }
            }
        }

        // Author 
        prop = webDAVNode.getProperty(HIPPO_NAMESPACE, "author");
        id = getIdOrCreate(prop.getPropertyAsString(), AUTHOR_NODETYPE, AUTHOR_NODE);
        newsArticle.setProperty(AUTHOR_ID_PROPERTY, id);

        // Section
        prop = webDAVNode.getProperty(HIPPO_NAMESPACE, "section");
        id = getIdOrCreate(prop.getPropertyAsString(), SECTION_NODETYPE, SECTION_NODE);
        newsArticle.setProperty(SECTION_ID_PROPERTY, id);

        // Magazine
        prop = webDAVNode.getProperty(HIPPO_NAMESPACE, "source");
        id = getIdOrCreate(prop.getPropertyAsString(), MAGAZINE_NODETYPE, MAGAZINE_NODE);
        Value[] ids = new Value[1];
        ids[0] = jcrSession.getValueFactory().createValue(id);
        newsArticle.setProperty(MAGAZINE_ID_PROPERTY, ids);

        // Newsdate
        prop = webDAVNode.getProperty(HIPPO_NAMESPACE, "documentdate");
        if (prop != null) {
            newsArticle.setProperty(NEWSDATE_PROPERTY, WebdavHelper.getCalendarFromProperty(prop, DOCUMENTDATE_FORMAT));
        }

        // Publication
        prop = webDAVNode.getProperty(HIPPO_NAMESPACE, "publicationdate");
        if (prop != null) {
            newsArticle.setProperty(PUBLISHED_PROPERTY, true);
            newsArticle.setProperty(PUBLICATIONDATE_PROPERTY, WebdavHelper.getCalendarFromProperty(prop, PUBLICATIONDATE_FORMAT));
        }

        // Imageset
        if (properties.containsKey("imageset") && !"".equals((String) properties.get("imageset"))) {

            // Create the new JCR illustration node
            javax.jcr.Node illustration = newsArticle.addNode("hippo:illustration", "hippo:illustration");

            // Set alternative alt text 
            if (properties.containsKey("imagealt")) {
                if (!"".equals((String) properties.get("imagealt"))) {
                    javax.jcr.Node altText = illustration.addNode(ALTTEXT_NODE, ALTTEXT_NODETYPE);
                    Value[] locales = new Value[1];
                    locales[0] = jcrSession.getValueFactory().createValue(DEFAULT_LOCALE);
                    altText.setProperty(LOCALE_PROPERTY, locales);
                    altText.setProperty(TEXT_PROPERTY, (String) properties.get("imagealt"));
                }
            }

            // create imageset
            id = createImageSet((String) properties.get("imageset"));
            illustration.setProperty(IMAGESET_ID_PROPERTY, id);
        }

        // Links
        List links = parseWebdavLinks(webDAVNode);
        ListIterator li = links.listIterator();
        while (li.hasNext()) {
            Link link = (Link) li.next();
            //System.out.println("Link     : " + link.toString());
            javax.jcr.Node linkNode = newsArticle.addNode(LINK_PROPERTY, LINK_PROPERTY);
            linkNode.setProperty(HREF_PROPERTY, link.getHref());
            linkNode.setProperty(TEXT_PROPERTY, link.getText());
        }

        // TODO: Categories
        // TODO: Relations

    }
*/
    /**
     * Create the imageSet and the images belonging to the imageSet
     * @param imageSet
     * @return the id of the imageSet
     * @throws RepositoryException
     */
    /*
    private long createImageSet(String imageSet) throws RepositoryException {
        if (imageSet.indexOf('/') < 0) {
            return -1;
        }
        String imageSetName = imageSet.substring(imageSet.lastIndexOf('/') + 1);
        String imageSetPath = imageSet.substring(0, imageSet.lastIndexOf('/'));

        long id = getId(imageSetName, IMAGESET_NODETYPE);
        // id has contraint >=0 
        if (id >= 0) {
            return id;
        }

        id = 1 + getMaxIdForNodeType(IMAGESET_NODETYPE);
        checkAndCreateStructureNode(IMAGESET_NODE + "/" + imageSetPath);
        javax.jcr.Node parent = getJcrSession().getRootNode().getNode(IMAGESET_NODE + "/" + imageSetPath);

        // Overwrite existing nodes
        if (parent.hasNode(imageSetName)) {
            parent.getNode(imageSetName).remove();
        }

        javax.jcr.Node imageSetNode = parent.addNode(imageSetName, IMAGESET_NODETYPE);
        imageSetNode.setProperty(HIPPO_ID_PROPERTY, id);

        try {
            WebdavNode webDavNode = new WebdavNode(getHttpClient(), imageSetLocation + imageSet + ".xml");

            ImageSet set = parseWebdavImageSet(webDavNode);
            //System.out.println("imageSet : " + set);

            //  Find alt text from description
            if (!"".equals(set.getDescription())) {
                javax.jcr.Node altText = imageSetNode.addNode(ALTTEXT_NODE, ALTTEXT_NODETYPE);
                Value[] locales = new Value[1];
                locales[0] = jcrSession.getValueFactory().createValue(DEFAULT_LOCALE);
                altText.setProperty(LOCALE_PROPERTY, locales);
                altText.setProperty(TEXT_PROPERTY, set.getDescription());
            }

            // create the images
            for (int i = 0; i < set.getImages().size(); i++) {
                ImageSetImage imageSetImage = (ImageSetImage) set.getImages().get(i);
                javax.jcr.Node imageNode = imageSetNode.addNode(imageSetName + "-" + imageSetImage.getSize(),
                        IMAGE_NODETYPE);
                imageNode.setProperty(SRC_PROPERTY, imageSetImage.getSrc());
                imageNode.setProperty(SIZE_PROPERTY, imageSetImage.getSize());
                imageNode.setProperty(CONTENTTYPE_PROPERTY, imageSetImage.getContentType());
            }
        } catch (ProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return id;
    }
    */

    /**
     * Parse the WebdavNode and fetch the links
     * @param webdavNode
     * @return returns a List of Link objects
     * @throws IOException
     */
    /*
    private List parseWebdavLinks(Node webdavNode) throws IOException {
        ArrayList links = new ArrayList();
        NodeList nodeList;

        // fetch contents of imageset
        byte[] contents;
        try {
            contents = webdavNode.getContents();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document d = db.parse(new ByteArrayInputStream(contents));

            // get image formats
            nodeList = d.getElementsByTagName("link");
            for (int i = 0; i < nodeList.getLength(); i++) {
                org.w3c.dom.Node domNode = nodeList.item(i);
                NamedNodeMap nodeMap = domNode.getAttributes();
                String text = domNode.getFirstChild().getNodeValue();
                String href = nodeMap.getNamedItem("href").getNodeValue();
                links.add(new Link(href, text));
            }

        } catch (OperationOnDeletedNodeException e) {
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        }
        return links;
    }
    */

    /**
     * Parse the WebdavNode and fetch the imagesets
     * @param webdavNode
     * @return a List with the ImageSet objects
     * @throws IOException
     * @throws ProcessingException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws DOMException
     */
    /*
    private ImageSet parseWebdavImageSet(WebdavNode webdavNode) throws IOException, ProcessingException,
            ParserConfigurationException, SAXException, DOMException {

        ImageSet set = new ImageSet();
        ArrayList images = new ArrayList();
        NodeList nodeList;

        // fetch contents of imageset
        byte[] contents = webdavNode.getContents();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(new ByteArrayInputStream(contents));

        // get description
        nodeList = d.getElementsByTagName("description");
        if (nodeList.getLength() > 0) {
            if (nodeList.item(0).getFirstChild() != null) {
                set.setDescription(nodeList.item(0).getFirstChild().getNodeValue());
            }
        }

        // get original
        nodeList = d.getElementsByTagName("original");
        if (nodeList.getLength() > 0) {
            org.w3c.dom.Node domNode = nodeList.item(0);
            NamedNodeMap nodeMap = domNode.getAttributes();
            String src = nodeMap.getNamedItem("src").getNodeValue();
            String size = "original";
            String contentType = nodeMap.getNamedItem("type").getNodeValue();
            images.add(new ImageSetImage(src, size, contentType));
        }

        // get image formats
        nodeList = d.getElementsByTagName("format");
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node domNode = nodeList.item(i);
            NamedNodeMap nodeMap = domNode.getAttributes();
            String src = nodeMap.getNamedItem("src").getNodeValue();
            String size = nodeMap.getNamedItem("name").getNodeValue();
            String contentType = nodeMap.getNamedItem("type").getNodeValue();
            images.add(new ImageSetImage(src, size, contentType));
        }
        set.setImages(images);
        return set;
    }
    */

    /**
     * Class to hold the imageSets
     */
    /*
    private class ImageSet {
        private String description = "";
        private ArrayList images;

        public void setImages(ArrayList images) {
            this.images = images;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

        public List getImages() {
            return this.images;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(getDescription());
            buf.append(' ');

            buf.append('[');
            for (int i = 0; i < images.size(); i++) {
                buf.append(((ImageSetImage) images.get(i)).getSize());
                buf.append(' ');
            }
            buf.append(']');
            return buf.toString();
        }
    }
     */
    
    /**
     * Class to hold the an image belonging to an imageSet
     */
    /*
    private class ImageSetImage {
        private String src;
        private String size;
        private String contentType;

        public ImageSetImage(String fileName, String sizeName, String contentType) {
            this.src = fileName;
            this.size = sizeName;
            this.contentType = contentType;
        }

        public String getSrc() {
            return this.src;
        }

        public String getSize() {
            return this.size;
        }

        public String getContentType() {
            return this.contentType;
        }
    }
    */

    /**
     * Class to hold a (http) link
     */
    /*
    private class Link {
        private String href;
        private String text;

        public Link(String href, String text) {
            this.href = href;
            this.text = text;
        }

        public String getHref() {
            return this.href;
        }

        public String getText() {
            return this.text;
        }

        public String toString() {
            return this.text + " => " + this.href;
        }
    }
    */
}
