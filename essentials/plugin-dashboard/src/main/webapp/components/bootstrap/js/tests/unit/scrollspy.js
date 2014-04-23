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

    module("scrollspy")

      test("should provide no conflict", function () {
        var scrollspy = $.fn.scrollspy.noConflict()
        ok(!$.fn.scrollspy, 'scrollspy was set back to undefined (org value)')
        $.fn.scrollspy = scrollspy
      })

      test("should be defined on jquery object", function () {
        ok($(document.body).scrollspy, 'scrollspy method is defined')
      })

      test("should return element", function () {
        ok($(document.body).scrollspy()[0] == document.body, 'document.body returned')
      })

      test("should switch active class on scroll", function () {
        var sectionHTML = '<div id="masthead"></div>'
          , $section = $(sectionHTML).append('#qunit-fixture')
          , topbarHTML ='<div class="topbar">'
          + '<div class="topbar-inner">'
          + '<div class="container">'
          + '<h3><a href="#">Bootstrap</a></h3>'
          + '<ul class="nav">'
          + '<li><a href="#masthead">Overview</a></li>'
          + '</ul>'
          + '</div>'
          + '</div>'
          + '</div>'
          , $topbar = $(topbarHTML).scrollspy()

        ok($topbar.find('.active', true))
      })

})
