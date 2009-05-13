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
package org.hippoecm.frontend.dialog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copied from https://issues.apache.org/jira/browse/WICKET-12
 * Code by Maurice Marrink to allow a modal window to render on a non-ajax request.
 */
public class PersistentModalWindow extends ModalWindow {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PersistentModalWindow.class);

    private transient Field showField;

    private boolean renderScript = false;

    /**
    * @param id
    */
    @SuppressWarnings("unchecked")
    public PersistentModalWindow(String id) {
        super(id);
        initShowField();
    }

    /**
    *
    */
    private void initShowField() {
        try {
            showField = ModalWindow.class.getDeclaredField("shown");
            showField.setAccessible(true);
        } catch (SecurityException e) {
            log.error(e.getMessage(), e);
        } catch (NoSuchFieldException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
    * @see org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow#onBeforeRender()
    */
    @Override
    protected void onBeforeRender() {
        boolean shown = isShown();
        super.onBeforeRender();
        // fix stupid parent behavior for non ajax requests
        if (shown && !isShown()) {
            show();
            get(getContentId()).setVisible(true);
            renderScript = true;
        }
    }

    /**
    * @see org.apache.wicket.markup.html.panel.Panel#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
    */
    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        // hack to open modal window in non ajax requests too
        if (renderScript) {
            try {
                Method m = ModalWindow.class.getDeclaredMethod("getWindowOpenJavascript");
                m.setAccessible(true);
                String script = (String) m.invoke(this);
                // hack to disable onclose warning for just this window
                container.getHeaderResponse().renderOnDomReadyJavascript(script);
            } catch (SecurityException e) {
                log.error(e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                log.error(e.getMessage(), e);
            }
            renderScript = false;
        }
    }

    /**
    * shows this modaldialog.
    */
    protected void show() {
        try {
            if (showField == null)
                initShowField();
            showField.set(this, true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }
}