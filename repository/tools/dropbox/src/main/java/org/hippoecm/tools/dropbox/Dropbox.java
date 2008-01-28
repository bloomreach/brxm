/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.dropbox;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

public class Dropbox {

    private String dropboxLocation;
    HippoRepository repo;

    SimpleCredentials cred;

    /* format yyyyMMddMMHHss = 20040124114558 */
    protected static final SimpleDateFormat fileDate = new SimpleDateFormat("yyyyMMddHHmmssZ");

    /*
     * Create a new Dropbox with dropbox as location to look for files
     */
    public Dropbox(String repoLoc, String dropbox) throws RepositoryException {
        this.dropboxLocation = dropbox;
        repo = HippoRepositoryFactory.getHippoRepository(repoLoc);
    }

    public Dropbox() throws RepositoryException {
        this("rmi://localhost:1099/hipporepository", "");
    }

    /*
     * Set the location of the dropbox
     */
    public void setDropbox(String location) {
        this.dropboxLocation = location;
    }

    public void setCredentials(SimpleCredentials cred) {
        this.cred = cred;
    }

    public SimpleCredentials getCredentials() {
        return cred;
    }

    /*
     * Get all the files from the dropbox location and save them to the
     * repository
     */
    public void drop(String relPath) throws RepositoryException {
        // make a session request to the repository
        Session session = repo.login(getCredentials());

        // Initialize facets
        initFacets(session);

        // put all the files in a node called dropbox
        Node root = session.getRootNode();
        if (root.hasNode(relPath)) {
            root = root.getNode(relPath);
        } else {
            root = root.addNode(relPath);
        }

        // Recusively walk through the dropbox directory and construct a JCR representation of
        // the files.
        File f = new File(dropboxLocation);
        try {
            dropFiles(f, session, root);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // leave the session
        session.logout();
        System.out.println("done!");
    }

    /*
     * Recusively append all files (except hidden files) located in the dropbox folder to the
     * JCR tree, while doing so it saves the changes to the repository.
     *
     * Note: - file names should not contain the following characters: ":"
     *           - node names encoded in ISO9075
     */
    private void dropFiles(File f, Session session, Node folder) throws RepositoryException, IOException {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(".") || files[i].getName().equals("..") || files[i].isHidden()) {
                continue;
            }
            if (files[i].isDirectory()) {

                //String nodeName = org.apache.jackrabbit.util.ISO9075.encode(files[i].getName());
                //nodeName = files[i].getName().replace(":", "_x003A_");

                String nodeName = ISO9075Helper.encodeLocalName(files[i].getName());

                if (folder.hasNode(nodeName)) {
                    dropFiles(files[i], session, folder.getNode(nodeName));
                } else {
                    dropFiles(files[i], session, folder.addNode(nodeName));
                }
                continue;
            }

            System.out.println("importing (" + files[i].getPath() + ") ...");
            createFile(folder, files[i]);
            session.save();
        }
    }

    protected Node createFile(Node folder, File f) throws RepositoryException {
        return createFile(folder, f, true);
    }

    /*
     * Creates a new file, node name encoded in ISO9075
     */
    protected Node createFile(Node folder, File f, boolean recreate) throws RepositoryException {
        FileDataSource ds = new FileDataSource(f);
        ds.setFileTypeMap(new MimetypesFileTypeMap(getClass().getResourceAsStream("mime.types")));

        String nodeName = ISO9075Helper.encodeLocalName(f.getName());
        
        if (folder.hasNode(nodeName)) {
            if (recreate) {
                folder.getNode(nodeName).remove();
                folder.addNode(nodeName, "hippo:document");
            }
        } else {
            folder.addNode(nodeName, "hippo:document");
        }
        Node n = folder.getNode(nodeName);
        
        n.setProperty("mimeType", ds.getContentType());
        n.setProperty("lastModified", fileDate.format(new Date(f.lastModified())));
        n.setProperty("filePath", f.getAbsolutePath());

        return n;
    }

    protected void initFacets(Session session) throws RepositoryException {
    }

    protected Node createFacet(Node navRoot, String docbase, String name, String[] facets) throws RepositoryException {
        Node nav = navRoot.addNode(name, HippoNodeType.NT_FACETSEARCH);
        nav.setProperty(HippoNodeType.HIPPO_QUERYNAME, name);
        nav.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        nav.setProperty(HippoNodeType.HIPPO_FACETS, facets);
        return nav;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 4) {
            usage();
        } else {
            try {
                Dropbox box = new Dropbox(args[0], args[1]);
                box.setCredentials(new SimpleCredentials(args[2], args[3].toCharArray()));
                box.drop("dropbox");
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    public static void usage() {
        System.err.println("Wrong number of arguments!");
        System.out.println("* Arguments: <repopath> <localpath> <username> <password>");
    }
}