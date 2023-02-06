import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {url} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class PlanDefinitionService {

  constructor(private http: HttpClient) { }

  getAllPlanDefinitions():Observable<any>{
    return this.http.get(url.PlanDefinition);
  }

  getPlanDefinitionsById(id:number):Observable<any>{
    return this.http.get(url.PlanDefinition + id);
  }
}
