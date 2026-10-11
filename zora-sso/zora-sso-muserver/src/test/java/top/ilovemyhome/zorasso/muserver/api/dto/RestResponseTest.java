package top.ilovemyhome.zorasso.muserver.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestResponseTest {

    @Test
    void createsSuccessfulResponse() {
        Map<String, String> data = Map.of("status", "UP");

        RestResponse<Map<String, String>> response = RestResponse.success(data);

        assertAll("successful response",
            () -> assertTrue(response.success()),
            () -> assertEquals(data, response.data()),
            () -> assertNull(response.message())
        );
    }

    @Test
    void createsFailureResponse() {
        RestResponse<Object> response = RestResponse.failure("invalid_request");

        assertAll("failed response",
            () -> assertFalse(response.success()),
            () -> assertNull(response.data()),
            () -> assertEquals("invalid_request", response.message())
        );
    }

    @Test
    void rejectsInvalidFailureState() {
        assertThrowsExactly(
            IllegalArgumentException.class,
            () -> new RestResponse<>(false, Map.of("unexpected", "data"), "failed")
        );
        assertThrowsExactly(
            IllegalArgumentException.class,
            () -> RestResponse.failure(" ")
        );
    }
}
