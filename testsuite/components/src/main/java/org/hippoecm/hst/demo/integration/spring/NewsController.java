/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.integration.spring;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.math.NumberUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.NewsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/news")
public class NewsController {

    private static Logger log = LoggerFactory.getLogger(NewsController.class);

    @RequestMapping(value={"", "/"})
    public ModelAndView listHandler(
            @RequestParam(value = "pi", defaultValue="1") String pageIndexParam,
            @RequestParam(value = "ps", defaultValue="10") String pageSizeParam,
            HttpServletRequest request) {

        HstRequestContext requestContext = RequestContextProvider.get();
        ModelAndView mav = new ModelAndView("news/list");

        List<HippoBean> documents = new ArrayList<HippoBean>();
        int pageIndex = NumberUtils.toInt(pageIndexParam, 1);
        int pageSize = NumberUtils.toInt(pageSizeParam, 10);
        int totalSize = 0;

        try {
            HippoBean scope = requestContext.getContentBean();
            mav.addObject("scope", scope);

            HstQuery query = requestContext.getHstQueryManagerFactory().createQueryManager(requestContext.getSession(), requestContext.getContentBeansTool().getObjectConverter()).createQuery(scope, NewsBean.class);
            query.setOffset((pageIndex - 1) * pageSize);
            query.setLimit(pageSize);
            HstQueryResult result = query.execute();
            totalSize = result.getTotalSize();
            HippoBeanIterator it = result.getHippoBeans();

            while (it.hasNext()) {
                NewsBean document = (NewsBean) it.nextHippoBean();
                if (document != null) {
                    documents.add(document);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get news documents.", e);
        }

        mav.addObject("documents", documents);

        mav.addObject("pageIndex", Integer.valueOf(pageIndex));
        mav.addObject("pageSize", Integer.valueOf(pageSize));
        mav.addObject("totalSize", Integer.valueOf(totalSize));

        int pageCount = (totalSize % pageSize == 0 ? (totalSize / pageSize) : (totalSize / pageSize + 1));
        List<Integer> pageNums = new ArrayList<Integer>();

        for (int i = 1; i <= pageCount; i++) {
            pageNums.add(i);
        }

        mav.addObject("pageNums", pageNums);

        return mav;
    }

    @RequestMapping("/**")
    public ModelAndView itemHandler() {
        ModelAndView mav = new ModelAndView("news/item");
        mav.addObject("document", RequestContextProvider.get().getContentBean());
        return mav;
    }

}
