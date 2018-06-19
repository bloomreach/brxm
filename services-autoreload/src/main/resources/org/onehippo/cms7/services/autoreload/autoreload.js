/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
(function(window, console) {

  var AUTO_RELOAD_PATH = "/autoreload",
    CONTEXT_PATH = "/site",
    RECONNECT_DELAY_MILLIS = 5000,
    MAX_RECONNECT_ATTEMPTS = 120,  // retry for 120 * 5000 ms = 10 minutes
    hasBeenConnected = false,
    isReloadingPage,
    isReconnecting,
    reconnectAttempts = 0,
    websocket;

  function serverUrl() {
    return "ws://" + document.location.host + CONTEXT_PATH + AUTO_RELOAD_PATH;
  }

  function log(msg) {
    if (typeof console.log === "function") {
      console.log("Hippo auto-reload " + msg);
    }
  }

  function reloadPage() {
    log("is reloading page...");
    isReloadingPage = true;
    window.document.location.reload();
  }

  function onOpen() {
    isReloadingPage = false;
    isReconnecting = false;
    hasBeenConnected = true;
    log("connected");
  }

  function onMessage(event) {
    var message = JSON.parse(event.data);
    if (message.command === "reloadPage") {
      reloadPage();
    } else {
      log("received unknown message:", message);
    }
  }

  function onError(event) {
    if (!isReconnecting) {
      var warning = "error";
      if (event.data) {
        warning += ": " + event.data;
      }
      log(warning);
    }
  }

  function onClose(event) {
    if (!isReloadingPage && hasBeenConnected) {
      reconnect();
    }
  }

  function connect() {
    log("connecting...");
    websocket = new window.WebSocket(serverUrl());
    websocket.onopen = onOpen;
    websocket.onmessage = onMessage;
    websocket.onerror = onError;
    websocket.onclose = onClose;
  }

  function disconnect() {
    if (websocket) {
      isReloadingPage = true; // prevent reconnect
      websocket.close();
    }
  }

  function reconnect() {
    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
      isReconnecting = true;
      reconnectAttempts++;
      log("disconnected, trying to reconnect...");
      window.setTimeout(connect, RECONNECT_DELAY_MILLIS);
    } else {
      isReconnecting = false;
      log("stopped trying to reconnect.");
    }
  }

  if (window.addEventListener && window.WebSocket) {
    window.addEventListener("load", connect);
    window.addEventListener("beforeunload", disconnect);
  } else {
    log("is not available because this browser does not support WebSockets");
  }

}(window, console));
