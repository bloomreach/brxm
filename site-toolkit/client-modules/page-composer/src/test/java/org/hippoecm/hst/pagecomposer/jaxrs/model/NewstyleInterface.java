package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Date;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

interface NewstyleInterface {
    @Parameter(name = "00-color", defaultValue = "blue")
    @Color
    String getColor();

    @Parameter(name = "01-documentLocation")
    @DocumentLink(docLocation = "/content", docType = "hst:testdocument")
    String getDocumentLocation();

    @Parameter(name = "02-image", defaultValue = "/content/gallery/default.png")
    @ImageSetPath
    String getImage();

    @Parameter(name = "03-date")
    Date getDate();

    @Parameter(name = "04-boolean")
    boolean isBoolean();

    @Parameter(name = "05-booleanClass")
    Boolean isBooleanClass();

    @Parameter(name = "06-int")
    int getInt();

    @Parameter(name = "07-integerClass")
    Integer getIntegerClass();

    @Parameter(name = "08-long")
    long getLong();

    @Parameter(name = "09-longClass")
    Long getLongClass();

    @Parameter(name = "10-short")
    short getShort();

    @Parameter(name = "11-shortClass")
    Short getShortClass();

    @Parameter(name = "12-jcrpath")
    @JcrPath(pickerInitialPath = "/content/documents/subdir/foo")
    String getJcrPath();

    @Parameter(name = "13-relativejcrpath")
    @JcrPath(isRelative = true, pickerInitialPath = "subdir/foo", pickerConfiguration = "cms-pickers/mycustompicker")
    String getRelativeJcrPath();

    @Parameter(name = "14-dropdownvalue")
    @DropDownList(value = {"value1", "value2", "value3"})
    String getDropDownValue();
}
