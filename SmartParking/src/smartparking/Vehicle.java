package smartparking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Vehicle {
    private String vehicleNo;
    private String vehicleType;
    private LocalDateTime entryTime;

    public Vehicle(String vehicleNo, String vehicleType) {
        if (vehicleNo == null || vehicleNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle number cannot be empty.");
        }
        this.vehicleNo = vehicleNo.trim().toUpperCase();
        this.vehicleType = vehicleType;
        this.entryTime = LocalDateTime.now();
    }

    public String getVehicleNo()    { return vehicleNo; }
    public String getVehicleType()  { return vehicleType; }
    public LocalDateTime getEntryTime() { return entryTime; }

    public String getDetails() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return String.format("Vehicle No: %s | Type: %s | Entry: %s",
                vehicleNo, vehicleType, entryTime.format(fmt));
    }

    @Override
    public String toString() { return vehicleNo + " (" + vehicleType + ")"; }
}
