'use strict'; // Strict javascript validation

angular.isUndefinedOrNull = function(value) {
	return angular.isUndefined(value) || value === null || value.length === 0;
};

var AsterixDashboard = angular.module("AsterixDashboard", []);

AsterixDashboard.factory("ServiceRegistry", [ "$http", function($http) {
	var service = {};

	service.list = function() {
		return $http.get("/services/");
	}

//	service.loadFiltered = function(appsToShow) {
//		return $http.get("/livelyness", {
//			'params' : {
//				'appsToShow' : appsToShow
//			}
//		});
//	}

	return service;

} ]);



AsterixDashboard.controller("ServiceRegistryController", ["$scope", "ServiceRegistry", "$interval", function ($scope, ServiceRegistry, $interval) {
	$scope.services = [];
	var getServices = function () {
		var servicesPromise = ServiceRegistry.list();
		servicesPromise.success(function (data, status) {
			$scope.services = data;
		})
	}
	var init = function() {
		getServices();
		$interval(getServices, 10000);
		
	}
	init();
}]);

AsterixDashboard.filter("StringReplace", function () {
	return function (input, what, whith) {
		return input.replace(what, whith);
	};
})