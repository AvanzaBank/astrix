'use strict'; // Strict javascript validation

angular.isUndefinedOrNull = function(value) {
	return angular.isUndefined(value) || value === null || value.length === 0;
};

var lunchApp = angular.module("lunchApp", []);

lunchApp.factory("LunchService", function($http) {
	var service = {};
	service.list = function() {
		return $http.get("/lunchrestaurants/");
	}
	service.add = function(restaurant) {
		return $http.post("/lunchrestaurants/", {name: restaurant.name, foodType: restaurant.foodType});
	}
	return service;
});


lunchApp.controller("LunchAppController", function ($scope, LunchService) {
	$scope.restaurants = [];
	$scope.add = LunchService.add
	var getRestaurants = function () {
		var restaurantPromise = LunchService.list();
		restaurantPromise.success(function (data, status) {
			$scope.restaurants = data;
		})
	}
	getRestaurants();
});

lunchApp.filter("StringReplace", function () {
	return function (input, what, whith) {
		return input.replace(what, whith);
	};
})