package org.hippoecm.hst.demo.beans;

import java.util.List;

import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.ContentBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;

public class GoGreenProductBean implements IdentifiableContentBean {

    private String identifier;
    private String title;
    private String summary;
    private String description;

    private Double price;
    private String[] categories;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    @IndexField
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @IndexField
    public String getSummary() {
        return summary ;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @IndexField
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @IndexField
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }


    @IndexField
    public String[] getCategories() {
        return categories;
    }
    public void setCategories(String[] categories) {
        this.categories = categories;
    }

}
