(function () {
    "use strict";
    angular.module('hippo.essentials')
            .controller('freemarkerSyncCtrl', function ($scope, $sce, $log, $rootScope, $http) {
                $scope.endpoint = $rootScope.REST.dynamic + 'freemarkersync/';
                $scope.writeAction = function (action) {
                    var map = Essentials.mapBuilder();
                    angular.forEach($scope.scriptNodes, function (value) {
                        console.log("---------------------------------");
                        console.log(value.selected);
                        if (value.selected) {
                            if (action == "file") {
                                map.put(value.displayValue, value.value);

                            } else {
                                if (value.filePath != null) {
                                    map.put(value.value, value.filePath);
                                }
                            }
                        }
                    });

                    $http.post($scope.endpoint + action, map).success(function (data) {
                        $scope.init();
                    });

                };
                $scope.saveToRepository = function () {
                    $scope.writeAction("repository");
                };
                $scope.saveToFile = function () {
                    $scope.writeAction("file");
                };
                $scope.init = function () {
                    var query = Essentials.queryBuilder("//hst:hst/hst:configurations//element(*, hst:template)[@hst:script]");
                    $http.post($rootScope.REST.jcrQuery, query).success(function (data) {
                        $scope.scriptNodes = [];
                        angular.forEach(data.nodes, function (value) {
                            var myValue = value.path;
                            var displayValue = myValue.replace("/hst:hst/hst:configurations/", "").replace("/hst:templates/", "/");
                            $scope.scriptNodes.push({"value": value.path, "displayValue": displayValue, "selected": false, "filePath": null});
                        });
                    });

                    // get files and match with above paths:
                    $http.get($scope.endpoint).success(function (data) {
                        angular.forEach($scope.scriptNodes, function (node) {
                            angular.forEach(data.items, function (value) {
                                if (value && value.value) {
                                    var fileName = node.displayValue + ".ftl";
                                    var myValue = value.value;
                                    if (myValue.indexOf(fileName, value.value.length - fileName.length) !== -1) {
                                        node.filePath = myValue;
                                    }
                                }
                            });

                        });


                    });

                };
                $scope.init();
            })
})();