package top.ilovemyhome.dagtask.server.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record FooUser(Long id, String name, int age, LocalDate birthday, LocalDateTime lastUpdateDt, YearMonth salaryMonth) {

        public static Builder builder(FooUser user){
            Builder builder = new Builder();
            builder.id(user.id)
              .name(user.name)
              .age(user.age)
              .birthday(user.birthday)
              .lastUpdateDt(user.lastUpdateDt)
              .salaryMonth(user.salaryMonth);
            return builder;
        }

        public static class Builder {
            private Long id;
            private String name;
            private int age;
            private LocalDate birthday;
            private LocalDateTime lastUpdateDt;
            private YearMonth salaryMonth;

            // Builder method to set the id
            public Builder id(Long id) {
                this.id = id;
                return this;
            }

            // Builder method to set the name
            public Builder name(String name) {
                this.name = name;
                return this;
            }

            // Builder method to set the age
            public Builder age(int age) {
                this.age = age;
                return this;
            }

            // Builder method to set the birthday
            public Builder birthday(LocalDate birthday) {
                this.birthday = birthday;
                return this;
            }

            // Builder method to set the lastUpdateDt
            public Builder lastUpdateDt(LocalDateTime lastUpdateDt) {
                this.lastUpdateDt = lastUpdateDt;
                return this;
            }

            // Builder method to set the salaryMonth
            public Builder salaryMonth(YearMonth salaryMonth) {
                this.salaryMonth = salaryMonth;
                return this;
            }

            public FooUser build(){
                return new FooUser(id, name, age, birthday, lastUpdateDt, salaryMonth);
            }

        }
    }
