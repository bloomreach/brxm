package org.hippoecm.frontend.plugins.standards;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

public class NamespaceFriendlyChoiceRenderer implements IChoiceRenderer {
    private static final long serialVersionUID = 1L;
    
    private List<String> doubles = new ArrayList<String>();
    
    public NamespaceFriendlyChoiceRenderer(List<String> choices) {
        List<String> all = new ArrayList<String>();
        for(String choice : choices) {
            String name = choice.substring(choice.indexOf(':')+1);
            if(all.contains(name)) {
                if(!doubles.contains(name)) doubles.add(name);
            } else {
                all.add(name);
            }
        }
    }
    
    public Object getDisplayValue(Object object) {
        String input = object.toString();
        String displayValue = input;
        int semicolon = input.indexOf(':');
        displayValue = input.substring(semicolon + 1);
        if(doubles.contains(displayValue)) {
            String namespace = input.substring(0, semicolon);
            return displayValue + " (" + namespace + ")";
        }
        return displayValue;
    }

    public String getIdValue(Object object, int index) {
        return object.toString();
    }

}

