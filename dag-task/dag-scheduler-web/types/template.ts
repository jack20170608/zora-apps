export interface TaskTemplate {
  id?: number;
  templateKey: string;
  templateName: string;
  description: string;
  version: string;
  active: boolean;
  dagDefinition: string; // JSON string of DAG definition
  parameterSchema: string; // JSON string of parameter schema
  createDt?: string;
  lastUpdateDt?: string;
  versionSeq?: number;
}

export interface TemplateListResponse {
  code: number;
  message: string;
  data: TaskTemplate[];
}

export interface TemplateResponse {
  code: number;
  message: string;
  data: TaskTemplate;
}
