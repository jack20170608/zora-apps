package top.ilovemyhome.dagtask.core.si;

import java.time.LocalDate;
import java.util.List;

public record Foo(Long id, List<Bar> barList, LocalDate someDate) {}
