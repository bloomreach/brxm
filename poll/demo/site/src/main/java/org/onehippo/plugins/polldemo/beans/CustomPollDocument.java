package org.onehippo.plugins.polldemo.beans;

import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

import org.onehippo.forge.poll.contentbean.compound.Poll;

/** 
 *
 */
@HippoEssentialsGenerated(internalName = "polldemo:customPollDocument")
@Node(jcrType = "polldemo:customPollDocument")
public class CustomPollDocument extends BaseDocument {
    @HippoEssentialsGenerated(internalName = "polldemo:title")
    public String getTitle() {
        return getProperty("polldemo:title");
    }

    public Poll getPoll() {
        return getBean("polldemo:poll");
    }
}
