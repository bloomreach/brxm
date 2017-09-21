/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import java.util.Locale;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.junit.Test;
import org.onehippo.taxonomy.plugin.AbstractTaxonomyTest;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;

import static org.junit.Assert.assertEquals;

public class TaxonomyHelperTest extends AbstractTaxonomyTest {

   /*
    * tests cases:
    *
    *  category      | document    | expected
    *  -------------------------------------------------------------------------
    *  "en"          | "en"        | name = translated name from info node
    *  "en"          | "fr"        | name = category name
    *  "en"          | "en-GB"     | name = translated name
    *                |             |
    *  "en-GB"       | "en"        | name = category name
    *                |             |
    *  "en", "en-GB" | "en"        | name = translated name from en info node
    *  "en", "en-GB" | "en-GB"     | name = translated name from en-GB info node
    *
    * */


    /**
     * The taxonomy category has one translation that matches the document locale.
     */
    @Test
    public void oneMatchingTaxonomyLocale() throws Exception {
        Node categoryNode = session.getNode("/test/taxonomy/top/branch");
        IModel<Node> nodeModel = new JcrNodeModel(categoryNode);
        JcrCategory category = new JcrCategory(nodeModel, true, getService());

        Locale documentLocale = new Locale.Builder().setLanguage("en").build();
        assertEquals(TaxonomyHelper.getCategoryName(category, documentLocale), BRANCH_NAME_EN);

    }

    /**
     * The taxonomy category has one translation that not matches the document locale.
     */
    @Test
    public void oneNonMatchingTaxonomyLocale() throws Exception {
        Node categoryNode = session.getNode("/test/taxonomy/top/branch");
        IModel<Node> nodeModel = new JcrNodeModel(categoryNode);
        JcrCategory category = new JcrCategory(nodeModel, true, getService());

        Locale documentLocale = new Locale.Builder().setLanguage("fr").build();
        assertEquals(TaxonomyHelper.getCategoryName(category, documentLocale), BRANCH_NAME);

    }

    /**
     * The taxonomy category has one translation that matches the document locale.
     * The taxonomy locale does not specify a region, but the document's locale does.
     */
    @Test
    public void oneMatchingTaxonomyLocaleWithRegion() throws Exception {
        Node categoryNode = session.getNode("/test/taxonomy/top/branch");
        IModel<Node> nodeModel = new JcrNodeModel(categoryNode);
        JcrCategory category = new JcrCategory(nodeModel, true, getService());

        Locale documentLocale = new Locale.Builder().setLanguage("en").setRegion("GB").build();
        assertEquals(TaxonomyHelper.getCategoryName(category, documentLocale), BRANCH_NAME_EN);
    }

    /**
     * The taxonomy category has one translation that not matches the document locale.
     * The taxonomy locale does specify a region, but the document's locale does not.
     */
    @Test
    public void oneNonMatchingTaxonomyLocaleWithRegion() throws Exception {
        Node categoryNode = session.getNode("/test/taxonomy/top/branch-two");
        IModel<Node> nodeModel = new JcrNodeModel(categoryNode);
        JcrCategory category = new JcrCategory(nodeModel, true, getService());

        Locale documentLocale = new Locale.Builder().setLanguage("en").build();
        assertEquals(TaxonomyHelper.getCategoryName(category, documentLocale), BRANCH_TWO_NAME);
    }

    /**
     * The taxonomy category has two translations, one with a language-only locale, the other also with Region.
     * The document locale has only a language code.
     */
    @Test
    public void twoTaxonomyLocalesDocumentWithLanguage() throws Exception {
        Node categoryNode = session.getNode("/test/taxonomy/top/branch-three");
        IModel<Node> nodeModel = new JcrNodeModel(categoryNode);
        JcrCategory category = new JcrCategory(nodeModel, true, getService());

        Locale documentLocale = new Locale.Builder().setLanguage("en").build();
        assertEquals(TaxonomyHelper.getCategoryName(category, documentLocale), BRANCH_NAME_THREE_EN);
    }

    /**
     * The taxonomy category has two translations, one with a language-only locale, the other also with Region.
     * The document locale has a language and region code.
     */
    @Test
    public void twoTaxonomyLocalesDocumentWithLanguageAndRegion() throws Exception {
        Node categoryNode = session.getNode("/test/taxonomy/top/branch-three");
        IModel<Node> nodeModel = new JcrNodeModel(categoryNode);
        JcrCategory category = new JcrCategory(nodeModel, true, getService());

        Locale documentLocale = new Locale.Builder().setLanguage("en").setRegion("GB").build();
        assertEquals(TaxonomyHelper.getCategoryName(category, documentLocale), BRANCH_NAME_THREE_EN_GB);
    }
}
