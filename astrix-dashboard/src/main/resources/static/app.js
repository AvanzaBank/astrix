'use strict'; // Strict javascript validation

angular.isUndefinedOrNull = function(value) {
	return angular.isUndefined(value) || value === null || value.length === 0;
};

var AstrixDashboard = angular.module("AstrixDashboard", []);

AstrixDashboard.factory("ServiceRegistry", [ "$http", function($http) {
	var service = {};

	service.list = function() {
		return $http.get("/services/");
	}

	return service;

} ]);



AstrixDashboard.controller("ServiceRegistryController", ["$scope", "ServiceRegistry", "$interval", function ($scope, ServiceRegistry, $interval) {
	$scope.services = [];
	var getServices = function () {
		var servicesPromise = ServiceRegistry.list();
		servicesPromise.success(function (data, status) {
			$scope.services = data;
		})
	}
	var init = function() {
		getServices();
		$interval(getServices, 1000);
		
	}
	init();
}]);

AstrixDashboard.filter("StringReplace", function () {
	return function (input, what, whith) {
		return input.replace(what, whith);
	};
})