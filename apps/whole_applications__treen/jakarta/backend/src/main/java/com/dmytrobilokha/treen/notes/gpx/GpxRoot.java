package com.dmytrobilokha.treen.notes.gpx;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import javax.annotation.CheckForNull;
import java.util.List;

@XmlRootElement(name = "gpx")
public class GpxRoot {

    @CheckForNull
    @XmlTransient
    private String collectionName;
    @XmlAttribute
    private String version;
    @XmlAttribute
    private String creator;
    @XmlElement(name = "wpt")
    private List<GpxPoint> points;

    public GpxRoot() {
        // JAXB requires this constructor
    }

    public GpxRoot(@CheckForNull String collectionName, List<GpxPoint> points) {
        this.collectionName = collectionName;
        this.version = "1.1";
        this.creator = "treen";
        this.points = points;
    }

    @CheckForNull
    public String getCollectionName() {
        return collectionName;
    }

    public String getVersion() {
        return version;
    }

    public String getCreator() {
        return creator;
    }

    public List<GpxPoint> getPoints() {
        return points;
    }
}
