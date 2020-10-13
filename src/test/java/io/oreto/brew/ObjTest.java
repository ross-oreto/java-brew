package io.oreto.brew;

import io.oreto.brew.obj.Safe;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ObjTest {
    @Test
    public void nullSafe() {
        Ex1 example1 = new Ex1(new Ex2(new Ex3("test")));
        Ex1 example2 = new Ex1(null);

        assertEquals("test", Safe.of(example1).q(Ex1::getEx2).q(Ex2::getEx3).q(Ex3::getName).val());
        assertNull(Safe.of(example2).q(Ex1::getEx2).q(Ex2::getEx3).q(Ex3::getName).val());
        assertEquals("missing", Safe.of(example2).q(Ex1::getEx2).q(Ex2::getEx3).q(Ex3::getName)
                .orElse("missing"));
        assertEquals("foo", Safe.of(example1).q(Ex1::getEx2).q(Ex2::getEx3).q(q->q.noGetter).val());
    }

    class Ex1 {
        private final Ex2 ex2;

        public Ex2 getEx2() { return  ex2; }

        public Ex1(Ex2 ex2) {
            this.ex2 = ex2;
        }
    }

    class Ex2 {
        private final Ex3 ex3;

        public Ex3 getEx3() {
            return ex3;
        }

        public Ex2(Ex3 ex3) {
            this.ex3 = ex3;
        }
    }

    class Ex3 {
        private final String name;
        public String noGetter = "foo";

        public String getName() {
            return name;
        }

        public Ex3(String name) {
            this.name = name;
        }
    }
}
