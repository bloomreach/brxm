/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('Page Service', function () {
    'use strict';

    var pageService, $httpBackend;

    beforeEach(module('hippo.channel'));

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('hippo.channel.ConfigService', {
                apiUrlPrefix: 'api',
                sitemapId: '7a66c027-9dd1-423e-8158-7c28144f47e2',
                sitemapItemId: '18b7b53e-d351-457e-99d7-d6f7398ec522'
            });
            $provide.value('hippo.channel.HstApiRequests', jasmine.createSpy());
        });
    });

    beforeEach(inject(function ($injector) {
        var pages = [
            {
                "id": "5b48b703-1b7f-43e4-8460-83a7b3835db6",
                "parentId": null,
                "name": "root",
                "pageTitle": 'Home Page',
                "pathInfo": "/",
                "componentConfigurationId": "hst:pages/home",
                "workspaceConfiguration": false,
                "inherited": false,
                "relativeContentPath": "common/homepage"
            },
            {
                "id": "18b7b53e-d351-457e-99d7-d6f7398ec522",
                "parentId": null,
                "name": "about",
                "pageTitle": "About Us",
                "pathInfo": "about",
                "componentConfigurationId": "hst:pages/textpage",
                "workspaceConfiguration": false,
                "inherited": false,
                "relativeContentPath": "common/about-us"
            },
            {
                "id": "b8f53634-cdef-46e9-8b86-6f60873a62a5",
                "parentId": null,
                "name": "news",
                "pageTitle": null,
                "pathInfo": "news",
                "componentConfigurationId": "hst:pages/newsoverview",
                "workspaceConfiguration": false,
                "inherited": false,
                "relativeContentPath": "news"
            },
            {
                "id": "a3771ec3-11d5-40b6-bb2a-6d24d70b7a32",
                "parentId": null,
                "name": "search",
                "pageTitle": null,
                "pathInfo": "search",
                "componentConfigurationId": "hst:pages/search",
                "workspaceConfiguration": false,
                "inherited": false,
                "relativeContentPath": null
            }
        ];

        $httpBackend = $injector.get('$httpBackend');

        // get all pages
        $httpBackend.when('GET', 'api/7a66c027-9dd1-423e-8158-7c28144f47e2./pages').respond({
            "data": {
                "id": "7a66c027-9dd1-423e-8158-7c28144f47e2",
                "pages": pages
            }
        });

        // get the current page ('about')
        $httpBackend.when('GET', 'api/7a66c027-9dd1-423e-8158-7c28144f47e2./item/18b7b53e-d351-457e-99d7-d6f7398ec522').respond({
            "data": pages[1]
        });

        // delete page by id
        $httpBackend.when('POST', 'api/7a66c027-9dd1-423e-8158-7c28144f47e2./item/18b7b53e-d351-457e-99d7-d6f7398ec522').respond({
            success: true,
            message: "Item deleted successfully",
            data: "18b7b53e-d351-457e-99d7-d6f7398ec522"
        });
    }));

    beforeEach(inject(['hippo.channel.PageService', function (PageService) {
        pageService = PageService;
    }]));

    afterEach(function () {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('should exist', function () {
        expect(pageService).toBeDefined();
    });

    function expectGetPages() {
        $httpBackend.expectGET('api/7a66c027-9dd1-423e-8158-7c28144f47e2./pages');
        $httpBackend.flush();
    }

    it('should get pages by sitemap id', function () {
        pageService.getPages().then(function (pages) {
            expect(pages).toBeDefined();
            expect(pages.length).toEqual(4);
            expect(pages[0].name).toEqual('root');
            expect(pages[0].pageTitle).toEqual('Home Page');
        });
        expectGetPages();
    });

    function expectGetCurrentPage() {
        $httpBackend.expectGET('api/7a66c027-9dd1-423e-8158-7c28144f47e2./item/18b7b53e-d351-457e-99d7-d6f7398ec522');
        $httpBackend.flush();
    }

    it('should get the current page by sitemap id', function () {
        pageService.getCurrentPage().then(function (currentPage) {
            expect(currentPage).toBeDefined();
            expect(currentPage.id).toEqual('18b7b53e-d351-457e-99d7-d6f7398ec522');
            expect(currentPage.name).toEqual('about');
            expect(currentPage.pageTitle).toEqual('About Us');
        });
        expectGetCurrentPage();
    });

    it('should get the hostname by sitemap id', function () {
        $httpBackend.expectGET('api/7a66c027-9dd1-423e-8158-7c28144f47e2./hostname').respond({
            data: 'hostname'
        });
        pageService.getHost().then(function (data) {
            expect(data).toBe('hostname');
        });
        $httpBackend.flush();
    });

    it('should remove a page', function () {
        $httpBackend.expectPOST('api/7a66c027-9dd1-423e-8158-7c28144f47e2./delete/18b7b53e-d351-457e-99d7-d6f7398ec522').respond(200, {
            success: true,
            message: "Item deleted successfully",
            data: "18b7b53e-d351-457e-99d7-d6f7398ec522"
        });

        pageService.deletePage('18b7b53e-d351-457e-99d7-d6f7398ec522');
        $httpBackend.flush();
    });
});
