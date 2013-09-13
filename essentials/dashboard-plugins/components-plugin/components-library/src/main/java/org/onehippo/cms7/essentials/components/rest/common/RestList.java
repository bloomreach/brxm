package org.onehippo.cms7.essentials.components.rest.common;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * @version "$Id: RestList.java 174726 2013-08-22 14:24:50Z mmilicevic $"
 */
@XmlTransient
public interface RestList<T extends Restful<? extends HippoBean>> {

    int getTotalSize();

    void setTotalSize(int size);

    int getPage();

    void setPage(int size);

    Date getLastBuildDate();

    void setLastBuildDate(Date date);

    void add(T resource);

    void addAll(List<T> items);

    Iterator<T> iterator();

    List<T> getItems();

    void setSortByDate(boolean byDate);

}