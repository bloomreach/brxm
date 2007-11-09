package org.hippoecm.tools.dropbox;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

public class DropboxExt extends Dropbox {
    
    public DropboxExt(String repoLoc, String dropbox) throws RepositoryException {
        super(repoLoc, dropbox);
    }

    protected Node createFile(Node folder, File f) throws RepositoryException {
        Node n = super.createFile(folder, f);
        Property prop = n.getProperty("mimeType");
        String mimeType = prop.getString();

        if (mimeType.equals("image/png") || mimeType.equals("image/jpg")) {
            n.setProperty("DocType", "picture");
        } else if (mimeType.equals("application/msword") || mimeType.equals("application/vnd.oasis.opendocument.text")
                || mimeType.equals("application/pdf")) {
            n.setProperty("DocType", "document");
        } else if (mimeType.equals("text/html")) {
            n.setProperty("DocType", "webpage");
        } else if (mimeType.equals("text/plain")) {
            n.setProperty("DocType", "text");
        }
        return n;
    }

    public static void main(String[] args) {
        try {
            DropboxExt box = new DropboxExt(args[0], args[1]);
            box.setCredentials(new SimpleCredentials(args[2], args[3].toCharArray()));
            box.drop();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
}