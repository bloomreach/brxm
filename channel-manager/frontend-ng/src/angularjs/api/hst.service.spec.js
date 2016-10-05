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

describe('HstService', () => {
  'use strict';

  let $q;
  let $httpBackend;
  let hstService;
  let ConfigServiceMock;

  const contextPath = '/testContextPath';
  const apiUrlPrefix = '/testApiUrlPrefix';
  const rootUuid = 'cafebabe';
  const hostname = 'test.host.name';
  const mountId = '1234';
  const handshakeUrl = `${contextPath}${apiUrlPrefix}/${rootUuid}./composermode/${hostname}/${mountId}`;

  beforeEach(() => {
    module('hippo-cm-api');

    ConfigServiceMock = {
      apiUrlPrefix,
      cmsUser: 'testUser',
      contextPath,
      rootUuid,
    };

    module(($provide) => {
      $provide.value('ConfigService', ConfigServiceMock);
    });

    inject((_$q_, _$httpBackend_, _HstService_) => {
      $q = _$q_;
      $httpBackend = _$httpBackend_;
      hstService = _HstService_;
    });
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('exists', () => {
    expect(hstService).toBeDefined();
  });

  it('prefixes all API urls with the context path and api url prefix', () => {
    expect(hstService._createApiUrl('1234', ['somepath'])).toEqual('/testContextPath/testApiUrlPrefix/1234./somepath');
  });

  it('trims concatenated path elements', () => {
    expect(hstService._createApiUrl('1234', ['  foo ', ' bar'])).toEqual('/testContextPath/testApiUrlPrefix/1234./foo/bar');
  });

  it('ignores undefined path elements', () => {
    expect(hstService._createApiUrl('1234', [undefined, 'bar', undefined])).toEqual('/testContextPath/testApiUrlPrefix/1234./bar');
  });

  it('removes clashing slashes from concatenated path elements', () => {
    expect(hstService._createApiUrl('1234', ['/foo/', '/bar'])).toEqual('/testContextPath/testApiUrlPrefix/1234./foo/bar');
  });

  it('can create an API URL with only a UUID', () => {
    expect(hstService._createApiUrl('1234', [])).toEqual('/testContextPath/testApiUrlPrefix/1234./');
  });

  it('can do a GET call', () => {
    $httpBackend.expectGET(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`, {
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
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
    $httpBackend.expectGET(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three?param1=value1&param%2F2=value%2F2`, {
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
      Accept: 'application/json, text/plain, */*',
    }).respond(200);
    hstService.doGetWithParams('some-uuid', params, 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('returns a rejected promise when a GET call fails', () => {
    $httpBackend.expectGET(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`).respond(500);
    hstService.doGet('some-uuid', 'one', 'two', 'three').then(fail);
    $httpBackend.flush();
  });

  it('can do a POST call', () => {
    $httpBackend.expectPOST(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`, { foo: 1 }, {
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json;charset=utf-8',
    }).respond(200);
    hstService.doPost({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('can do a POST call without data', () => {
    $httpBackend.expectPOST(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`, null, {
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json;charset=utf-8',
    }).respond(200);
    hstService.doPost(null, 'some-uuid', 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('returns a rejected promise when a POST call fails', () => {
    $httpBackend.expectPOST(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`, { foo: 1 }).respond(500);
    hstService.doPost({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').then(fail);
    $httpBackend.flush();
  });

  it('constructs a valid handshake url when initializing a channel session', () => {
    $httpBackend.expectGET(handshakeUrl).respond(200);
    hstService.initializeSession(hostname, mountId);
    $httpBackend.flush();
  });

  it('uses the new context path after it has been changed', () => {
    $httpBackend.expectGET('/testContextPath/testApiUrlPrefix/1234./test').respond(200);
    hstService.doGet('1234', 'test');
    $httpBackend.flush();

    ConfigServiceMock.contextPath = '/newContextPath';

    $httpBackend.expectGET('/newContextPath/testApiUrlPrefix/1234./test').respond(200);
    hstService.doGet('1234', 'test');
    $httpBackend.flush();
  });

  describe('when initialization is successful', () => {
    it('resolves a promise', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalled();
    });

    it('resolves with a privileges object', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const privileges = {
        canWrite: true,
      };
      $httpBackend.expectGET(handshakeUrl).respond(200, { data: privileges });
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(privileges);
    });

    it('resolves with null if response data parameter is missing', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(handshakeUrl).respond(200);
      hstService.initializeSession(hostname, mountId).then(promiseSpy);
      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith(null);
    });
  });

  it('rejects a promise when initialization fails', () => {
    const catchSpy = jasmine.createSpy('catchSpy');
    $httpBackend.expectGET(handshakeUrl).respond(500);
    hstService
      .initializeSession(hostname, mountId)
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('gets a channel by id', () => {
    const channelA = {
      id: 'channelA',
      contextPath: '/a',
    };
    const url = `${contextPath}${apiUrlPrefix}/${rootUuid}./channels/${channelA.id}`;
    const catchSpy = jasmine.createSpy('catchSpy');

    $httpBackend.expectGET(url).respond(200, channelA);
    hstService.getChannel('channelA')
      .then(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalledWith(channelA);
  });

  it('throws exception when get a channel without id', () => {
    expect(() => hstService.getChannel()).toThrowError('Channel id must be defined');
    expect(() => hstService.getChannel('')).toThrowError('Channel id must be defined');
  });

  it('rejects a promise when a channel load fails', () => {
    const catchSpy = jasmine.createSpy('catchSpy');
    const url = `${contextPath}${apiUrlPrefix}/${rootUuid}./channels/test`;
    $httpBackend.expectGET(url).respond(500);
    hstService.
      getChannel('test')
      .catch(catchSpy);

    $httpBackend.flush();
    expect(catchSpy).toHaveBeenCalled();
  });

  it('adds a new component from catalog toolkit', () => {
    spyOn(hstService, 'doPost').and.returnValue($q.when({ id: 'cafebabe' }));

    hstService.addHstComponent({ id: '123456' }, 'container1').then((response) => {
      expect(response).toEqual({ id: 'cafebabe' });
    });

    expect(hstService.doPost).toHaveBeenCalledWith(null, 'container1', '123456');
  });

  it('removes an exist component from a container', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    const url = `${contextPath}${apiUrlPrefix}/container-1./component-foo`;
    $httpBackend.expectDELETE(url).respond(200);

    hstService.removeHstComponent('container-1', 'component-foo').then(promiseSpy);
    $httpBackend.flush();

    expect(promiseSpy).toHaveBeenCalled();
  });

  it('updates component orders of a container', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    const url = `${contextPath}${apiUrlPrefix}/container-1./`;
    $httpBackend.expectPUT(url, { foo: 'foo-value', baa: 'baah' }).respond(200);

    hstService.updateHstComponent('container-1', { foo: 'foo-value', baa: 'baah' }).then(promiseSpy);
    $httpBackend.flush();

    expect(promiseSpy).toHaveBeenCalled();
  });

  it('extracts the sitemap from the returned pages response', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    const siteMap = ['dummy'];
    const siteMapId = 'testSiteMapId';
    const url = `${contextPath}${apiUrlPrefix}/${siteMapId}./pages`;
    $httpBackend.expectGET(url).respond(200, { data: { pages: siteMap } });

    hstService.getSiteMap('testSiteMapId').then(promiseSpy);
    $httpBackend.flush();

    expect(promiseSpy).toHaveBeenCalledWith(siteMap);
  });

  it('rejects the promise when retrieving the sitemap fails', () => {
    const catchSpy = jasmine.createSpy('catchSpy');
    const siteMapId = 'testSiteMapId';
    const url = `${contextPath}${apiUrlPrefix}/${siteMapId}./pages`;
    $httpBackend.expectGET(url).respond(500);

    hstService.getSiteMap('testSiteMapId').catch(catchSpy);
    $httpBackend.flush();

    expect(catchSpy).toHaveBeenCalled();
  });

  it('augments the set of requested POST headers', () => {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    const uuid = 'test-uuid';
    const url = `${contextPath}${apiUrlPrefix}/${uuid}./pages`;
    const appHeaders = {
      foo: 'bar',
      test: 'me',
    };
    const httpHeaders = {
      foo: 'bar',
      test: 'me',
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
      Accept: 'application/json, text/plain, */*',
    };
    const response = { data: { key: 'value' } };
    $httpBackend.expectPOST(url, undefined, httpHeaders).respond(200, response);
    hstService.doPostWithHeaders(uuid, appHeaders, 'pages').then(promiseSpy);
    $httpBackend.flush();

    expect(promiseSpy).toHaveBeenCalledWith(response);
  });

  it('handles undefined path elements gracefully', () => {
    $httpBackend.expectGET(`${contextPath}${apiUrlPrefix}./one`).respond(200);
    hstService.doGet(undefined, 'one');
    $httpBackend.flush();

    $httpBackend.expectGET(`${contextPath}${apiUrlPrefix}/uuid./one`).respond(200);
    hstService.doGet('uuid', undefined, 'one');
    $httpBackend.flush();

    delete ConfigServiceMock.contextPath;
    $httpBackend.expectGET(`${apiUrlPrefix}/uuid./`).respond(200);
    hstService.doGet('uuid');
    $httpBackend.flush();

    delete ConfigServiceMock.apiUrlPrefix;
    $httpBackend.expectGET('uuid./').respond(200);
    hstService.doGet('uuid');
    $httpBackend.flush();
    ConfigServiceMock.apiUrlPrefix = apiUrlPrefix;
    ConfigServiceMock.contextPath = contextPath;
  });

  it('can do a put call', () => {
    $httpBackend.expectPUT(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`, { foo: 1 }, {
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json;charset=utf-8',
    }).respond(200);
    hstService.doPut({ foo: 1 }, 'some-uuid', 'one', 'two', 'three').catch(fail);
    $httpBackend.flush();
  });

  it('reports user activity to the CMS when the backend is called', () => {
    spyOn(window.APP_TO_CMS, 'publish');
    $httpBackend.expectGET(`${contextPath}${apiUrlPrefix}/some-uuid./one/two/three`, {
      'CMS-User': 'testUser',
      'Force-Client-Host': 'true',
      Accept: 'application/json, text/plain, */*',
    }).respond(200);
    hstService.doGet('some-uuid', 'one', 'two', 'three');
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('user-activity');
    $httpBackend.flush();
  });
});
