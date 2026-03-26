package smartparking;

public class Car extends Vehicle {
    private static final double RATE_PER_HOUR = 30.0;

    public Car(String vehicleNo) {
        super(vehicleNo, "Car");
    }

    public double getRatePerHour() { return RATE_PER_HOUR; }
}
