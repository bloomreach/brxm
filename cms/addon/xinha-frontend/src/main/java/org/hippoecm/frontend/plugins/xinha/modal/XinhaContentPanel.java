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
package org.hippoecm.frontend.plugins.xinha.modal;

import java.util.EnumMap;
import java.util.Iterator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.yui.util.JavascriptUtil;

public abstract class XinhaContentPanel<K extends Enum<K>> extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final protected Form form;
    final protected EnumMap<K, String> values;
    final protected AjaxButton ok;
    final protected AjaxButton cancel;
    final protected FeedbackPanel feedback;
    final protected JcrNodeModel nodeModel;
    final protected XinhaModalWindow modal;

    public XinhaContentPanel(final XinhaModalWindow modal, final JcrNodeModel nodeModel, final EnumMap<K, String> values) {
        super(modal.getContentId());
        this.values = values;
        this.nodeModel = nodeModel;
        this.modal = modal;
        
        add(form = new Form("form"));

        ok = new AjaxButton("ok", form) {
            private static final long serialVersionUID = 1L;

            protected void onSubmit(AjaxRequestTarget target, Form form) {
                onOk();
                modal.onSelect(target, getSelectedValue());
            }
        };

        form.add(ok);

        form.add(cancel = new AjaxButton("close", form) {
            private static final long serialVersionUID = 1L;

            public void onSubmit(AjaxRequestTarget target, Form form) {
                onCancel();
                modal.onCancel(target);
            }
        });
        //TODO: feedback is written in the page feedbackpanel, not this one in the modalwindow
        form.add(feedback = new FeedbackPanel("feedback2"));
        feedback.setOutputMarkupId(true);
        
        form.add(new EmptyPanel("extraButtons"));
    }
    
    protected EnumModel<K> newEnumModel(Enum<K> e) {
        return new EnumModel<K>(values, e);
    }

    protected String getSelectedValue() {
        StringBuilder sb = new StringBuilder(values.size() * 20);
        sb.append('{');

        Iterator<K> it = values.keySet().iterator();
        while (it.hasNext()) {
            K key = it.next();
            sb.append(getXinhaParameterName(key)).append(": ").append(JavascriptUtil.serialize2JS(values.get(key)));
            if (it.hasNext())
                sb.append(',');
        }
        sb.append('}');

        return sb.toString();
    }

    protected void onOk() {
    }

    protected void onCancel() {
    }

    /**
     * Return the Xinha parameter name encoded in enumeration K
     * @param k
     * @return
     */
    abstract protected String getXinhaParameterName(K k);

}

class EnumModel<K extends Enum<K>> implements IModel {
    private static final long serialVersionUID = 1L;

    private EnumMap<K, String> values;
    private Enum<K> e;

    public EnumModel(EnumMap<K, String> values, Enum<K> e) {
        this.e = e;
        this.values = values;
    }

    public Object getObject() {
        return values.get(e);
    }

    @SuppressWarnings("unchecked")
    public void setObject(Object object) {
        if (object == null)
            return;
        values.put((K) e, (String) object);
    }

    public void detach() {
    }
}
