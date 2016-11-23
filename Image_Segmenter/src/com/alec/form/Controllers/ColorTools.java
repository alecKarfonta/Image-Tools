package com.alec.form.Controllers;

import java.awt.Color;
import static com.alec.form.Controllers.ImageTools.getColorDifference;

public class ColorTools {
    
    public static double getDifference(Color color1, Color color2) {
        double difference = 0.0;
        // get the color difference
        if (color1 != null && color2 != null) {
            difference += Math.pow(color1.getRed() - color2.getRed(),2);
            difference += Math.pow(color1.getBlue() - color2.getBlue(),2);
            difference += Math.pow(color1.getGreen() - color2.getGreen(),2);
            difference += Math.pow(color1.getAlpha() - color2.getAlpha(),2);
        }
        return Math.sqrt(difference);
    }
    
      public static boolean isColorMatch(Color color1, Color color2, int range) {
        double difference = 0;
        // get the color difference
        if (color1 != null && color2 != null) {
            difference += Math.pow(color1.getRed() - color2.getRed(),2);
            difference += Math.pow(color1.getBlue() - color2.getBlue(),2);
            difference += Math.pow(color1.getGreen() - color2.getGreen(),2);
            difference += Math.pow(color1.getAlpha() - color2.getAlpha(),2);
        }
        difference = Math.sqrt(difference);
       // System.out.println("isColorMatch(): difference = " + difference);
        if (difference < range) {
            return true;
        }              
        return false;
    }
      
    
    public static boolean isColorMatch(int color1, int color2, int range) {
        if (getColorDifference(color1, color2) < range) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public static boolean isColorMatch(long color1, long color2, int range) {
        if (getColorDifference(color1, color2) < range) {
            return true;
        }
        else {
            return false;
        }
    }
    
    
}
