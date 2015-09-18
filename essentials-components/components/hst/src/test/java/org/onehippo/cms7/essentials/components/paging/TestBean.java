package org.onehippo.cms7.essentials.components.paging;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoItem;

@XmlRootElement(name = "TestBean")
@XmlAccessorType(XmlAccessType.NONE)
public class TestBean extends HippoItem {

    @XmlElement
    public String getTitle() {
        return "testTitle";
    }

}
