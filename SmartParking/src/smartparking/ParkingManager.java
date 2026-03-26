package smartparking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class ParkingManager {
    private List<ParkingSlot> slots;
    private Map<String, Integer> vehicleSlotMap; // vehicleNo -> slotNumber
    private List<String> activityLog;

    private static final double CAR_RATE  = 30.0;
    private static final double BIKE_RATE = 15.0;

    public ParkingManager(int carSlots, int bikeSlots) {
        slots = new ArrayList<>();
        vehicleSlotMap = new HashMap<>();
        activityLog = new ArrayList<>();

        int id = 1;
        for (int i = 0; i < carSlots; i++, id++) {
            slots.add(new ParkingSlot(id, "Car"));
        }
        for (int i = 0; i < bikeSlots; i++, id++) {
            slots.add(new ParkingSlot(id, "Bike"));
        }
    }

    // --- ENTRY ---
    public ParkingSlot addVehicle(Vehicle vehicle) throws ParkingException {
        if (vehicleSlotMap.containsKey(vehicle.getVehicleNo())) {
            throw new ParkingException("Vehicle " + vehicle.getVehicleNo() + " is already parked.");
        }

        for (ParkingSlot slot : slots) {
            if (!slot.isOccupied() && slot.getSlotType().equalsIgnoreCase(vehicle.getVehicleType())) {
                slot.assignSlot(vehicle);
                vehicleSlotMap.put(vehicle.getVehicleNo(), slot.getSlotNumber());
                log("ENTRY: " + vehicle.getVehicleNo() + " [" + vehicle.getVehicleType() + "] -> Slot " + slot.getSlotNumber());
                return slot;
            }
        }
        throw new ParkingException("No available " + vehicle.getVehicleType() + " slots.");
    }

    // --- EXIT ---
    public double removeVehicle(String vehicleNo) throws ParkingException {
        vehicleNo = vehicleNo.trim().toUpperCase();
        if (!vehicleSlotMap.containsKey(vehicleNo)) {
            throw new ParkingException("Vehicle " + vehicleNo + " not found in parking.");
        }

        int slotNum = vehicleSlotMap.get(vehicleNo);
        ParkingSlot slot = getSlotByNumber(slotNum);
        Vehicle vehicle = slot.freeSlot();
        vehicleSlotMap.remove(vehicleNo);

        double fee = calculateFee(vehicle);
        log(String.format("EXIT: %s [%s] | Slot %d freed | Fee: ₹%.2f",
                vehicleNo, vehicle.getVehicleType(), slotNum, fee));
        return fee;
    }

    // --- FEE ---
    public double calculateFee(Vehicle vehicle) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = Duration.between(vehicle.getEntryTime(), now).toMinutes();
        long hours = (long) Math.ceil(minutes / 60.0);
        if (hours < 1) hours = 1;
        double rate = vehicle.getVehicleType().equalsIgnoreCase("Car") ? CAR_RATE : BIKE_RATE;
        return hours * rate;
    }

    // --- QUERY ---
    public List<ParkingSlot> getAllSlots()        { return Collections.unmodifiableList(slots); }
    public List<String> getActivityLog()          { return Collections.unmodifiableList(activityLog); }

    public List<ParkingSlot> getAvailableSlots(String type) {
        List<ParkingSlot> avail = new ArrayList<>();
        for (ParkingSlot s : slots) {
            if (!s.isOccupied() && s.getSlotType().equalsIgnoreCase(type)) avail.add(s);
        }
        return avail;
    }

    public List<ParkingSlot> getOccupiedSlots() {
        List<ParkingSlot> occ = new ArrayList<>();
        for (ParkingSlot s : slots) {
            if (s.isOccupied()) occ.add(s);
        }
        return occ;
    }

    public int getTotalSlots(String type) {
        int count = 0;
        for (ParkingSlot s : slots) if (s.getSlotType().equalsIgnoreCase(type)) count++;
        return count;
    }

    public int getAvailableCount(String type) {
        return getAvailableSlots(type).size();
    }

    public boolean isVehicleParked(String vehicleNo) {
        return vehicleSlotMap.containsKey(vehicleNo.trim().toUpperCase());
    }

    public Vehicle getParkedVehicle(String vehicleNo) {
        vehicleNo = vehicleNo.trim().toUpperCase();
        if (!vehicleSlotMap.containsKey(vehicleNo)) return null;
        int slotNum = vehicleSlotMap.get(vehicleNo);
        ParkingSlot slot = getSlotByNumber(slotNum);
        return slot != null ? slot.getParkedVehicle() : null;
    }

    private ParkingSlot getSlotByNumber(int num) {
        for (ParkingSlot s : slots) if (s.getSlotNumber() == num) return s;
        return null;
    }

    private void log(String msg) {
        String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("HH:mm:ss").format(LocalDateTime.now());
        activityLog.add("[" + timestamp + "] " + msg);
    }
}
