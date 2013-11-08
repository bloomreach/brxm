package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Calendar;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
//todo annotation based instead of string type return
@JcrWikiNode(nodeType = "mytestproject:newsdocument")
public class NewsWikiStrategy extends DefaultStrategy {

    private static Logger log = LoggerFactory.getLogger(NewsWikiStrategy.class);

    protected NewsWikiStrategy(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean onText(final Node doc, final Node currentSubFolder, final String text) {
        try {
            //set body with images
            final Node body = getOrAddNode(doc, "mytestproject:body", "hippostd:html");
            final String textBody = createImages(doc, currentSubFolder, body, text);
            body.setProperty("hippostd:content", textBody.trim());

            return true;
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    @Override
    public boolean onTimeStamp(final Node doc, final Node currentSubFolder, final Calendar timestamp) {
        try {
            //set property with timestamp
            doc.setProperty("mytestproject:date", timestamp);
            return true;
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    @Override
    public void onUserName(final Node doc, final Node currentSubFolder, final String username) {

    }

    @Override
    public void onTitle(final Node doc, final Node currentSubFolder, final String docTitle) {
        try {
            doc.setProperty("mytestproject:title", docTitle);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    @Override
    public String getType() {
        return "mytestproject:newsdocument";
    }


}
