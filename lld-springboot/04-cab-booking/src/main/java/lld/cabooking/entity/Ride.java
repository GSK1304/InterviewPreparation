package lld.cabooking.entity;
import jakarta.persistence.*;
import lld.cabooking.enums.RideStatus;
import lld.cabooking.enums.VehicleType;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "ride") @Getter @Setter @NoArgsConstructor
public class Ride {
    @Id @Column(name = "ride_id") private String rideId;
    @Column(name = "rider_id", nullable = false) private String riderId;
    @Column(name = "driver_id") private String driverId;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @Column(name = "pickup_lat", nullable = false) private Double pickupLat;
    @Column(name = "pickup_lng", nullable = false) private Double pickupLng;
    @Column(name = "dropoff_lat", nullable = false) private Double dropoffLat;
    @Column(name = "dropoff_lng", nullable = false) private Double dropoffLng;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RideStatus status = RideStatus.REQUESTED;
    @Column(name = "estimated_fare_paise") private Long estimatedFarePaise;
    @Column(name = "actual_fare_paise") private Long actualFarePaise;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt = Instant.now();
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
}
