package top.ilovemyhome.hosthelper.muserver.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

public class FooUserTest {

    @Test
    void testFooUserRecordAndBuilder() {
        // Test with all fields populated
        LocalDate birthday = LocalDate.of(1990, 5, 15);
        LocalDateTime lastUpdate = LocalDateTime.now();
        YearMonth salaryMonth = YearMonth.of(2023, 11);

        FooUser user1 = new FooUser(1L, "John Doe", 30, birthday, lastUpdate, salaryMonth);

        assertEquals(1L, user1.id());
        assertEquals("John Doe", user1.name());
        assertEquals(30, user1.age());
        assertEquals(birthday, user1.birthday());
        assertEquals(lastUpdate, user1.lastUpdateDt());
        assertEquals(salaryMonth, user1.salaryMonth());

        // Test builder with all fields
        FooUser.Builder builder = FooUser.builder(user1);
        FooUser userFromBuilder = builder.build();

        assertEquals(user1, userFromBuilder);

        // Test builder with partial fields
        FooUser partialFooUser = new FooUser.Builder()
                .id(2L)
                .name("Jane Smith")
                .age(25)
                .build();

        assertEquals(2L, partialFooUser.id());
        assertEquals("Jane Smith", partialFooUser.name());
        assertEquals(25, partialFooUser.age());
        assertNull(partialFooUser.birthday());
        assertNull(partialFooUser.lastUpdateDt());
        assertNull(partialFooUser.salaryMonth());

        // Test edge cases
        FooUser edgeCaseFooUser = new FooUser.Builder()
                .id(Long.MAX_VALUE)
                .name("")  // empty name
                .age(0)    // minimum age
                .birthday(LocalDate.MIN)
                .lastUpdateDt(LocalDateTime.MIN)
                .salaryMonth(YearMonth.of(1970, 1))  // Unix epoch start
                .build();

        assertEquals(Long.MAX_VALUE, edgeCaseFooUser.id());
        assertEquals("", edgeCaseFooUser.name());
        assertEquals(0, edgeCaseFooUser.age());
        assertEquals(LocalDate.MIN, edgeCaseFooUser.birthday());
        assertEquals(LocalDateTime.MIN, edgeCaseFooUser.lastUpdateDt());
        assertEquals(YearMonth.of(1970, 1), edgeCaseFooUser.salaryMonth());

        // Test null values
        FooUser nullFooUser = new FooUser.Builder()
                .id(null)
                .name(null)
                .build();

        assertNull(nullFooUser.id());
        assertNull(nullFooUser.name());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDate birthday = LocalDate.of(1985, 8, 20);
        FooUser user1 = new FooUser(1L, "Alice", 35, birthday, null, null);
        FooUser user2 = new FooUser(1L, "Alice", 35, birthday, null, null);
        FooUser user3 = new FooUser(2L, "Bob", 40, birthday, null, null);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1, user3);
    }
}
