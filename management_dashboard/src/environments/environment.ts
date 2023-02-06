// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  baseApiUrl: "http://localhost:8180/fhir/",
 };

export const url = {
    PlanDefinition: environment.baseApiUrl + "PlanDefinition/",
    Task: environment.baseApiUrl + "Task" + "?status=completed&_count=10000",
    CarePlanStatus: environment.baseApiUrl + "CarePlan?instantiates-canonical=PlanDefinition/",
    Patient: environment.baseApiUrl + "Patient",
};

//CarePlan?status=completed&instantiates-canonical=PlanDefinition/
/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
