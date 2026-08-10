package com.company.assistant.shuttle;

import jakarta.persistence.*;

@Entity
@Table(name = "shuttle_route_point")
public class ShuttleRoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private ShuttleRoute route;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    private Double latitude;

    private Double longitude;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ShuttleRoute getRoute() { return route; }
    public void setRoute(ShuttleRoute route) { this.route = route; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
