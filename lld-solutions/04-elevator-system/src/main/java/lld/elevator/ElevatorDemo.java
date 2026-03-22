package lld.elevator;

public class ElevatorDemo {
    public static void main(String[] args) {
        Building tower = new Building.Builder("Tech Tower", 20)
            .addElevator("E1", 0,  10)
            .addElevator("E2", 10, 10)
            .addElevator("E3", 5,  8)
            .strategy(new ScanStrategy())
            .build();

        tower.displayStatus();

        tower.requestElevator(1,  Direction.UP);
        tower.requestElevator(15, Direction.DOWN);
        tower.requestElevator(7,  Direction.UP);
        tower.selectFloor("E1", 8);
        tower.selectFloor("E2", 3);

        tower.simulate(15);

        tower.setMaintenance("E3", true);
        System.out.println("E3 under maintenance:");
        tower.requestElevator(12, Direction.DOWN);

        try {
            tower.requestElevator(25, Direction.UP);
        } catch (InvalidFloorException e) {
            System.out.println("Error: " + e.getMessage());
        }

        tower.displayStatus();
    }
}
