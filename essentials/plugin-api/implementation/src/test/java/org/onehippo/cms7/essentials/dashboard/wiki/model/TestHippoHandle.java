package org.onehippo.cms7.essentials.dashboard.wiki.model;

import java.util.List;

import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * [hippo:handle] > nt:base
 - hippo:discriminator (string) multiple
 + * (hippo:document) = hippo:document multiple version
 + hippo:request (hippo:request) multiple ignore
 * @version "$Id$"
 */
@JcrNode(nodeType = "hippo:handle")
public class TestHippoHandle extends TestHippoNode {

    private static Logger log = LoggerFactory.getLogger(TestHippoHandle.class);

    // + * (hippo:document) = hippo:document multiple version
    @JcrChildNode(name = "hippo:document")
    private List<TestHippoDocument> documents;

    public List<TestHippoDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(final List<TestHippoDocument> documents) {
        this.documents = documents;
    }
}
