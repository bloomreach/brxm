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

    module("tabs")

      test("should provide no conflict", function () {
        var tab = $.fn.tab.noConflict()
        ok(!$.fn.tab, 'tab was set back to undefined (org value)')
        $.fn.tab = tab
      })

      test("should be defined on jquery object", function () {
        ok($(document.body).tab, 'tabs method is defined')
      })

      test("should return element", function () {
        ok($(document.body).tab()[0] == document.body, 'document.body returned')
      })

      test("should activate element by tab id", function () {
        var tabsHTML =
            '<ul class="tabs">'
          + '<li><a href="#home">Home</a></li>'
          + '<li><a href="#profile">Profile</a></li>'
          + '</ul>'

        $('<ul><li id="home"></li><li id="profile"></li></ul>').appendTo("#qunit-fixture")

        $(tabsHTML).find('li:last a').tab('show')
        equals($("#qunit-fixture").find('.active').attr('id'), "profile")

        $(tabsHTML).find('li:first a').tab('show')
        equals($("#qunit-fixture").find('.active').attr('id'), "home")
      })

      test("should activate element by tab id", function () {
        var pillsHTML =
            '<ul class="pills">'
          + '<li><a href="#home">Home</a></li>'
          + '<li><a href="#profile">Profile</a></li>'
          + '</ul>'

        $('<ul><li id="home"></li><li id="profile"></li></ul>').appendTo("#qunit-fixture")

        $(pillsHTML).find('li:last a').tab('show')
        equals($("#qunit-fixture").find('.active').attr('id'), "profile")

        $(pillsHTML).find('li:first a').tab('show')
        equals($("#qunit-fixture").find('.active').attr('id'), "home")
      })


      test("should not fire closed when close is prevented", function () {
        $.support.transition = false
        stop();
        $('<div class="tab"/>')
          .on('show.bs.tab', function (e) {
            e.preventDefault();
            ok(true);
            start();
          })
          .on('shown.bs.tab', function () {
            ok(false);
          })
          .tab('show')
      })

      test("show and shown events should reference correct relatedTarget", function () {
        var dropHTML =
            '<ul class="drop">'
          + '<li class="dropdown"><a data-toggle="dropdown" href="#">1</a>'
          + '<ul class="dropdown-menu">'
          + '<li><a href="#1-1" data-toggle="tab">1-1</a></li>'
          + '<li><a href="#1-2" data-toggle="tab">1-2</a></li>'
          + '</ul>'
          + '</li>'
          + '</ul>'

        $(dropHTML).find('ul>li:first a').tab('show').end()
          .find('ul>li:last a').on('show', function(event){
            equals(event.relatedTarget.hash, "#1-1")
          }).on('shown', function(event){
            equals(event.relatedTarget.hash, "#1-1")
          }).tab('show')
      })

})
