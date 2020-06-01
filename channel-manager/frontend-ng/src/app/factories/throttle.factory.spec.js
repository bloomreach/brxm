/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('ThrottleFactory', () => {
  let throttle;
  let $timeout;
  let $browser;
  let $log;

  const delay = 100;
  let calls;
  let fn = angular.noop;

  beforeEach(() => {
    angular.mock.module('hippo-cm.factories');

    inject(($injector) => {
      $browser = $injector.get('$browser');
      $log = $injector.get('$log');
      $timeout = $injector.get('$timeout');
      throttle = $injector.get('throttle');

      // reset browser defer for $timeout service
      let requestCount = 0;
      const requests = [];
      const pendingDeferIds = {};

      function completeRequest(...args) {
        const callback = args[0];
        try {
          callback(...args.slice(1));
        } finally {
          requestCount -= 1;
          if (requestCount === 0) {
            while (requests.length) {
              try {
                requests.pop()();
              } catch (e) {
                $log.error(e);
              }
            }
          }
        }
      }

      $browser.defer = (callback, timeout) => {
        requestCount += 1;
        const timeoutId = setTimeout(() => {
          delete pendingDeferIds[timeoutId];
          completeRequest(callback);
        }, timeout || 0);
        pendingDeferIds[timeoutId] = true;
        return timeoutId;
      };

      $browser.defer.cancel = (deferId) => {
        if (pendingDeferIds[deferId]) {
          delete pendingDeferIds[deferId];
          clearTimeout(deferId);
          completeRequest(angular.noop);
          return true;
        }
        return false;
      };
    });

    calls = [];
  });

  function throttleValueAndExpectCallsLengthToBe(value, expectedCallsLength, expectedCallsLengthFinally) {
    fn(value).finally(() => {
      expect(calls.length).toEqual(expectedCallsLengthFinally || expectedCallsLength);
    });
    expect(calls.length).toEqual(expectedCallsLength);
  }

  it('test throttle function without trailing', (done) => {
    fn = throttle((id) => {
      calls.push(id);
    }, delay, false);

    $timeout(() => $timeout(() => {
      expect(calls.length).toEqual(0);
      throttleValueAndExpectCallsLengthToBe(1, 1);
      throttleValueAndExpectCallsLengthToBe(2, 1);
      throttleValueAndExpectCallsLengthToBe(3, 1);
    }, delay * 0))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(1);
        throttleValueAndExpectCallsLengthToBe(4, 1);
        throttleValueAndExpectCallsLengthToBe(5, 1);
      }, delay * 0.5))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(1);
        throttleValueAndExpectCallsLengthToBe(6, 2);
        throttleValueAndExpectCallsLengthToBe(7, 2);
      }, delay * 0.5))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(2);
      }, delay * 1.5))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(2);
      }, delay * 1))
      .then(() => $timeout(() => {
        expect(calls).toEqual([1, 6]);
      }, delay * 1))
      .finally(done);
  });

  it('test throttle function with trailing', (done) => {
    fn = throttle((id) => {
      calls.push(id);
    }, delay, true);

    $timeout(() => $timeout(() => {
      expect(calls.length).toEqual(0);
      throttleValueAndExpectCallsLengthToBe(1, 1);
      throttleValueAndExpectCallsLengthToBe(2, 1);
      throttleValueAndExpectCallsLengthToBe(3, 1);
    }, delay * 0))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(1);
        throttleValueAndExpectCallsLengthToBe(4, 1);
        throttleValueAndExpectCallsLengthToBe(5, 1, 2);
      }, delay * 0.5))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(1);
        throttleValueAndExpectCallsLengthToBe(6, 2, 2);
        throttleValueAndExpectCallsLengthToBe(7, 2, 3);
      }, delay * 0.5))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(3);
      }, delay * 1.5))
      .then(() => $timeout(() => {
        expect(calls.length).toEqual(3);
      }, delay * 1))
      .then(() => $timeout(() => {
        expect(calls).toEqual([1, 6, 7]);
      }, delay * 1))
      .finally(done);
  });
});
