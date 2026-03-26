package smartparking;

public class ParkingSlot {
    private int slotNumber;
    private boolean isOccupied;
    private Vehicle parkedVehicle;
    private String slotType; // "Car" or "Bike"

    public ParkingSlot(int slotNumber, String slotType) {
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.isOccupied = false;
        this.parkedVehicle = null;
    }

    public boolean assignSlot(Vehicle vehicle) {
        if (!isOccupied && slotType.equalsIgnoreCase(vehicle.getVehicleType())) {
            this.parkedVehicle = vehicle;
            this.isOccupied = true;
            return true;
        }
        return false;
    }

    public Vehicle freeSlot() {
        Vehicle v = parkedVehicle;
        parkedVehicle = null;
        isOccupied = false;
        return v;
    }

    public int getSlotNumber()    { return slotNumber; }
    public boolean isOccupied()   { return isOccupied; }
    public Vehicle getParkedVehicle() { return parkedVehicle; }
    public String getSlotType()   { return slotType; }

    @Override
    public String toString() {
        return "Slot " + slotNumber + " [" + slotType + "] - "
                + (isOccupied ? "OCCUPIED (" + parkedVehicle.getVehicleNo() + ")" : "AVAILABLE");
    }
}
