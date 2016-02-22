describe('HstCommentsProcessorService', function () {
  'use strict';

  var hstCommentsProcessorService;
  function NOOP() {}

  beforeEach(function () {
    module('hippo-cm.channel.hippoIframe');

    inject(function (_hstCommentsProcessorService_) {
      hstCommentsProcessorService = _hstCommentsProcessorService_;
    });
  });

  // PhantomJS does not support XPath querying through document.evaluate (https://github.com/ariya/phantomjs/issues/10161)
  it('should use DOM-walking when XPath querying is not available', function () {
    jasmine.getFixtures().load('channel/hippoIframe/hstCommentsProcessor.service.fixture.html');
    spyOn(hstCommentsProcessorService, 'processCommentsWithDomWalking');
    hstCommentsProcessorService.run($j('#jasmine-fixtures')[0], NOOP);
    expect(hstCommentsProcessorService.processCommentsWithDomWalking).toHaveBeenCalled();
  });


  it('should process comments with DOM-walking', function () {
    var gatheredData = [];
    jasmine.getFixtures().load('channel/hippoIframe/hstCommentsProcessor.service.fixture.html');
    hstCommentsProcessorService.processCommentsWithDomWalking($j('#jasmine-fixtures')[0], function (element, json) {
      gatheredData.push(json);
    });

    expect(gatheredData).toEqual([
      { 'HST-Type': 'CONTAINER_COMPONENT', name: 'Container-1' },
      { 'HST-Type': 'CONTAINER_ITEM_COMPONENT', name: 'Container-1-Item-1' },
      { 'HST-Type': 'PAGE-META-DATA', name: 'Page-1' },
    ]);
  });

  it('should gracefully handle an undefined element when DOM-walking', function () {
    expect(hstCommentsProcessorService.processCommentsWithDomWalking).not.toThrow();
  });


  it('should not invoke callback when JSON data is invalid', function () {
    var observer = { callback: NOOP };
    spyOn(observer, 'callback');

    jasmine.getFixtures().load('channel/hippoIframe/hstCommentsProcessor.service.invalid.json.fixture.html');
    hstCommentsProcessorService.processCommentsWithDomWalking($j('#jasmine-fixtures')[0], observer.callback);

    expect(observer.callback).not.toHaveBeenCalled();
  });

});
