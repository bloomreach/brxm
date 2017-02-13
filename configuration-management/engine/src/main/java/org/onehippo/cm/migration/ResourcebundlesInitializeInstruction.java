package org.onehippo.cm.migration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;

public class ResourcebundlesInitializeInstruction extends InitializeInstruction {

    public ResourcebundlesInitializeInstruction(final EsvNode instructionNode, final Type type,
                                                final InitializeInstruction combinedWith) throws EsvParseException {
        super(instructionNode, type, combinedWith);
    }

    public void processResourceBundles(ModuleImpl module) throws IOException, EsvParseException {
        JSONObject json = new JSONObject(IOUtils.toString(new FileInputStream(getResource()), "UTF-8"));
        parse(json, new Stack<>(), module.addSource(getSourcePath()));
    }

    private void parse(final JSONObject json, final Stack<String> path, SourceImpl source) throws EsvParseException {
        for (String key : json.keySet()) {
            if (!(json.get(key) instanceof JSONObject)) {
                throw new EsvParseException("Invalid resourcebundle: "+getResourcePath()+". Expected json object");
            }
            JSONObject map = (JSONObject)json.get(key);
            if (map.length() > 0) {
                Iterator<String> iterator = map.keys();
                String mapKey = iterator.next();
                if (map.get(mapKey) instanceof String) {
                    final String bundleName = buildName(path);
                    final ConfigDefinitionImpl def = source.addConfigDefinition();
                    final DefinitionNodeImpl node = new DefinitionNodeImpl(bundleName, path.peek(), def);
                    def.setNode(node);
                    DefinitionNodeImpl localeNode = node.addNode(key);
                    localeNode.addProperty(mapKey, new ValueImpl((String)map.get(mapKey)));
                    while (iterator.hasNext()) {
                        mapKey = iterator.next();
                        localeNode.addProperty(mapKey, new ValueImpl((String)map.get(mapKey)));
                    }
                }
                else {
                    path.push(key);
                    parse(map, path, source);
                    path.pop();
                }
            }
        }
    }

    private static String buildName(Stack<String> path) {
        final StringBuilder sb = new StringBuilder("/hippo:configuration/hippo:translations/");
        final Iterator<String> iterator = path.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append("/");
            }
        }
        return sb.toString();
    }
}
