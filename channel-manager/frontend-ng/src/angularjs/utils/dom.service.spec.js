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

describe('DomService', () => {
  'use strict';

  let DomService;
  const fixturesPath = `/${jasmine.getFixtures().fixturesPath}`;

  beforeEach(() => {
    module('hippo-cm.utils');

    module(($provide) => {
      $provide.value('$document', [{
        location: {
          pathname: '/app/root/index.html',
          host: 'localhost:8080',
        },
      }]);
    });

    inject((_DomService_) => {
      DomService = _DomService_;
    });

    jasmine.getFixtures().load('utils/dom.service.fixture.html');
  });

  function testInIframe(callback) {
    const iframe = $j('#testIframe');
    iframe.on('load', () => {
      callback(iframe[0].contentWindow);
    });
    iframe.attr('src', `${fixturesPath}/utils/dom.service.iframe.fixture.html`);
  }

  it('can add a css file to the head', (done) => {
    testInIframe((iframeWindow) => {
      const cssFile = `${fixturesPath}/utils/dom.service.fixture.css`;
      DomService.addCss(iframeWindow, cssFile)
        .then(() => {
          const red = $j(iframeWindow.document).find('#red');
          expect(red.css('color')).toEqual('rgb(255, 0, 0)');
          done();
        });
    });
  });

  it('can add a script file to the body', (done) => {
    testInIframe((iframeWindow) => {
      const script = `${fixturesPath}/utils/dom.service.fixture.js`;
      expect(iframeWindow.DomServiceTestScriptLoaded).not.toBeDefined();
      DomService.addScript(iframeWindow, script)
        .then(() => {
          expect(iframeWindow.DomServiceTestScriptLoaded).toEqual(true);
          done();
        });
    });
  });

  it('rejects a promise when a script url returns 404', (done) => {
    testInIframe((iframeWindow) => {
      DomService.addScript(iframeWindow, 'does-not-exist.js').catch(done);
    });
  });

  it('returns the app root url with // protocol', () => {
    expect(DomService.getAppRootUrl()).toEqual('//localhost:8080/app/root/');
  });

  function expectEqualComputedStyle(elements1, elements2) {
    expect(elements1.length).toEqual(elements2.length);

    for (let i = 0; i < elements1.length; i++) {
      const computedStyle1 = window.getComputedStyle(elements1[i]);
      const computedStyle2 = window.getComputedStyle(elements2[i]);
      expect(computedStyle1.cssText).toEqual(computedStyle2.cssText);
    }
  }

  it('can copy the computed style of an element', () => {
    window.loadStyleFixtures('utils/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');
    DomService.copyComputedStyleExcept(source[0], target[0]);
    expectEqualComputedStyle(source, target);
  });

  it('can copy the computed style of an element except excluded properties', () => {
    window.loadStyleFixtures('utils/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    expect(source.css('position')).toEqual('fixed');
    DomService.copyComputedStyleExcept(source[0], target[0], ['color']);
    expect(target.css('position')).toEqual('static');
  });

  it('can copy the computed style of descendants', () => {
    window.loadStyleFixtures('utils/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    DomService.copyComputedStyleOfDescendantsExcept(source[0], target[0]);
    expectEqualComputedStyle(source.find('*'), target.find('*'));
  });

  it('can copy the computed style of descendants except excluded properties', () => {
    window.loadStyleFixtures('utils/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    const sourceUl = source.find('ul');
    const targetUl = target.find('ul');

    // 'color' is excluded so the default value (black) should be inherited
    expect(sourceUl.css('color')).toEqual('rgb(0, 0, 255)');
    DomService.copyComputedStyleOfDescendantsExcept(source[0], target[0], ['color']);
    expect(targetUl.css('color')).toEqual('rgb(0, 0, 0)');
  });
});

