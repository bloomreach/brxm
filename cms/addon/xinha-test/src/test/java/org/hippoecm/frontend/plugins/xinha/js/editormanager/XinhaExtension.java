package org.hippoecm.frontend.plugins.xinha.js.editormanager;

import java.io.Serializable;
import java.util.List;

public interface XinhaExtension extends Serializable {

    class ListEntry {
        private String key;
        private CharSequence value;

        public ListEntry(String key, CharSequence value) {
            this.key = key;
            this.value = value;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public CharSequence getValue() {
            return value;
        }
    }

    void populateProperties(List<ListEntry> properties);

}
