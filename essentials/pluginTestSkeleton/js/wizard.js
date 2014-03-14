(function () {
    "use strict";
    angular.module('hippo.essentials').directive("wizard", function () {
        return {
            replace: true,
            restrict: 'E',
            scope: {
                label: '@',
                options: '=',
                selectedDescription: '=',
                ngModel: '=',
                onSelect: '&'
            },

            link: function (scope, element, attrs, ctrl) {
                scope.onPowerpackSelect = function () {
                    scope.onSelect();
                }
            },

            template: '<div><select  ng-required="true" ng-selected="onPowerpackSelect()" ng-model="ngModel">' +
                    ' <option ng-repeat="option in options" value="{{option.pluginId}}" ng-disabled="!option.enabled">{{option.name}}</option>' +
                    '</select>' +
                    '<div class="clearfix sep-10">&nbsp;</div>' +
                    '<div id="option.description">{{selectedDescription}}</div>' +
                    '<div class="clearfix sep-10">&nbsp;</div>' +
                    '</div>'
        };
    })


})();