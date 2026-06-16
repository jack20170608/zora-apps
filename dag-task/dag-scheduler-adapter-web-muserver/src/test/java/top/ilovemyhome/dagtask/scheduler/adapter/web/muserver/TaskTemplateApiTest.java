package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskTemplateUseCase;
import top.ilovemyhome.dagtask.si.TaskTemplate;

class TaskTemplateApiTest {

    @Test
    void create_whenUseCaseReturnsFalse_returnsBadRequest() {
        ManageTaskTemplateUseCase manage = mock(ManageTaskTemplateUseCase.class);
        QueryTaskTemplateUseCase query = mock(QueryTaskTemplateUseCase.class);
        TaskTemplate template = TaskTemplate.builder()
            .withTemplateKey("daily-job")
            .withTemplateName("Daily Job")
            .withVersion("v1")
            .withDagDefinition("{}")
            .build();
        when(manage.createTemplate(template, true)).thenReturn(false);

        Response response = new TaskTemplateApi(query, manage).create(template, true);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(manage).createTemplate(template, true);
    }
}
