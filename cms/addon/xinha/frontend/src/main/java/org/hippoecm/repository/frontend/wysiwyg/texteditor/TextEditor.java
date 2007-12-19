package org.hippoecm.repository.frontend.wysiwyg.texteditor;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextArea;
import org.hippoecm.repository.frontend.wysiwyg.HtmlEditor;

public class TextEditor extends HtmlEditor {
    private static final long serialVersionUID = 1L;

    public TextEditor(final String id) {
        super(id);
        setRenderBodyOnly(true);

        final TextArea editor = new TextArea("editor", getModel());
        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        editor.add(new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                final String saveCall = "{wicketAjaxGet('" + getCallbackUrl()
                        + "&save=true&'+this.name+'='+wicketEncode(this.value)); return false;}";

                System.out.println(saveCall);

                tag.put("onblur", saveCall);
            }

            protected void respond(AjaxRequestTarget target) {
                RequestCycle requestCycle = RequestCycle.get();
                boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save")).booleanValue();

                if (save) {
                    editor.processInput();

                    System.out.println("editor value: " + editor.getValue());
                    System.out.println("editor contents: " + TextEditor.this.getContent());

                    if (editor.isValid()) {

                    }
                }
            }
        });

        add(editor);
    }

    public void init() {
    }

    //    @Override
    //    protected void onRender(MarkupStream markupStream) {
    //        // TODO Auto-generated method stub
    //        super.onRender(markupStream);
    //        Iterator it = markupStream.componentTagIterator();
    //        System.out.println("ITERATE COMPONENTS");
    //        while(it.hasNext()) {
    //            System.out.println(it.next());
    //        }
    //        System.out.println("ITERATE END");
    //   
    ////        System.out.println("render");
    ////        System.out.println("RENDER STREAM");
    ////        System.out.println(markupStream.toHtmlDebugString());
    //    }
    //    
    //    protected void onComponentTag(ComponentTag tag)
    //    {
    //        super.onComponentTag(tag);
    //        System.out.println("tag: " + tag);
    //    }
    //    
    //    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
    //    {
    //        super.onComponentTagBody(markupStream, openTag);
    //        System.out.println("BODY GENERATE");
    //        Iterator it = markupStream.componentTagIterator();
    //        System.out.println("ITERATE COMPONENTS in BODY");
    //        while(it.hasNext()) {
    //            System.out.println(it.next());
    //        }
    //        System.out.println("ITERATE END");
    //        /*
    //        if (getModelObject() == null)
    //        {
    //            replaceComponentTagBody(markupStream, openTag, defaultNullLabel());
    //        }
    //        else
    //        {
    //            super.onComponentTagBody(markupStream, openTag);
    //        }
    //        */
    //    }

}