package top.ilovemyhome.dagtask.agent.log;

import top.ilovemyhome.dagtask.si.TaskLogWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory implementation of {@link TaskLogWriter} for testing.
 */
class InMemoryTaskLogWriter implements TaskLogWriter {

    private final List<String> infos = new ArrayList<>();
    private final List<String> warns = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    @Override
    public void info(String message) {
        infos.add(message);
    }

    @Override
    public void warn(String message) {
        warns.add(message);
    }

    @Override
    public void error(String message) {
        errors.add(message);
    }

    @Override
    public void stdout(String message) {
    }

    @Override
    public void stderr(String message) {
    }

    @Override
    public void close() {
    }

    public List<String> getInfos() {
        return infos;
    }

    public List<String> getWarns() {
        return warns;
    }

    public List<String> getErrors() {
        return errors;
    }
}
