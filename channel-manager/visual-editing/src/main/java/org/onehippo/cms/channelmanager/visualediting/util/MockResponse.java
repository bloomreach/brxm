/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.visualediting.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms.channelmanager.visualediting.model.Document;
import org.onehippo.cms.channelmanager.visualediting.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.visualediting.model.FieldTypeSpec;

/**
 * This class temporarily provides the front-end with mock responses of the visual editing REST API.
 * Once the back-end has been fully implemented, this class should be deleted.
 */
public class MockResponse {
    private static final String DOCUMENT_TYPE = "ns:testdocument";
    private MockResponse() {}

    public static Document createTestDocument(final String id) {
        final Document doc = new Document();

        doc.setId(id);
        doc.setType(DOCUMENT_TYPE);
        doc.setDisplayName("Tobi's test document");

        // String
        doc.addField("ns:string", "Lorem ipsum dolor sit amet");
        doc.addField("ns:multiplestring", Arrays.asList(
                "consectetur adipiscing elit",
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
                "Ut enim ad minim veniam"
        ));
        doc.addField("ns:requiredstring",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat");

        // Multi-line string
        doc.addField("ns:multilinestring",
                "Duis aute irure dolor in reprehenderit\nin voluptate velit esse cillum dolore eu fugiat nulla pariatur");
        doc.addField("ns:requiredmultiplemultilinestring", Arrays.asList(
                "", // invalid
                "Excepteur sint occaecat cupidatat non proident",
                "sunt in culpa qui officia deserunt\nmollit anim id est laborum"
        ));

        // Choice
        Map<String, Object> children = new HashMap<>();
        children.put("ns:secondchoice", Arrays.asList(
                "Sed ut perspiciatis unde omnis iste natus error sit",
                "voluptatem accusantium doloremque laudantium"
        ));
        doc.addField("ns:singlechoice", children);

        children = new HashMap<>();
        Map<String, Object> grandChildren = new HashMap<>();
        grandChildren.put("ns:firstchild", "");
        grandChildren.put("ns:secondchild", "totam rem aperiam");
        children.put("ns:thirdchoice", grandChildren);
        children.put("ns:firstchoice",
                "eaque ipsa quae ab illo inventore\nveritatis et quasi architecto beatae vitae dicta sunt explicabo");
        grandChildren = new HashMap<>();
        grandChildren.put("ns:secondchoice", "Nemo enim ipsam voluptatem quia voluptas sit");
        children.put("ns:secondchoice", grandChildren);
        doc.addField("ns:multiplechoice", children);

        // Compound
        children = new HashMap<>();
        children.put("ns:firstchild", Arrays.asList());
        children.put("ns:secondchild",
                "aspernatur aut odit aut fugit\nsed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt");
        doc.addField("ns:onelevelnesting", children);

        children = new HashMap<>();
        children.put("ns:firstchild", "Neque porro quisquam est");
        grandChildren = new HashMap<>();
        grandChildren.put("ns:secondgrandchild", Arrays.asList(
                "qui dolorem ipsum quia dolor sit amet"
        ));
        children.put("ns:thirdchild", grandChildren);
        Map<String, Object> moreChildren = new HashMap<>();
        moreChildren.put("ns:firstchild", "");
        grandChildren = new HashMap<>();
        grandChildren.put("ns:firstgrandchild", Arrays.asList(
                "consectetur, adipisci velit",
                "sed quia non numquam eius modi tempora incidunt ut labore"
        ));
        grandChildren.put("ns:secondgrandchild", "et dolore magnam aliquam quaerat voluptatem");
        Map<String, Object> moreGrandChildren = new HashMap<>();
        moreGrandChildren.put("ns:secondgrandchild",
                "Ut enim ad minima veniam\nquis nostrum exercitationem ullam corporis suscipit laboriosam");
        moreChildren.put("ns:secondchild", Arrays.asList(
                grandChildren,
                moreGrandChildren
        ));
        grandChildren = new HashMap<>();
        grandChildren.put("ns:firstgrandchild", "nisi ut aliquid ex ea commodi consequatur");
        moreChildren.put("ns:thirdchild", grandChildren);
        doc.addField("ns:twolevelnesting", Arrays.asList(
                children,
                moreChildren
        ));

        return doc;
    }

    public static DocumentTypeSpec createTestDocumentType() {
        final DocumentTypeSpec docType = new DocumentTypeSpec();

        docType.setId(DOCUMENT_TYPE);
        docType.setDisplayName("Tobi's test document type");

        // String fields

        FieldTypeSpec field = new FieldTypeSpec();
        field.setId("ns:string");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setCaption("Simple string");
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("ns:multiplestring");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setCaption("Multiple string");
        field.setMultiple(true);
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("ns:requiredstring");
        field.setType(FieldTypeSpec.Type.STRING);
        field.setCaption("Required string");
        field.addValidator(FieldTypeSpec.Validator.REQUIRED);
        docType.addField(field);

        // Multi-line string fields

        field = new FieldTypeSpec();
        field.setId("ns:multilinestring");
        field.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        field.setCaption("Multi-line string");
        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("ns:requiredmultiplemultilinestring");
        field.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        field.setCaption("Required multiple multi-line string");
        field.setMultiple(true);
        field.addValidator(FieldTypeSpec.Validator.REQUIRED);
        docType.addField(field);

        // Choice ("content blocks") fields

        field = new FieldTypeSpec();
        field.setId("ns:singlechoice");
        field.setType(FieldTypeSpec.Type.CHOICE);
        field.setCaption("Single choice");

        FieldTypeSpec child = new FieldTypeSpec();
        child.setId("ns:firstchoice");
        child.setType(FieldTypeSpec.Type.STRING);
        child.setCaption("First Choice: simple string");
        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:secondchoice");
        child.setType(FieldTypeSpec.Type.STRING);
        child.setCaption("Second Choice: multiple string");
        child.setMultiple(true);
        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:thirdchoice");
        child.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        child.setCaption("Third Choice: Multi-line required string");
        child.setMultiple(true);
        child.addValidator(FieldTypeSpec.Validator.REQUIRED);
        field.addField(child);

        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("ns:multiplechoice");
        field.setType(FieldTypeSpec.Type.CHOICE);
        field.setCaption("Multiple choice");
        field.setMultiple(true);

        child = new FieldTypeSpec();
        child.setId("ns:firstchoice");
        child.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        child.setCaption("First Choice: multi-line string");
        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:secondchoice");
        child.setType(FieldTypeSpec.Type.CHOICE);
        child.setCaption("Second Choice: sub-choice");

        FieldTypeSpec grandChild = new FieldTypeSpec();
        grandChild.setId("ns:firstchoice");
        grandChild.setType(FieldTypeSpec.Type.STRING);
        grandChild.setCaption("First sub-choice");
        grandChild.setMultiple(true);
        child.addField(grandChild);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:secondchoice");
        grandChild.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        grandChild.setCaption("Second sub-choice");
        child.addField(grandChild);

        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:thirdchoice");
        child.setType(FieldTypeSpec.Type.COMPOUND);
        child.setCaption("Third Choice: compound");
        child.setMultiple(true);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:firstchild");
        grandChild.setType(FieldTypeSpec.Type.STRING);
        grandChild.setCaption("First sub-child");
        child.addField(grandChild);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:secondchild");
        grandChild.setType(FieldTypeSpec.Type.STRING);
        grandChild.setCaption("Second sub-child");
        grandChild.addValidator(FieldTypeSpec.Validator.REQUIRED);
        child.addField(grandChild);

        field.addField(child);

        docType.addField(field);

        // Compound fields

        field = new FieldTypeSpec();
        field.setId("ns:onelevelnesting");
        field.setType(FieldTypeSpec.Type.COMPOUND);
        field.setCaption("One level of nesting");
        field.setMultiple(true);

        child = new FieldTypeSpec();
        child.setId("ns:firstchild");
        child.setType(FieldTypeSpec.Type.STRING);
        child.setCaption("First child");
        child.setMultiple(true);
        child.addValidator(FieldTypeSpec.Validator.REQUIRED);
        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:secondchild");
        child.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        child.setCaption("Second child");
        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:thirdchild");
        child.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        child.setCaption("Third child");
        child.setMultiple(true);
        field.addField(child);

        docType.addField(field);

        field = new FieldTypeSpec();
        field.setId("ns:twolevelnesting");
        field.setType(FieldTypeSpec.Type.COMPOUND);
        field.setCaption("Two levels of nesting");
        field.setMultiple(true);

        child = new FieldTypeSpec();
        child.setId("ns:firstchild");
        child.setType(FieldTypeSpec.Type.STRING);
        child.setCaption("First child");
        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:secondchild");
        child.setType(FieldTypeSpec.Type.COMPOUND);
        child.setCaption("Second child");
        child.setMultiple(true);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:firstgrandchild");
        grandChild.setType(FieldTypeSpec.Type.STRING);
        grandChild.setCaption("First grand-child");
        grandChild.setMultiple(true);
        child.addField(grandChild);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:secondgrandchild");
        grandChild.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        grandChild.setCaption("Second grand-child");
        grandChild.addValidator(FieldTypeSpec.Validator.REQUIRED);
        child.addField(grandChild);

        field.addField(child);

        child = new FieldTypeSpec();
        child.setId("ns:thirdchild");
        child.setType(FieldTypeSpec.Type.CHOICE);
        child.setCaption("Third child");

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:firstgrandchild");
        grandChild.setType(FieldTypeSpec.Type.STRING);
        grandChild.setCaption("First grand-child");
        child.addField(grandChild);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:secondgrandchild");
        grandChild.setType(FieldTypeSpec.Type.STRING);
        grandChild.setCaption("Second grand-child");
        grandChild.setMultiple(true);
        grandChild.addValidator(FieldTypeSpec.Validator.REQUIRED);
        child.addField(grandChild);

        grandChild = new FieldTypeSpec();
        grandChild.setId("ns:thirdgrandchild");
        grandChild.setType(FieldTypeSpec.Type.MULTILINE_STRING);
        grandChild.setCaption("Third grand-child");
        child.addField(grandChild);

        field.addField(child);

        docType.addField(field);

        return docType;
    }
}
