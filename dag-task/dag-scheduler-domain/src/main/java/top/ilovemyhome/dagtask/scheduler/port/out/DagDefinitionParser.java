package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition;

import java.util.Map;

/**
 * Outbound port: parses raw JSON strings (stored in {@code TaskTemplate.dagDefinition}
 * and {@code TaskTemplate.parameterSchema}) into typed domain values.
 * <p>
 * The implementation lives in an adapter (e.g. {@code JacksonDagDefinitionParser} in
 * dag-allinone-muserver / dag-scheduler-app), keeping the pure domain free from Jackson
 * and other JSON libraries.
 * </p>
 * <p>
 * Note: this port references {@link DagDefinition} which is defined inside
 * {@link top.ilovemyhome.dagtask.scheduler.port.in.SubmitDagRunUseCase}. This is a
 * pragmatic choice to avoid duplicating the record; it will be resolved when
 * DagDefinition/TaskDefinition are extracted to their own package in step 3.
 * </p>
 */
public interface DagDefinitionParser {

    /** Parse the raw JSON DAG definition string into a typed record. */
    DagDefinition parseDag(String json);

    /**
     * Parse the parameter schema JSON and return the default values for each parameter.
     *
     * @param parameterSchemaJson the JSON schema describing template parameters
     * @return map of parameter name to default value (empty map if schema is null/blank)
     */
    Map<String, String> parseParameterDefaults(String parameterSchemaJson);
}