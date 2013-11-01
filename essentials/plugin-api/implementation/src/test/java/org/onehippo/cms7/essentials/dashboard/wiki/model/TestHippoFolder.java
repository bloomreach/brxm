package org.onehippo.cms7.essentials.dashboard.wiki.model;

import java.util.ArrayList;
import java.util.List;

import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * [hippostd:folder] > hippo:document orderable
 - hippostd:foldertype (string) multiple copy
 + * = hippostd:folder multiple
 *
 * @version "$Id$"
 */
@JcrNode(nodeType = "hippostd:folder"/*, mixinTypes = {"hippo:harddocument", "hippotranslation:translated", "hippo:translated"}*/)
public class TestHippoFolder extends TestHippoDocument {

    private static Logger log = LoggerFactory.getLogger(TestHippoFolder.class);

    @JcrProperty(name = "hippostd:foldertype")
    private List<String> foldertype;

    @JcrChildNode(createContainerNode = false, containerNodeType = "hippostd:folder")//(createContainerNode = "hippostd:folder")
    private List<TestHippoFolder> folders;

    @JcrChildNode//(name = "hippo:handle")
    private List<TestHippoHandle> handles;

    public List<String> getFoldertype() {
        return foldertype;
    }

    public void setFoldertype(final List<String> foldertype) {
        this.foldertype = foldertype;
    }

    public List<TestHippoFolder> getFolders() {
        return folders;
    }

    public void setFolders(final List<TestHippoFolder> folders) {
        this.folders = folders;
    }

    public List<TestHippoHandle> getHandles() {
        return handles;
    }

    public void setHandles(final List<TestHippoHandle> handles) {
        this.handles = handles;
    }

    public boolean add(final TestHippoFolder testHippoFolder) {
        if(folders == null){
            folders = new ArrayList<>();
        }
        return folders.add(testHippoFolder);
    }
}
