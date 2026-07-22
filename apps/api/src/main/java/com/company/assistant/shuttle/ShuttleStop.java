package com.company.assistant.shuttle;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "shuttle_stop")
public class ShuttleStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private ShuttleRoute route;

    private String name;

    private LocalTime time;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    private Double latitude;

    private Double longitude;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ShuttleRoute getRoute() { return route; }
    public void setRoute(ShuttleRoute route) { this.route = route; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
