package ${beansPackage};

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType=EventsDocument.DOCUMENT_TYPE)
public class EventsDocument extends HippoDocument {

    /**
     * The document type of the news document.
     */
    public final static String DOCUMENT_TYPE = "essentialsdemo:eventsdocument";

    private final static String TITLE = "essentialsdemo:title";
    private final static String DATE = "essentialsdemo:date";
    private final static String INTRODUCTION = "essentialsdemo:introduction";
    private final static String IMAGE = "essentialsdemo:image";
    private final static String CONTENT = "essentialsdemo:content";
    private final static String LOCATION = "essentialsdemo:location";
    private final static String END_DATE = "essentialsdemo:enddate";

    /**
     * Get the title of the document.
     *
     * @return the title
     */
    public String getTitle() {
        return getProperty(TITLE);
    }

    /**
     * Get the date of the document, i.e. the start date of the event.
     *
     * @return the (start) date
     */
    public Calendar getDate() {
        return getProperty(DATE);
    }

    /**
     * Get the introduction of the document.
     *
     * @return the introduction
     */
    public String getIntroduction() {
        return getProperty(INTRODUCTION);
    }

    /**
     * Get the image of the document.
     *
     * @return the image
     */
    public HippoGalleryImageSet getImage() {
        return getLinkedBean(IMAGE, HippoGalleryImageSet.class);
    }

    /**
     * Get the main content of the document.
     *
     * @return the content
     */
    public HippoHtml getContent() {
        return getHippoHtml(CONTENT);
    }

    /**
     * Get the location of the document.
     *
     * @return the location
     */
    public String getLocation() {
        return getProperty(LOCATION);
    }

    /**
     * Get the end date of the document, i.e. the end date of the event.
     *
     * @return the end date
     */
    public Calendar getEndDate() {
        return getProperty(END_DATE);
    }

}
