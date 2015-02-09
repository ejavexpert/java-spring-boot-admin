/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

module.exports = function ($scope, application, ApplicationTrace, $interval) {
    $scope.lastTraceTime = 0;
    $scope.traces = [];
    $scope.refresher = null;
    $scope.refreshInterval = 5;

    $scope.refresh = function () {
        ApplicationTrace.getTraces(application)
            .success(function (traces) {
                for (var i = 0; i < traces.length; i++) {
                    if (traces[i].timestamp > $scope.lastTraceTime) {
                        $scope.traces.push(traces[i]);
                    }
                }
                if (traces.length > 0) {
                    $scope.lastTraceTime = traces[traces.length - 1].timestamp;
                }

            })
            .error(function (error) {
                $scope.error = error;
            });
    };

    $scope.toggleAutoRefresh = function() {
        if ($scope.refresher === null) {
            $scope.refresher = $interval(function () {
                $scope.refresh();
            }, $scope.refreshInterval * 1000);
        } else {
            $interval.cancel($scope.refresher);
            $scope.refresher = null;
        }
    };

    $scope.refresh();
};
