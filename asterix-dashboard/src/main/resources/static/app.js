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



AsterixDashboard.controller("ServiceRegistryController", ["$scope", "ServiceRegistry", function ($scope, ServiceRegistry) {
	$scope.services = [];
	var init = function() {
		var servicesPromise = ServiceRegistry.list();
		servicesPromise.success(function (data, status) {
			$scope.services = data;
		});
	}
	init();
}]);
