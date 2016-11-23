package com.alec.form.Models;

import com.alec.form.Controllers.ImageTools;
import java.awt.Point;
import java.awt.image.BufferedImage;

public class Datapoint {

    private String name;
    private Point point1, point2;
    private JointHistogram histogram;

    private Datapoint() {
    };
    
    public Datapoint(String name, Point point1, Point point2) {
        this.name = name; 
        this.point1 = point1;
        this.point2 = point2;
        this.histogram = new JointHistogram();
    }
    
    public JointHistogram extractHistogram(BufferedImage wholeImage) {
        BufferedImage dpImage = ImageTools.getCroppedBorderImage(
                                    wholeImage, 
                                    point1.x, point1.y,       // x,y
                                    point2.x - point1.x,      // w
                                    point2.y - point1.y);
        histogram.extract(dpImage);
        System.out.println("Histogram: " + histogram.toString());
        return histogram;
    }
    

    public String getName() {
        return name;
    }

    public Point getPoint1() {
        return point1;
    }
    
    public Point getPoint2() {
        return point2;
    }

    public JointHistogram getHistogram() {
        return histogram;
    }
    
    
}
