/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
describe('IFrameConnections', function () {

  let iFrameConnections, childApi, parentApi, promise, parentApiPromise;
  let subAppLocation, subAppFlags;

  beforeEach(function () {
      childApi = {
        navigate: function (location, flags) {
          subAppLocation = location;
          subAppFlags = flags;
        },
      };
      parentApi = {
        navigate: function (location, flags) {
        }
      };
      parentApiPromise = MakeQuerablePromise(new Promise(function (resolve, reject) {
        resolve(parentApi)
      }));
      promise = MakeQuerablePromise(new Promise(function (resolve, reject) {
        resolve(childApi);
      }));
      iFrameConnections = new Hippo.IFrameConnections(parentApiPromise);
    }
  );

  describe('registerIFrame', function () {
    it('adds an entry with identifier as key to the connections map', function () {
      const iFrameElement = {className: 'identifier'}
      iFrameConnections.registerIframe(iFrameElement).then(
        () => expect(iFrameConnections.getConnections().has(iFrameElement)).toBe(true)
      )
    });
    it('adds an entry with as value a promise', function () {
      let iframeElement = {className: 'identifier'};
      iFrameConnections.registerIframe(iframeElement).then(() => {
        let value = iFrameConnections.getChildApiPromise(iframeElement);
        expect(value).toEqual(promise);
      });
    });
  });

  function MakeQuerablePromise (promise) {
    // Don't modify any promise that has been already modified.
    if (promise.isResolved) {
      return promise;
    }

    // Set initial state
    var isPending = true;
    var isRejected = false;
    var isFulfilled = false;

    // Observe the promise, saving the fulfillment in a closure scope.
    var result = promise.then(
      function (v) {
        isFulfilled = true;
        isPending = false;
        return v;
      },
      function (e) {
        isRejected = true;
        isPending = false;
        throw e;
      }
    );

    result.isFulfilled = function () {
      return isFulfilled;
    };
    result.isPending = function () {
      return isPending;
    };
    result.isRejected = function () {
      return isRejected;
    };
    return result;
  }

})
;
