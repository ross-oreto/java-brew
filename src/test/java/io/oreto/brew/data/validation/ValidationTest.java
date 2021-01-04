package io.oreto.brew.data.validation;

import io.oreto.brew.web.page.Form;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ValidationTest {

    @Test
    public void validatePojo() {
        Set<Validator.Invalid> errors = Validator.validate(new Pojo1().withTest("123456").withI(1));
        assertEquals(4, errors.size());

        errors = Validator.validate(new Pojo1().withName("test").withTest("test").withI(2).withB(true));
        assertEquals(0, errors.size());

        errors = Validator.validate(new Pojo1().withName("test"), "name");
        assertEquals(0, errors.size());
    }

    @Test
    public void validateForm() {
        Pojo2 pojo2 = new Pojo2();
        Form<Pojo2> form =
                Form.of(Pojo2.class)
                        .withData(pojo2)
                        .withValidator(Validator.of(pojo2.getName()).check(name -> Objects.equals(name, "test")))
                        .withValidator(Validator.of(pojo2.getI()).check(i -> i >= 2))
                        .withValidator(data -> Validator.of(data.getTest())
                                .property("test").check(test -> Objects.equals(test, "test")))
                        .validate();
        assertFalse(form.isValid());
        assertEquals(6, form.validationErrors().size());
    }

    public static class Pojo1 {
        @Required(group = "pojoForm", property = "newName")
        private String name;

        @Max(4)
        private String test;

        @Min(2)
        private int i;

        @True
        public boolean b;

        public String getName() {
            return name;
        }
        public String getTest() {
            return test;
        }
        public int getI() {
            return i;
        }

        public Pojo1 withName(String name) {
            this.name = name;
            return this;
        }
        public Pojo1 withTest(String test) {
            this.test = test;
            return this;
        }
        public Pojo1 withI(int i) {
            this.i = i;
            return this;
        }

        public Pojo1 withB(boolean b) {
            this.b = b;
            return this;
        }
    }

    public static class Pojo2 {
        @Size(value = 2)
        private String name;
        private String test;
        private int i;

        @Required(message = "validation.id.required")
        @Size(value = 17, message = "validation.id.length")
        private Long id;

        public String getName() {
            return name;
        }
        public String getTest() {
            return test;
        }
        public int getI() {
            return i;
        }
        public Long getId() {
            return id;
        }

        public Pojo2 withName(String name) {
            this.name = name;
            return this;
        }
        public Pojo2 withTest(String test) {
            this.test = test;
            return this;
        }
        public Pojo2 withI(int i) {
            this.i = i;
            return this;
        }

        public Pojo2 withId(Long id) {
            this.id = id;
            return this;
        }
    }
}
