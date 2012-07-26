/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.layout;

import java.io.Serializable;


public class PageLayoutSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private YuiId rootId = new YuiId("doc3");

    private int headerHeight;
    private int footerHeight;
    private int leftWidth;
    private int rightWidth;

    private String headerGutter = "0px 0px 0px 0px";
    private String bodyGutter = "0px 0px 0px 0px";
    private String leftGutter = "0px 0px 0px 0px";
    private String rightGutter = "0px 0px 0px 0px";

    private boolean bodyScroll;
    private boolean headerScroll;
    private boolean footerScroll;
    private boolean leftScroll;
    private boolean rightScroll;
    
    private boolean headerResize;
    private boolean footerResize;
    private boolean leftResize;
    private boolean rightResize;

    public void setRootId(YuiId rootId) {
        this.rootId = rootId;
    }
    public YuiId getRootId() {
        return rootId;
    }
    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }
    public int getHeaderHeight() {
        return headerHeight;
    }
    public void setFooterHeight(int footerHeight) {
        this.footerHeight = footerHeight;
    }
    public int getFooterHeight() {
        return footerHeight;
    }
    public void setLeftWidth(int leftWidth) {
        this.leftWidth = leftWidth;
    }
    public int getLeftWidth() {
        return leftWidth;
    }
    public void setRightWidth(int rightWidth) {
        this.rightWidth = rightWidth;
    }
    public int getRightWidth() {
        return rightWidth;
    }
    public void setHeaderGutter(String headerGutter) {
        this.headerGutter = headerGutter;
    }
    public String getHeaderGutter() {
        return headerGutter;
    }
    public void setBodyGutter(String bodyGutter) {
        this.bodyGutter = bodyGutter;
    }
    public String getBodyGutter() {
        return bodyGutter;
    }
    public void setLeftGutter(String leftGutter) {
        this.leftGutter = leftGutter;
    }
    public String getLeftGutter() {
        return leftGutter;
    }
    public void setRightGutter(String rightGutter) {
        this.rightGutter = rightGutter;
    }
    public String getRightGutter() {
        return rightGutter;
    }
    public void setBodyScroll(boolean bodyScroll) {
        this.bodyScroll = bodyScroll;
    }
    public boolean isBodyScroll() {
        return bodyScroll;
    }
    public void setHeaderScroll(boolean headerScroll) {
        this.headerScroll = headerScroll;
    }
    public boolean isHeaderScroll() {
        return headerScroll;
    }
    public void setFooterScroll(boolean footerScroll) {
        this.footerScroll = footerScroll;
    }
    public boolean isFooterScroll() {
        return footerScroll;
    }
    public void setLeftScroll(boolean leftScroll) {
        this.leftScroll = leftScroll;
    }
    public boolean isLeftScroll() {
        return leftScroll;
    }
    public void setRightScroll(boolean rightScroll) {
        this.rightScroll = rightScroll;
    }
    public boolean isRightScroll() {
        return rightScroll;
    }
    public void setHeaderResize(boolean headerResize) {
        this.headerResize = headerResize;
    }
    public boolean isHeaderResize() {
        return headerResize;
    }
    public void setFooterResize(boolean footerResize) {
        this.footerResize = footerResize;
    }
    public boolean isFooterResize() {
        return footerResize;
    }
    public void setLeftResize(boolean leftResize) {
        this.leftResize = leftResize;
    }
    public boolean isLeftResize() {
        return leftResize;
    }
    public void setRightResize(boolean rightResize) {
        this.rightResize = rightResize;
    }
    public boolean isRightResize() {
        return rightResize;
    }

}
