/*
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

async function loadImage(image, onLazyLoadCallback) {

  const loadingAttribute = image.getAttribute('loading');
  const isLazy = loadingAttribute && loadingAttribute.trim().toLowerCase() === 'lazy';

  return window.$Promise((resolve, reject) => {

    function fulfill() {
      if (isLazy) {
        onLazyLoadCallback(image);
      } else if (image.naturalWidth) {
        resolve(image);
      } else {
        reject(image);
      }

      image.removeEventListener('load', fulfill);
      image.removeEventListener('error', fulfill);
    }

    if (image.naturalWidth) {
      // If the browser can determine the naturalWidth the image is already loaded successfully
      resolve(image);
    } else if (image.complete) {
      // If the image is complete but the naturalWidth is 0px it is probably broken
      reject(image);
    } else {
      image.addEventListener('load', fulfill);
      image.addEventListener('error', fulfill);

      if (isLazy) {
        // resolve the main promise for lazy-load images immediately
        resolve(image);
      }
    }
  });
}

async function loadImages(input, onLazyLoadCallback) {
  if (input.length === undefined || input.length === 0) {
    // if there are no images to wait for, we immediately resolve
    return window.$Promise.resolve();
  }

  // otherwise, wait for each image to load completely before resolving
  // errors are caught and stored in the promise result
  return window.$Promise.all(input.map((img) => loadImage(img, onLazyLoadCallback).catch((error) => error)));
}


/**
 * Wait for all image elements in the "images" array to finish loading before calling "onLoadCallback".
 *
 * If an image has attribute loading="lazy", the "onLazyLoadCallback" will be used to issue an async callback. This
 * means that the callback is invoked once the browser determines that the image should be loaded (e.g. when the user
 * scrolls). This does introduce a bit of overhead for lazy-load images that are already visible to the user as it will
 * trigger multiple callbacks.
 *
 * @param {*} images The images to wait for
 * @param {*} onLoadCallback  Callback to invoke once all images have finished loading
 * @param {*} onLazyLoadCallback  Callback to invoke once a "lazy-load" image has finished loading
 */
export async function waitForImagesToLoad(images, onLoadCallback, onLazyLoadCallback = () => {}) {
  try {
    await loadImages(images.get(), onLazyLoadCallback);
  } finally {
    onLoadCallback();
  }

}
