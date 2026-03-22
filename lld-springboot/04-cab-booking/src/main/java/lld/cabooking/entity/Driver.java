package lld.cabooking.entity;
import jakarta.persistence.*;
import lld.cabooking.enums.DriverStatus;
import lld.cabooking.enums.VehicleType;
import lombok.*;
@Entity @Table(name = "driver") @Getter @Setter @NoArgsConstructor
public class Driver {
    @Id @Column(name = "driver_id") private String driverId;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String phone;
    @Column(name = "vehicle_number", nullable = false) private String vehicleNumber;
    @Column(name = "vehicle_model", nullable = false) private String vehicleModel;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false) private VehicleType vehicleType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DriverStatus status = DriverStatus.AVAILABLE;
    @Column(name = "current_lat") private Double currentLat;
    @Column(name = "current_lng") private Double currentLng;
    @Column(name = "total_ratings") private Integer totalRatings = 0;
    @Column(name = "rating_sum") private Double ratingSum = 0.0;
    public double getAverageRating() { return totalRatings == 0 ? 5.0 : ratingSum / totalRatings; }
    public double distanceTo(double lat, double lng) {
        double R=6371, dLat=Math.toRadians(lat-currentLat), dLng=Math.toRadians(lng-currentLng);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(currentLat))*Math.cos(Math.toRadians(lat))*Math.sin(dLng/2)*Math.sin(dLng/2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }
}
