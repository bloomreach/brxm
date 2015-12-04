'use strict';

describe('hippo nbgp protractor test', function () {
  it('should have a title', function () {
    browser.get('http://localhost:9000/#/');

    expect(browser.getTitle()).toEqual('Hippo Angular Boilerplate');
  });
});