

app.controller('newsEventsCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.pageModel = WM.createModel();
    $scope.installSampleData = false;
    $scope.stepVisible = $scope.pageModel.getSelectedArray();
    $scope.resultMessages = null;
    $scope.selectedDescription = "Please make a selection";
    $scope.packs = null;

    $scope.init = function () {

        $scope.packs = {"items": [
            {"enabled": true, "name": "Basic News and Events site", "value": "news-events"},
            {"enabled": false, "name": "A REST only site that contains only REST services and no pages.", "value": "empty-rest"}
        ], "project": {"namespace": "marketplace"}, "steps": [
            {"buttonText": "Next", "name": "Select a powerpack"},
            {"buttonText": "Next", "name": "Select another powerpack"},
            {"buttonText": "Finish", "name": "Install"}
        ]};
        var length = $scope.packs.steps.length;
        for (var i = 0; i < length; i++) {
            var step = $scope.packs.steps[i];
            $scope.pageModel.addPage(step.buttonText);

        }
        $scope.pageModel.setSelected(0);


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


    $scope.onWizardButton = function (index) {


        // execute installation:
        $http({
            method: 'GET',
            url: $rootScope.REST.powerpacks_install + $scope.selectedItem + "/" + $scope.installSampleData
        }).success(function (data) {
                    $scope.resultMessages = data;
                    $scope.stepVisible = [false, true];
                });
    };

    $scope.init();

})