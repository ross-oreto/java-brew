package io.oreto.brew;

import io.oreto.brew.collections.Lists;
import io.oreto.brew.obj.Reflect;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class ReflectTest {

    @Test
    public void setter1() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Setter setter = new Setter();

        Reflect.setFieldValue(setter, "test", "hello");
        Assert.assertEquals("hello", Reflect.getFieldValue(setter, "test"));
    }

    @Test
    public void setter2() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Setter setter = new Setter();

        Reflect.setFieldValue(setter, "i", 20);
        Assert.assertEquals(20, Reflect.getFieldValue(setter, "i"));
    }

    @Test
    public void copy() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Setter setter1 = new SubSetter("s1", 1);
        Setter setter2 = new SubSetter("s2", 2);

        Reflect.copy(setter1, setter2, "i");
        Assert.assertEquals(2, setter1.getI());

        setter1 = new SubSetter("s1", 1);
        setter2 = new SubSetter("s2", 2);
        Reflect.copy(setter1, setter2, Lists.of("i"), Reflect.CopyOptions.create().exclusion());
        Assert.assertEquals(1, setter1.getI());
        Assert.assertEquals("s2", setter1.getTest());
    }

    public static class Setter {
        private String test;
        private int i;

        public Setter(String test, int i) {
            this.test = test;
            this.i = i;
        }

        public Setter() { }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    public static class SubSetter extends Setter {

        public SubSetter(String s1, int i) {
            super(s1, i);
        }
    }
}
