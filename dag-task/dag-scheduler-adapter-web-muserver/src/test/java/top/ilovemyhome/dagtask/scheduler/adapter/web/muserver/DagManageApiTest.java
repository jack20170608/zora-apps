package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.scheduler.port.in.InstantiateDagTemplateUseCase;

class DagManageApiTest {

    @Test
    void instantiate_whenTemplateMissing_returnsBadRequest() {
        InstantiateDagTemplateUseCase useCase = mock(InstantiateDagTemplateUseCase.class);
        when(useCase.instantiateFromTemplate("tpl", "order-1", "Order 1", Map.of()))
            .thenReturn(Optional.empty());

        Response response = new DagManageApi(useCase).instantiate("tpl", "order-1", "Order 1", Map.of());

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
