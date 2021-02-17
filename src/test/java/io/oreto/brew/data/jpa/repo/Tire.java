package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table
public class Tire implements Model<Tire.TireId> {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private TireId id;

    public Tire withId(TireId id) {
        this.id = id;
        return this;
    }

    @Embeddable
    public static class TireId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String make;
        private Integer size;

        public TireId() { }

        public TireId(String make, Integer size) {
            this.make = make;
            this.size = size;
        }

        public String getMake() {
            return make;
        }

        public void setMake(String make) {
            this.make = make;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getMake(), getSize());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof TireId)) {
                return false;
            }
            TireId tireId = (TireId) o;
            return Objects.equals(tireId.getMake(), getMake())
                    && Objects.equals(tireId.getSize(), getSize());
        }
    }

    public TireId getId() {
        return id;
    }

    public void setId(TireId id) {
        this.id = id;
    }
}
