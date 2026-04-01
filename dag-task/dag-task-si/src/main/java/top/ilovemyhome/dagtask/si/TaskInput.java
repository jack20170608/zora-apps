package top.ilovemyhome.dagtask.si;

import java.util.Map;

public record TaskInput<I>(Long taskId, I input, Map<String, String> attributes) {}
