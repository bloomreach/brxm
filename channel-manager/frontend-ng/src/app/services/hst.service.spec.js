/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('HstService', () => {
  let $q;
  let $timeout;
  let $httpBackend;
  let hstService;
  let ConfigServiceMock;

  const cmsContextPath = '/cms';
  const contextPath = '/testContextPath';
  const hostGroup = 'testHostGroup';
  const apiUrlPrefix = '/testApiUrlPrefix';
  const rootUuid = 'cafebabe';
  const hostname = 'test.host.name';
  const mountId = '1234';
  const handshakeUrl = `${cmsContextPath}${apiUrlPrefix}/${rootUuid}./composermode/${hostname}/${mountId}`;
  const channel = { contextPath, hostname, mountId };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ConfigServiceMock = {
      apiUrlPrefix,
      cmsUser: 'testUser',
      getCmsContextPath: () => cmsContextPath,
      rootUuid,
    };

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject((_$q_, _$timeout_, _$httpBackend_, _HstService_) => {
      $q = _$q_;
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      hstService = _HstService_;
    });
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('prefixes all API urls with the context path and api url prefix', () => {
    expect(hstService._createApiUrl('1234', ['somepath'])).toEqual(`${cmsContextPath}${apiUrlPrefix}/1234./somepath`);
  });

  it('trims concatenated path elements', () => {
    expect(hstService._createApiUrl('1234', ['  foo ', ' bar']))
      .toEqual(`${cmsContextPath}${apiUrlPrefix}/1234./foo/bar`);
  });

  it('ignores undefined path elements', () => {
    expect(hstService._createApiUrl('1234', [undefined, 'bar', undefined]))
      .toEqual(`${cmsContextPath}${apiUrlPrefix}/1234./bar`);
  });

  it('removes clashing slashes from concatenated path elements', () => {
    expect(hstService._createApiUrl('1234', ['/foo/', '/bar']))
      .toEqual(`${cmsContextPath}${apiUrlPrefix}/1234./foo/bar`);
  });

  it('can create an API URL with only a UUID', () => {
    expect(hstService._createApiUrl('1234', [])).toEqual(`${cmsContextPath}${apiUrlPrefix}/1234./`);
  });

  describe('when initialization is successful', () => {
    it('resolves a promise', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(channel).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalled();
    });

    it('resolves with a privileges object', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const privileges = {};
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: privileges });
      hstService.initializeSession(channel).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(privileges);
    });

    it('resolves with null if response data parameter is missing', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(channel).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(null);
    });
  });

  it('rejects a promise when initialization fails', () => {
    const catchSpy = jasmine.createSpy('catchSpy');
    $httpBackend.expectGET(handshakeUrl).respond(500);
    hstService
      .initializeSession(channel)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('gets a channel by id', () => {
    const channelA = {
      id: 'channelA',
      contextPath: '/a',
    };
    const url = `${cmsContextPath}${apiUrlPrefix}/${rootUuid}./channels/${channelA.id}`;
    const catchSpy = jasmine.createSpy('catchSpy');

    $httpBackend.expectGET(url).respond(200, channelA);
    hstService.getChannel('channelA', contextPath)
      .then(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalledWith(channelA);
  });

  it('rejects promise when get a channel without id', (done) => {
    hstService.getChannel('')
      .then(() => done(new Error('Promise should not be resolved')))
      .catch(() => done());

    $timeout.flush();
  });

  it('rejects a promise when a channel load fails', () => {
    const catchSpy = jasmine.createSpy('catchSpy');
    const url = `${cmsContextPath}${apiUrlPrefix}/${rootUuid}./channels/test`;
    $httpBackend.expectGET(url).respond(500);
    hstService
      .getChannel('test', contextPath, hostGroup)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('rejects a promise on a request cancel', () => {
    const catchSpy = jasmine.createSpy('catchSpy');
    const url = `${cmsContextPath}${apiUrlPrefix}/${rootUuid}./channels/test`;
    $httpBackend.expectGET(url).respond(200);
    hstService
      .getChannel('test', contextPath, hostGroup)
      .cancel()
      .catch(catchSpy);

    $timeout.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  describe('with an initialized session', () => {
    beforeEach(() => {
      hstService.contextPath = contextPath;
      hstService.hostGroup = hostGroup;
    });

    it('can do a GET call', () => {
      $httpBackend.expectGET(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`, {
        'CMS-User': 'testUser',
        contextPath,
        hostGroup,
        Accept: 'application/json, text/plain, */*',
      }).respond(200);
      hstService.doGet('some-uuid', 'one', 'two', 'three').catch(fail);
      $httpBackend.flush();
    });

    it('can do a GET call with query parameters', () => {
      const params = {
        param1: 'value1',
        'param/2': 'value/2',
      };
      $httpBackend
        .expectGET(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three?param1=value1&param%2F2=value%2F2`, {
          'CMS-User': 'testUser',
          contextPath,
          hostGroup,
          Accept: 'application/json, text/plain, */*',
        }).respond(200);
      hstService.doGetWithParams('some-uuid', params, 'one', 'two', 'three').catch(fail);
      $httpBackend.flush();
    });

    it('returns a rejected promise when a GET call fails', () => {
      $httpBackend.expectGET(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`).respond(500);
      hstService.doGet('some-uuid', 'one', 'two', 'three').then(fail);
      $httpBackend.flush();
    });

    it('can do a POST call', () => {
      $httpBackend.expectPOST(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`, { foo: 1 }, {
        'CMS-User': 'testUser',
        Accept: 'application/json, text/plain, */*',
        contextPath,
        hostGroup,
        'Content-Type': 'application/json;charset=utf-8',
      }).respond(200);
      hstService.doPost({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').catch(fail);
      $httpBackend.flush();
    });

    it('can do a POST call without data', () => {
      $httpBackend.expectPOST(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`, null, {
        'CMS-User': 'testUser',
        Accept: 'application/json, text/plain, */*',
        contextPath,
        hostGroup,
        'Content-Type': 'application/json;charset=utf-8',
      }).respond(200);
      hstService.doPost(null, 'some-uuid', 'one', 'two', 'three').catch(fail);
      $httpBackend.flush();
    });

    it('returns a rejected promise when a POST call fails', () => {
      $httpBackend.expectPOST(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`, { foo: 1 }).respond(500);
      hstService.doPost({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').then(fail);
      $httpBackend.flush();
    });

    it('constructs a valid handshake url when initializing a channel session', () => {
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(channel);
      $httpBackend.flush();
    });

    it('can do a put call', () => {
      $httpBackend.expectPUT(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`, { foo: 1 }, {
        'CMS-User': 'testUser',
        Accept: 'application/json, text/plain, */*',
        contextPath,
        hostGroup,
        'Content-Type': 'application/json;charset=utf-8',
      }).respond(200);
      hstService.doPut({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').catch(fail);
      $httpBackend.flush();
    });

    it('can do a put form call', () => {
      $httpBackend.expectPUT(`${cmsContextPath}${apiUrlPrefix}/some-uuid./one/two/three`, 'foo=1&bar=a%20b', {
        'CMS-User': 'testUser',
        Accept: 'application/json, text/plain, */*',
        contextPath,
        hostGroup,
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
      }).respond(200);
      hstService.doPutForm({ foo: 1, bar: 'a b' }, 'some-uuid', 'one', 'two', 'three').catch(fail);
      $httpBackend.flush();
    });

    it('augments the set of requested POST headers', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const uuid = 'test-uuid';
      const url = `${cmsContextPath}${apiUrlPrefix}/${uuid}./pages`;
      const appHeaders = {
        foo: 'bar',
        test: 'me',
      };
      const httpHeaders = {
        foo: 'bar',
        test: 'me',
        'CMS-User': 'testUser',
        Accept: 'application/json, text/plain, */*',
        contextPath,
        hostGroup,
      };
      const response = { data: { key: 'value' } };
      $httpBackend.expectPOST(url, undefined, httpHeaders).respond(200, response);
      hstService.doPostWithHeaders(uuid, appHeaders, 'pages').then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith(response);
    });

    it('adds a new component from catalog toolkit', () => {
      spyOn(hstService, 'doPost').and.returnValue($q.when({ id: 'cafebabe' }));
      const container = jasmine.createSpyObj('container', ['getId', 'isXPageLayoutComponent']);
      container.getId.and.returnValue('container1');
      container.isXPageLayoutComponent.and.returnValue(false);

      hstService.addHstComponent({ id: '123456' }, container).then((response) => {
        expect(response).toEqual({ id: 'cafebabe' });
      });

      expect(hstService.doPost).toHaveBeenCalledWith(null, 'container1', '123456', undefined);
    });

    it('adds a new layout component from catalog toolkit', () => {
      spyOn(hstService, 'doPost').and.returnValue($q.when({ id: 'cafebabe' }));
      const container = jasmine.createSpyObj('container', ['getId', 'isXPageLayoutComponent',
        'getXPageLayoutHippoIdentifier']);
      container.getId.and.returnValue('container1');
      container.isXPageLayoutComponent.and.returnValue(true);
      container.getXPageLayoutHippoIdentifier.and.returnValue('hippoId');

      hstService.addHstComponent({ id: '123456' }, container).then((response) => {
        expect(response).toEqual({ id: 'cafebabe' });
      });

      expect(hstService.doPost).toHaveBeenCalledWith(null, 'container1', 'hippoId', '123456', undefined);
    });

    it('adds a new component from catalog toolkit before another component', () => {
      spyOn(hstService, 'doPost').and.returnValue($q.when({ id: 'cafebabe' }));
      const container = jasmine.createSpyObj('container', ['getId', 'isXPageLayoutComponent']);
      container.getId.and.returnValue('container1');

      hstService.addHstComponent({ id: '123456' }, container, '654321').then((response) => {
        expect(response).toEqual({ id: 'cafebabe' });
      });

      expect(hstService.doPost).toHaveBeenCalledWith(null, 'container1', '123456', '654321');
    });

    it('updates component orders of a container', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const url = `${cmsContextPath}${apiUrlPrefix}/container-1./`;
      $httpBackend.expectPUT(url, { foo: 'foo-value', baa: 'baah' }).respond(200);

      hstService.updateHstContainer('container-1', { foo: 'foo-value', baa: 'baah' }).then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalled();
    });

    it('extracts the sitemap from the returned pages response', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const siteMap = ['dummy'];
      const siteMapId = 'testSiteMapId';
      const url = `${cmsContextPath}${apiUrlPrefix}/${siteMapId}./pages`;
      $httpBackend.expectGET(url).respond(200, { data: { pages: siteMap } });

      hstService.getSiteMap('testSiteMapId').then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith(siteMap);
    });

    it('rejects the promise when retrieving the sitemap fails', () => {
      const catchSpy = jasmine.createSpy('catchSpy');
      const siteMapId = 'testSiteMapId';
      const url = `${cmsContextPath}${apiUrlPrefix}/${siteMapId}./pages`;
      $httpBackend.expectGET(url).respond(500);

      hstService.getSiteMap('testSiteMapId').catch(catchSpy);
      $httpBackend.flush();

      expect(catchSpy).toHaveBeenCalled();
    });
  });
});
