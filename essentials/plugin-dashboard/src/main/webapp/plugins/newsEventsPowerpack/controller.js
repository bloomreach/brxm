app.controller('newsEventsCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.hideAll = false;
    $scope.installSampleData = false;
    $scope.stepVisible = [true, false];
    $scope.resultMessages = null;
    $scope.selectedDescription = "Please make a selection";
    $scope.packs = null;
    $scope.buttons = [
        {buttonText: "Next", previousIndex: 0, nextIndex: 1}
    ];
    $scope.initCalled = false;
    $scope.init = function () {
        if ($scope.initCalled) {
            console.log("already called");
            return;
        }

        $scope.initCalled = true;
        $scope.packs = {"items": [
            {"enabled": true, "name": "Basic News and Events site", "value": "news-events"},
            {"enabled": false, "name": "A REST only site that contains only REST services and no pages.", "value": "empty-rest"}
        ], "project": {"namespace": "marketplace"}, "steps": [
            {"buttonText": "Next", "name": "Select a powerpack"},
            {"buttonText": "Finish", "name": "Install"}
        ]};
        if (true) {
            return;
        }
        $http({
            method: 'GET',
            url: $rootScope.REST.powerpacks
        }).success(function (data) {
                    $scope.packs = data;

                });

    };

    // TODO fix HTML rendering
    //$scope.trustedContent = $sce.trustAsHtml($scope.resultMessages.message.value);
    $scope.selectChange = function () {


        for (var i = 0; i < $scope.packs.items.length; i++) {
            var powerpack = $scope.packs.items[i];
            if (powerpack.value === $scope.selectedItem) {
                $scope.selectedDescription = powerpack.name;
            }
        }
    };


    $scope.onWizardButton = function (idx) {
        if (idx == 0) {
            $scope.buttons = [
                {buttonText: "Next", previousIndex: 0, nextIndex: 1}

            ];
            $scope.stepVisible = [true, false];
        } else {
            $scope.buttons = [
                {buttonText: "Previous", previousIndex: 0, nextIndex: 1},
                {buttonText: "Finish", previousIndex: 0, nextIndex: 2}
            ];
            $scope.stepVisible = [false, true];
        }

        if (idx == 2) {

            // execute installation:
            $http({
                method: 'GET',
                url: $rootScope.REST.powerpacks_install + $scope.selectedItem + "/" + $scope.installSampleData
            }).success(function (data) {
                        $scope.resultMessages = data;
                        $scope.hideAll = true;
                    });
        }


    };

    $scope.init();

});