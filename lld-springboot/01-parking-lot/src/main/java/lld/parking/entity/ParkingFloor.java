package lld.parking.entity;

import jakarta.persistence.*;
import lld.parking.enums.SpotSize;
import lld.parking.enums.SpotStatus;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_floor")
@Getter @Setter @NoArgsConstructor
public class ParkingFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_number", nullable = false, unique = true)
    private Integer floorNumber;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParkingSpot> spots = new ArrayList<>();

    public long availableCount() {
        return spots.stream().filter(s -> s.getStatus() == SpotStatus.AVAILABLE).count();
    }
}
