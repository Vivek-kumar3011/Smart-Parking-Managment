package smartparking;

public class Bike extends Vehicle {
    private static final double RATE_PER_HOUR = 15.0;

    public Bike(String vehicleNo) {
        super(vehicleNo, "Bike");
    }

    public double getRatePerHour() { return RATE_PER_HOUR; }
}
