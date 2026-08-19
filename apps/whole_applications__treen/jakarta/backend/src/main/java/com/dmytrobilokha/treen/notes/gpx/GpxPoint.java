package com.dmytrobilokha.treen.notes.gpx;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

public class GpxPoint {

    @XmlAttribute
    private String lat;
    @XmlAttribute
    private String lon;
    @XmlElement
    private String name;
    @XmlElement(name = "desc")
    private String description;
    @XmlElement
    private String type;

    public GpxPoint() {
        // JAXB requires this constructor
    }

    public GpxPoint(String lat, String lon, String name, String description) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.description = description;
        this.type = "Travel";
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

}
