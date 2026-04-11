package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface DagManageService {



    List<Long> createTasks(List<TaskRecord> records);



    void createTasksFromDagDefinition(String dagDefinitionJson, String orderKey,
                                       Map<String, String> parameters)
            throws com.fasterxml.jackson.core.JsonProcessingException;


    Optional<TaskOrder> instantiateFromTemplate(
            String templateKey, String orderKey, String orderName,
            Map<String, String> parameters);


    Optional<TaskOrder> instantiateFromTemplate(
            String templateKey, String version, String orderKey, String orderName,
            Map<String, String> parameters);
}
