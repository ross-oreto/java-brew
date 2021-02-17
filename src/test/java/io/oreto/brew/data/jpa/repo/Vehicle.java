package io.oreto.brew.data.jpa.repo;

import io.oreto.brew.data.Model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table
@IdClass(Vehicle.VehicleId.class)
public class Vehicle implements Model<Vehicle.VehicleId> {
    private static final long serialVersionUID = 1L;

    @Id private String make;
    @Id private String model;

    @Override
    public VehicleId getId() {
        return new VehicleId(make, model);
    }

    @Override
    public void setId(VehicleId vehicleId) {
        this.make = vehicleId.make;
        this.model = vehicleId.model;
    }

    @OneToOne(cascade = CascadeType.PERSIST) Tire tire;

    public Vehicle withMake(String make) {
        this.make = make;
        return this;
    }

    public Vehicle withModel(String model) {
        this.model = model;
        return this;
    }

    public Vehicle withTire(Tire tire) {
        this.tire = tire;
        return this;
    }

    public static class VehicleId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String make;
        private String model;

        public VehicleId() {}

        public VehicleId(String make, String model) {
            this.make = make;
            this.model = model;
        }

        public String getMake() {
            return make;
        }

        public void setMake(String make) {
            this.make = make;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getMake(), getModel());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof VehicleId)) {
                return false;
            }
            VehicleId vehicleId = (VehicleId) o;
            return Objects.equals(vehicleId.getMake(), getMake())
                    && Objects.equals(vehicleId.getModel(), getModel());
        }
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Tire getTire() {
        return tire;
    }

    public void setTire(Tire tire) {
        this.tire = tire;
    }
}
