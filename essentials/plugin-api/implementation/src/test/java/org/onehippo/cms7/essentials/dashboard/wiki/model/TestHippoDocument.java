package org.onehippo.cms7.essentials.dashboard.wiki.model;

import java.util.List;

import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * [hippo:document] > nt:base
 * - hippo:availability (string) multiple ignore
 *
 * @version "$Id$"
 */
@JcrNode(nodeType = "hippo:document")
public abstract class TestHippoDocument extends TestHippoNode {

    private static Logger log = LoggerFactory.getLogger(TestHippoDocument.class);

    //- hippo:availability (string) multiple ignore
//    @JcrProperty(name = "hippo:availability")
//    private List<String> availability;


}
