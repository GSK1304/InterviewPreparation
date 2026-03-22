package lld.cabooking;
public record Location(double lat, double lng) {
    public Location {
        if (lat < -90 || lat > 90)   throw new IllegalArgumentException("Invalid lat: " + lat);
        if (lng < -180 || lng > 180) throw new IllegalArgumentException("Invalid lng: " + lng);
    }
    public double distanceTo(Location o) {
        double R=6371, dLat=Math.toRadians(o.lat-lat), dLng=Math.toRadians(o.lng-lng);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(o.lat))*Math.sin(dLng/2)*Math.sin(dLng/2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }
    @Override public String toString() { return String.format("(%.4f,%.4f)", lat, lng); }
}
