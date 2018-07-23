/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.components.folder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class JsonT9Tree extends T9Tree {

    private final T9Node root;
    private Map<String, T9Node> nodes = new HashMap<String, T9Node>();
    private Map<String, List<T9Node>> children = new HashMap<String, List<T9Node>>();
    private Map<String, List<T9Node>> translations = new HashMap<String, List<T9Node>>();

    public JsonT9Tree() {
        try (InputStream is = getClass().getResourceAsStream("folder-translation-nodes.json")) {
            JSONTokener tokener = new JSONTokener(new InputStreamReader(is));
            JSONArray data = new JSONArray(tokener);
            this.root = new T9Node("root");
            this.nodes.put(root.getId(), root);
            process(getRoot(), data);
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Could not read repository data.", e);
        }
    }

    private void process(T9Node parent, JSONArray jsonChildren) throws JSONException {
        List<T9Node> childNodes = new LinkedList<T9Node>();
        for (int i = 0; i < jsonChildren.length(); i++) {
            JSONObject object = jsonChildren.getJSONObject(i);
            T9Node node = createT9Node(parent, object);
            nodes.put(node.getId(), node);
            childNodes.add(node);
            if (node.getT9id() != null) {
                List<T9Node> siblings;
                if (translations.containsKey(node.getT9id())) {
                    siblings = translations.get(node.getT9id());
                } else {
                    siblings = new LinkedList<T9Node>();
                    translations.put(node.getT9id(), siblings);
                }
                siblings.add(node);
            }
            if (object.has("children")) {
                process(node, object.getJSONArray("children"));
            } else {
                process(node, new JSONArray());
            }
        }
        children.put(parent.getId(), childNodes);
    }

    private static T9Node createT9Node(T9Node parent, JSONObject json) throws JSONException {
        T9Node node = new T9Node(parent, json.getString("id"));
        if (json.has("t9id")) {
            node.setT9id(json.getString("t9id"));
            node.setLang(json.getString("lang"));
        }
        node.setName(json.getString("text"));

        return node;
    }

    @Override
    public T9Node getRoot() {
        return root;
    }

    @Override
    public List<T9Node> getChildren(String nodeId) {
        return children.get(nodeId);
    }

    @Override
    public List<T9Node> getSiblings(String t9Id) {
        return translations.get(t9Id);
    }

    @Override
    public T9Node getNode(String id) {
        return nodes.get(id);
    }

}
