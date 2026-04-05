package top.ilovemyhome.dagtask.si;

import java.util.Map;

public record TaskInput(Long taskId, Object input, Map<String, String> attributes) {}
