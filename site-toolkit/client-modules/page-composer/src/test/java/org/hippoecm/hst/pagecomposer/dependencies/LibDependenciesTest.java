package org.hippoecm.hst.pagecomposer.dependencies;

import org.hippoecm.hst.pagecomposer.dependencies.ext.ExtCore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class LibDependenciesTest {

    @Test
    public void extTest() throws Exception {
        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);

        DependencyManager manager = new DependencyManager();
        manager.add(new ExtCore("3.3.0"));
        manager.write(writer);
        
        assertEquals(4, results.size());
        assertEquals("<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"lib/ext/core/3.3.0/resources/css/ext-all.css\"/>", results.get(0));
        assertEquals("<script type=\"text/javascript\" src=\"lib/ext/core/3.3.0/adapter/ext/ext-base.js\"></script>", results.get(1));
        assertEquals("<script type=\"text/javascript\" src=\"lib/ext/core/3.3.0/ext-all.js\"></script>", results.get(2));
        assertEquals("<script type=\"text/javascript\">Ext.BLANK_IMAGE_URL = 'lib/ext/core/3.3.0/resources/images/default/s.gif';</script>", results.get(3));
    }
}
