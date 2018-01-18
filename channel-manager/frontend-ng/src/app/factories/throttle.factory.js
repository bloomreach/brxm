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

export default function throttle($q, $timeout) {
  return (callback, threshhold, trailing) => {
    threshhold = threshhold || 250;

    let last;
    let promise;

    return (...args) => {
      let now = +new Date();

      $timeout.cancel(promise);
      if (last && now < (last + threshhold)) {
        if (trailing) {
          promise = $timeout(() => {
            now = +new Date();
            last = now;
            return callback(...args);
          }, threshhold);
        }
        return promise;
      }
      const deferred = $q.defer();
      last = now;
      deferred.resolve(callback(...args));
      promise = deferred.promise;
      return promise;
    };
  };
}
