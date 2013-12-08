package org.onehippo.cms7.essentials.rest.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.Lists;

/**
 * @version "$Id: RestfulList.java 174870 2013-08-23 13:56:24Z mmilicevic $"
 */
@XmlRootElement(name = "collection")
public class RestfulList<T extends Restful> implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<T> items = Lists.newArrayList();

    private boolean sortByDate = true;

    private int totalSize;
    private int page = 1; // one by default
    private Date lastBuildDate;

    @XmlAttribute

    public int getTotalSize() {
        return totalSize;
    }


    public void setTotalSize(final int totalSize) {
        this.totalSize = totalSize;
    }

    @XmlAttribute

    public int getPage() {
        return page;
    }


    public void setPage(final int page) {
        this.page = page;
    }


    public void setLastBuildDate(Date date) {
        this.lastBuildDate = date;
    }

    @XmlAttribute(name = "date-built")

    public Date getLastBuildDate() {
        return lastBuildDate;
    }


    public void add(T resource) {
        items.add(resource);
    }


    public void addAll(List<T> items) {
        items.addAll(items);
    }

    public Iterator<T> iterator() {
        return items.iterator();
    }

    @XmlElementRefs({
            @XmlElementRef(type = PluginRestful.class),
            @XmlElementRef(type = PowerpackRestful.class),
            @XmlElementRef(type = VendorRestful.class),
            @XmlElementRef(type = StatusRestful.class),
            @XmlElementRef(type = DependencyRestful.class),
    })
    public List<T> getItems() {
        return items;
    }

    @XmlTransient
    public boolean isSortByDate() {
        return sortByDate;
    }


    public void setSortByDate(final boolean sortByDate) {
        this.sortByDate = sortByDate;
    }


}
