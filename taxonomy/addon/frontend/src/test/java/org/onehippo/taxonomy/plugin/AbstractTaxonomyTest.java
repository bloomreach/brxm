/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import java.util.Collections;
import java.util.List;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.api.EditableTaxonomy;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;
import org.onehippo.taxonomy.plugin.model.JcrTaxonomy;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFO;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFOS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_KEY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_NAME;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY;

public abstract class AbstractTaxonomyTest extends PluginTest {

    public final static String TOP_KEY = "top-key";
    public final static String BRANCH_KEY = "branch-key";
    public final static String TAXONOMY_NAME = "taxonomy"; // taxonomy root node name
    public final static String TOP_NAME = "top";
    public final static String BRANCH_NAME = "branch";
    public final static String BRANCH_TWO_NAME = "branch-two";
    public final static String BRANCH_NAME_EN = "branch-name-en";
    public final static String BRANCH_SYNONYM = "branch-synonym";
    public static final String BRANCH_NAME_THREE_EN = "branch-name-three-en";
    public static final String BRANCH_NAME_THREE_EN_GB = "branch-name-three-en-GB";

    private  ITaxonomyService service;

    String[] content = new String[] {
            "/test", "nt:unstructured",
                "/test/taxonomy", TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY,
                    "jcr:mixinTypes", HippoStdNodeType.NT_PUBLISHABLESUMMARY,
                    "hippostd:state", "unpublished",
                    "hippostd:stateSummary", "new",
                    "hippostdpubwf:createdBy", "admin",
                    "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
                    "hippostdpubwf:lastModifiedBy", "admin",
                    "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00",
                    "/test/taxonomy/top", NODETYPE_HIPPOTAXONOMY_CATEGORY,
                        HIPPOTAXONOMY_KEY, TOP_KEY,
                        "/test/taxonomy/top/branch", NODETYPE_HIPPOTAXONOMY_CATEGORY,
                            HIPPOTAXONOMY_KEY, BRANCH_KEY,
                            "/test/taxonomy/top/branch/" + HIPPOTAXONOMY_CATEGORYINFOS, HIPPOTAXONOMY_CATEGORYINFOS,
                                "/test/taxonomy/top/branch/" + HIPPOTAXONOMY_CATEGORYINFOS + "/en", HIPPOTAXONOMY_CATEGORYINFO,
                                HIPPOTAXONOMY_NAME, BRANCH_NAME_EN,
                                HIPPOTAXONOMY_SYNONYMS, BRANCH_SYNONYM,
                        "/test/taxonomy/top/branch-two", NODETYPE_HIPPOTAXONOMY_CATEGORY,
                            HIPPOTAXONOMY_KEY, "branch-two-key",
                            "/test/taxonomy/top/branch-two/" + HIPPOTAXONOMY_CATEGORYINFOS, HIPPOTAXONOMY_CATEGORYINFOS,
                                "/test/taxonomy/top/branch-two/" + HIPPOTAXONOMY_CATEGORYINFOS + "/en_GB", HIPPOTAXONOMY_CATEGORYINFO,
                                HIPPOTAXONOMY_NAME, "branch-name-en-GB",
                         "/test/taxonomy/top/branch-three", NODETYPE_HIPPOTAXONOMY_CATEGORY,
                            HIPPOTAXONOMY_KEY, "branch-three-key",
                            "/test/taxonomy/top/branch-three/" + HIPPOTAXONOMY_CATEGORYINFOS, HIPPOTAXONOMY_CATEGORYINFOS,
                                "/test/taxonomy/top/branch-three/" + HIPPOTAXONOMY_CATEGORYINFOS + "/en", HIPPOTAXONOMY_CATEGORYINFO,
                                HIPPOTAXONOMY_NAME, BRANCH_NAME_THREE_EN,
                                "/test/taxonomy/top/branch-three/" + HIPPOTAXONOMY_CATEGORYINFOS + "/en_GB", HIPPOTAXONOMY_CATEGORYINFO,
                                HIPPOTAXONOMY_NAME, BRANCH_NAME_THREE_EN_GB

    };

    protected EditableTaxonomy taxonomy;
    protected EditableTaxonomy nonEditingTaxonomy;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        service = createNiceMock(ITaxonomyService.class);
        final List<JcrCategoryFilter> filters = getFilters();
        expect(service.getCategoryFilters()).andReturn(filters).anyTimes();
        replay(service);

        build(content, session);
        session.save();
        taxonomy = new JcrTaxonomy(new JcrNodeModel("/test/taxonomy"), true, service);
        nonEditingTaxonomy = new JcrTaxonomy(new JcrNodeModel("/test/taxonomy"), false, service);
    }

    public List<JcrCategoryFilter> getFilters() {
        return Collections.emptyList();
    }

    public ITaxonomyService getService() {
        return service;
    }

}
