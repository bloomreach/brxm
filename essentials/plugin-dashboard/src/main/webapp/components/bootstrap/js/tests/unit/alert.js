/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

$(function () {

    module("alert")

      test("should provide no conflict", function () {
        var alert = $.fn.alert.noConflict()
        ok(!$.fn.alert, 'alert was set back to undefined (org value)')
        $.fn.alert = alert
      })

      test("should be defined on jquery object", function () {
        ok($(document.body).alert, 'alert method is defined')
      })

      test("should return element", function () {
        ok($(document.body).alert()[0] == document.body, 'document.body returned')
      })

      test("should fade element out on clicking .close", function () {
        var alertHTML = '<div class="alert-message warning fade in">'
          + '<a class="close" href="#" data-dismiss="alert">×</a>'
          + '<p><strong>Holy guacamole!</strong> Best check yo self, you\'re not looking too good.</p>'
          + '</div>'
          , alert = $(alertHTML).alert()

        alert.find('.close').click()

        ok(!alert.hasClass('in'), 'remove .in class on .close click')
      })

      test("should remove element when clicking .close", function () {
        $.support.transition = false

        var alertHTML = '<div class="alert-message warning fade in">'
          + '<a class="close" href="#" data-dismiss="alert">×</a>'
          + '<p><strong>Holy guacamole!</strong> Best check yo self, you\'re not looking too good.</p>'
          + '</div>'
          , alert = $(alertHTML).appendTo('#qunit-fixture').alert()

        ok($('#qunit-fixture').find('.alert-message').length, 'element added to dom')

        alert.find('.close').click()

        ok(!$('#qunit-fixture').find('.alert-message').length, 'element removed from dom')
      })

      test("should not fire closed when close is prevented", function () {
        $.support.transition = false
        stop();
        $('<div class="alert"/>')
          .on('close.bs.alert', function (e) {
            e.preventDefault();
            ok(true);
            start();
          })
          .on('closed.bs.alert', function () {
            ok(false);
          })
          .alert('close')
      })

})
