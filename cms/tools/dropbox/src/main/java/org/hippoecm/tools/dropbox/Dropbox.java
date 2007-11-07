package org.hippoecm.tools.dropbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class Dropbox
{
	private String dropboxLocation;
	private HippoRepository repo;
	
	private SimpleCredentials cred;
	
	/*
	 * Create a new Dropbox with dropbox as location to look for files
	 */
	public Dropbox(String repoLoc, String dropbox) throws RepositoryException
	{
		this.dropboxLocation = dropbox;
		repo = (HippoRepository) HippoRepositoryFactory.getHippoRepository(repoLoc);
	}
	
	public Dropbox() throws RepositoryException {
		this("rmi://localhost:1099/jackrabbit.repository", "");
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
	public void drop() throws RepositoryException {
		// make a session request to the repository
		Session session = repo.login(getCredentials());
		
		Node root = session.getRootNode();
		
		// put all the files in a node called dropbox
		if(root.hasNode("dropbox")) {
			root = root.getNode("dropbox");
		} else {
			root = root.addNode("dropbox");
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
	 * 		 - node names encoded in ISO9075
	 */
	private void dropFiles(File f, Session session, Node folder) throws RepositoryException, IOException {
		File[] files = f.listFiles();
		
		for(int i=0;i<files.length;i++) {
			
			if(files[i].getName().equals(".") || 
					files[i].getName().equals("..") ||
					files[i].isHidden()) {
				continue;
			}
			
			if(files[i].isDirectory()) {
				dropFiles(files[i], session, folder.addNode(files[i].getName()));
				continue;
			}
			
			System.out.println("importing (" + files[i].getName() + ") ...");
			
			try {
				createFile(folder, files[i]);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			session.save();
		}
	}
	
	/*
	 * Creates a new file, node name encoded in ISO9075
	 */
	protected Node createFile(Node folder, File f) throws IOException, RepositoryException {
		FileDataSource ds = new FileDataSource(f);
		ds.setFileTypeMap(new MimetypesFileTypeMap(getClass().getResourceAsStream("mime.types")));
		
		Node n = folder.addNode( org.apache.jackrabbit.util.ISO9075.encode(f.getName()), "hippo:document");
		n.setProperty("mimeType", ds.getContentType());	
		n.setProperty("lastModified", f.lastModified());
		n.setProperty("path", f.getAbsolutePath());
		
		return n;
	}
	
	public static void main(String[] args) {
		try {
			Dropbox box = new Dropbox(args[0], args[1]);
			box.setCredentials(new SimpleCredentials(args[2], args[3].toCharArray()));
			box.drop();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}
}