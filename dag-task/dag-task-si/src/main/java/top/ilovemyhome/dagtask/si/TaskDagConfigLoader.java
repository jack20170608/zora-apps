package top.ilovemyhome.dagtask.si;

import top.ilovemyhome.dagtask.si.TaskOrder;

import java.io.File;

/**
 * Interface for loading DAG task workflow configurations from YAML files.
 */
public interface TaskDagConfigLoader {

    /**
     * Load a DAG workflow configuration from a classpath resource.
     *
     * @param resourcePath the path to the resource on the classpath
     * @return the loaded TaskOrder ready for persistence
     * @throws DagConfigurationException if validation fails
     */
    TaskOrder loadFromClasspath(String resourcePath);

    /**
     * Load a DAG workflow configuration from a filesystem file.
     *
     * @param file the file to load
     * @return the loaded TaskOrder ready for persistence
     * @throws DagConfigurationException if validation fails
     */
    TaskOrder loadFromFile(File file);

    /**
     * Load a DAG workflow configuration from YAML content string.
     *
     * @param yamlContent the YAML content string
     * @return the loaded TaskOrder ready for persistence
     * @throws DagConfigurationException if validation fails
     */
    TaskOrder loadFromYaml(String yamlContent);
}
