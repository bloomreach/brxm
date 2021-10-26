/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

describe('DomService', () => {
  let DomService;
  let $window;
  const fixturesPath = `/${jasmine.getFixtures().fixturesPath}`;
  const documentMock = [{
    location: {
      pathname: '/app/root/index.html',
      host: 'localhost:8080',
    },
  }];

  documentMock.on = angular.noop;
  documentMock.off = angular.noop;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    angular.mock.module(($provide) => {
      $provide.value('$document', documentMock);
    });

    inject((_DomService_, _$window_) => {
      DomService = _DomService_;
      $window = _$window_;
    });

    jasmine.getFixtures().load('services/dom.service.fixture.html');
  });

  function testInIframe(callback) {
    const iframe = $j('#testIframe');
    iframe.on('load', () => {
      callback(iframe[0].contentWindow);
    });
    iframe.attr('src', `${fixturesPath}/services/dom.service.iframe.fixture.html`);
  }

  it('adds a CSS link tag to the head', (done) => {
    testInIframe((iframeWindow) => {
      DomService.addCssLinks(iframeWindow, [
        'testFile.css',
        'anotherFile.css',
      ]);

      const head = $j(iframeWindow.document).find('head');
      const links = $j(head).children('link');

      expect(links.length).toEqual(2);

      expect(links.eq(0).attr('href')).toContain('testFile.css');
      expect(links.eq(1).attr('href')).toContain('anotherFile.css');

      done();
    });
  });

  it('rejects on failed loading', (done) => {
    testInIframe((iframeWindow) => {
      DomService.addCssLinks(iframeWindow, [
        'testFile.css',
        'anotherFile.css',
      ])
        .then(() => fail(' calling DomService.addCssLinks() should have rejected'))
        .catch(done);

      const links = $j('head link[href$="File.css"]', iframeWindow.document);
      links.eq(0).trigger('load');
      links.eq(1).trigger('error');
    });
  });

  it('resolves on complete loading', (done) => {
    testInIframe((iframeWindow) => {
      DomService.addCssLinks(iframeWindow, [
        'testFile.css',
        'anotherFile.css',
      ])
        .then(done)
        .catch(() => fail('calling DomService.addCssLinks() should have resolved'));

      const links = $j('head link[href$="File.css"]', iframeWindow.document);
      links.eq(0).trigger('load');
      links.eq(1).trigger('load');
    });
  });

  it('adds a script file to the body', (done) => {
    testInIframe((iframeWindow) => {
      const script = `${fixturesPath}/services/dom.service.fixture.js`;
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

  // Release 67 of Google Chrome introduced a failing test on comparing the copied computed style values,
  // specifically the '-webkit-app-region' property. A little investigation showed that we don't
  // depend on this property and as such can safely skip it during the validation.
  const skipComputedStyleProperties = ['-webkit-app-region', 'app-region'];
  function expectEqualComputedStyle(elementsSource, elementsTarget) {
    expect(elementsSource.length).toEqual(elementsTarget.length);

    for (let i = 0; i < elementsSource.length; i += 1) {
      const computedStyleSource = window.getComputedStyle(elementsSource[i]);
      const computedStyleTarget = window.getComputedStyle(elementsTarget[i]);
      expect(computedStyleSource.length).toEqual(computedStyleTarget.length);

      for (let j = 0; j < computedStyleSource.length; j += 1) {
        const propertyName = computedStyleSource.item(j);
        if (!skipComputedStyleProperties.includes(propertyName)) {
          const propertyValueSource = computedStyleSource.getPropertyValue(propertyName);
          const propertyValueTarget = computedStyleTarget.getPropertyValue(propertyName);
          expect(propertyValueSource).toBe(propertyValueTarget, `-> CSS property ${propertyName}`);
        }
      }
    }
  }

  it('copies the computed style of an element', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');
    DomService.copyComputedStyleExcept(source[0], target[0]);
    expectEqualComputedStyle(source, target);
  });

  it('merges the existing style attribute of the target with the copied computed style of the source element', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    expect(source.css('width')).not.toEqual('42px');
    target[0].style.width = '42px';

    DomService.copyComputedStyleExcept(source[0], target[0], ['width']);

    expect(target.css('color')).toEqual(source.css('color'));
    expect(target.css('width')).toEqual('42px');
  });

  it('copies the computed style of an element except excluded properties', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    // don't copy the 'position' property, so the target should use the default value 'static'
    expect(source.css('position')).toEqual('fixed');
    expect(target.css('position')).toEqual('static');
    DomService.copyComputedStyleExcept(source[0], target[0], ['position']);
    expect(target.css('position')).toEqual('static');
  });

  it('copies the computed style of descendants', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    DomService.copyComputedStyleOfDescendantsExcept(source[0], target[0]);
    expectEqualComputedStyle(source.find('*'), target.find('*'));
  });

  it('copies the computed style of descendants except excluded properties', () => {
    window.loadStyleFixtures('services/dom.service.fixture.css');
    const source = $j('#copyComputedStyleSource');
    const target = $j('#copyComputedStyleTarget');

    // 'color' is excluded from copying so the default value (black) should be inherited by the target
    expect(source.find('ul').css('color')).toEqual('rgb(0, 0, 255)');
    DomService.copyComputedStyleOfDescendantsExcept(source[0], target[0], ['color']);
    expect(target.find('ul').css('color')).toEqual('rgb(0, 0, 0)');
  });

  it('creates a mousedown event', () => {
    const mouseDownEvent = DomService.createMouseDownEvent(window, 100, 200);
    expect(mouseDownEvent.type).toEqual('mousedown');
    expect(mouseDownEvent.bubbles).toEqual(true);
    expect(mouseDownEvent.clientX).toEqual(100);
    expect(mouseDownEvent.clientY).toEqual(200);
    expect(mouseDownEvent.view).toEqual(window);
  });

  it('creates a pointer event', () => {
    $window.navigator.pointerEnabled = true;
    const mouseDownEvent = DomService.createMouseDownEvent(window, 100, 200);
    expect(mouseDownEvent.type).toEqual('pointerdown');
    expect(mouseDownEvent.bubbles).toEqual(true);
    expect(mouseDownEvent.clientX).toEqual(100);
    expect(mouseDownEvent.clientY).toEqual(200);
    expect(mouseDownEvent.view).toEqual(window);
  });

  it('checks if an element is hidden on the page', () => {
    $j('.shouldBeHidden').each((index, el) => {
      expect(DomService.isVisible($j(el))).toBe(false);
    });
  });

  it('checks if an element is visible on the page', () => {
    $j('.shouldBeVisible').each((index, el) => {
      expect(DomService.isVisible($j(el))).toBe(true);
    });
  });

  it('checks whether the body is visible', () => {
    expect(DomService.isVisible($j(document.body))).toBe(true);
  });

  it('escapes HTML characters in strings', () => {
    expect(DomService.escapeHtml('&<>"\'/')).toEqual('&amp;&lt;&gt;&quot;&#x27;&#x2F;');
    expect(DomService.escapeHtml('<script>alert("xss")</script>'))
      .toEqual('&lt;script&gt;alert(&quot;xss&quot;)&lt;&#x2F;script&gt;');
  });

  describe('getAssetUrl', () => {
    it('should resolve a relative link', () => {
      expect(DomService.getAssetUrl('some.js')).toContain('/some.js');
    });
  });

  describe('isFrameAccessible', () => {
    it('should return false when content window document is not accessible', () => {
      const iframe = { contentWindow: {} };
      Object.defineProperty(iframe.contentWindow, 'document', { get: () => { throw new Error('error'); } });

      expect(DomService.isFrameAccessible(iframe)).toBe(false);
    });

    it('should return false when parent document is not accessible', () => {
      const parent = {};
      Object.defineProperty(parent, 'document', { get: () => { throw new Error('error'); } });

      expect(DomService.isFrameAccessible(parent)).toBe(false);
    });

    it('should return false when document has no body', () => {
      const parent = { document: {} };

      expect(DomService.isFrameAccessible(parent)).toBe(false);
    });

    it('should return false when document body has no HTML', () => {
      const parent = { document: { body: {} } };

      expect(DomService.isFrameAccessible(parent)).toBe(false);
    });

    it('should return true when document body has HTML', () => {
      const parent = { document: { body: { innerHTML: '' } } };

      expect(DomService.isFrameAccessible(parent)).toBe(true);
    });
  });
});
