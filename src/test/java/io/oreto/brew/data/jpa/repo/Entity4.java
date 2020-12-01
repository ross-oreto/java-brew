package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Entity
public class Entity4 implements Model<Entity4.CompId> {

    @EmbeddedId
    private CompId notCalledId;
    private String test;

    public Entity4(CompId id, String test) {
        this.notCalledId = id;
        this.test = test;
    }

    public Entity4() { }

    @Override
    public String idName() {
        return "notCalledId";
    }

    public CompId getNotCalledId() {
        return notCalledId;
    }

    public void setNotCalledId(CompId notCalledId) {
        this.notCalledId = notCalledId;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    @Embeddable
    public static class CompId implements Serializable {
        public static CompId of(Integer i, String s) {
            return new CompId(i, s);
        }

        @NotNull
        private Integer i;

        @NotNull
        private String s;

        public CompId() { }

        public CompId(@NotNull Integer i, @NotNull String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, s);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof CompId)) {
                return false;
            }
            CompId CompId = (CompId) o;
            return Objects.equals(CompId.getI(), getI())
                    && Objects.equals(CompId.getS(), getS());
        }

        public Integer getI() {
            return i;
        }

        public void setI(Integer i) {
            this.i = i;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return String.format("%s, %s", i, s);
        }
    }
}
