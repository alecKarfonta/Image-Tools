package com.alec.form.Controllers;

import com.alec.form.Views.Constants;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ImageTools {
    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    public static BufferedImage autoDeskew (BufferedImage image) {
        // edge detection, to make line detection easier
        LineDetector lineDectector = new LineDetector();
        lineDectector.setSourceImage(image);
        lineDectector.process();
        BufferedImage edgedImage = lineDectector.getEdgesImage();
        
        // use hough transform to extract all straight lines
        HoughTransform houghTransform = new HoughTransform(edgedImage.getWidth(), edgedImage.getHeight());
        houghTransform.addPoints(edgedImage);
        // only return lines greater than half the image's width
        ArrayList<HoughLine> lines = houghTransform.getLines(100);
        double thetaSum = 0.0;
        if (Constants.isDebugMode) {
            System.out.println("Presort: ");
            for (HoughLine line : lines) {
                System.out.println("r = " + line.r + " theta = " + Math.toDegrees(line.theta));
            }
        }

        // sort the lines by length
        Collections.sort(lines, new Comparator<HoughLine>() {
           @Override
           public int compare(HoughLine line1, HoughLine line2) {
               if (line1.r > line2.r) {
                   return -1;
               } else {
                   return 1;
               }
           } 
        });
        
        if (Constants.isDebugMode) {
            System.out.println("Postsort: ");
            for (HoughLine line : lines) {
                System.out.println("r = " + line.r + " theta = " + Math.toDegrees(line.theta));
            }
        }
        // get the average angle of the top lines
        int topCount = 5;
        if (lines.size() < topCount) {
            topCount = lines.size();
        }
        int lineCount = 0;  // that we actually consider
        for (int index = 0; index < topCount; index++) {
            HoughLine line = lines.get(index);
            // ignore any line > +/- 10 degrees from 90 
            if (Math.toDegrees(line.theta) > 80 && Math.toDegrees(line.theta) < 100) {
                if (Constants.isDebugMode) {
                    System.out.println("Line: r = " + line.r + " theta = " + Math.toDegrees(line.theta));
                }
                thetaSum += line.theta;
                lineCount++;
                if (Constants.isDebugMode) {
                    line.draw(image, Color.red.getRGB());
                }
            }
        }
        
        double thetaAvg = Math.toDegrees(thetaSum) / lineCount;
        if (Constants.isDebugMode) {
            System.out.println("Average Line Angle: " + thetaAvg);
        }
        // only auto rotate a max of 10 degrees
        if (thetaAvg > 80 && thetaAvg < 100) {
            image = ImageTools.rotateImage(image, 90 - thetaAvg );
        }
        
        
        return image;
    }
    
    public static BufferedImage autoRotate(BufferedImage image) {
        
        // can we just use Straighten and Deskew(http://blogs.adobe.com/acrolaw/2013/06/straighten-and-deskew-pdf-pages-in-acrobat-xi/)
        // for images that are really bad, the people scanning them should have them centereed
        
        
//        // rotate the image until there is no white 
//        // x = 10% of image width from each edge
//        // y starts at 0 and ends when it reaches a non white pixel
//        int y = 0;
//        /** /
//        // while the top left corner is white and the top right corner is not white
//        while (ColorTools.isColorMatch(new Color(image.getRGB(5, 5)), Color.white, 5)) {
//            image = rotateImage(image, .25f);
//        }
//        /**/
//        while (ColorTools.isColorMatch(new Color(image.getRGB(image.getWidth() - 5, 5)), Color.white, 5)) {
//            image = rotateImage(image, -.25f);
//        }
//        /** /
//        while (ColorTools.isColorMatch(new Color(image.getRGB(5, image.getHeight() - 5)), Color.white, 5)) {
//            image = rotateImage(image, -.25f);
//        }
//        /**/
//        while (ColorTools.isColorMatch(new Color(image.getRGB(image.getWidth() - 15, image.getHeight() - 2)), Color.white, 5)) {
//            image = rotateImage(image, .25);
//        }
//        /**/ 

        return image;
    }

    public static BufferedImage convertImageToGrey(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        result.getGraphics().drawImage(image, 0, 0, null);
        return result;
    }

    // removes all white space
    public static BufferedImage autoCrop(BufferedImage image) {
        // read the border color from the image
        long borderColor = (image.getRGB(0, 0)
                + image.getRGB(0, image.getHeight() - 1)
                + image.getRGB(image.getWidth() - 1, 0)
                + image.getRGB(image.getWidth() - 1, image.getHeight() - 1))
                / 4;

        boolean atEnd = false;
        int upperBorder = -1, lowerBorder = -1, leftBorder = -1, rightBorder = -1;
        int lowerOffset = -1, rightOffset = -1;

        // find the upper border, by traversing down the middle of the image looking for the end of the border
        do {
            upperBorder++; // y value starts at 0, then increment each iteration
            // if the current pixel is the border color, and not past the end of the image
            if (!ColorTools.isColorMatch(image.getRGB(image.getWidth() / 2, upperBorder), borderColor, 100000)
                    && upperBorder <= image.getHeight()) {
                // go to the right up to ten pixels to see if this is a line and not just text
                for (int x = image.getWidth() / 2; x < (x + Math.max(10, image.getWidth() - x)) && x < image.getWidth(); x++) {
                    if (!ColorTools.isColorMatch(image.getRGB(x, upperBorder), borderColor, 100000)) {
                        atEnd = true;
                        break;
                    }
                }
            }
        } while (!atEnd);

        // find the left border
        atEnd = false;
        do {
            leftBorder++; // x value starts at 0, then increment each iteration

            if (!ColorTools.isColorMatch(image.getRGB(leftBorder, image.getHeight() / 2), borderColor, 100000)
                    && upperBorder <= image.getHeight()) {
                // go down up to ten pixels to see if this is a line and not just text
                for (int y = 0; y < (y + Math.max(10, image.getWidth() - y)) && y < image.getWidth(); y++) {
                    if (!ColorTools.isColorMatch(image.getRGB(leftBorder, image.getHeight() / 2 + y), borderColor, 100000)) {
                        atEnd = true;
                        break;
                    }
                }
            }

            for (int y = 0; y < image.getHeight(); y++) {
                if (!ColorTools.isColorMatch(image.getRGB(leftBorder, y), borderColor, 100000)) {
                    atEnd = true;
                    break;
                }
            }

            if (leftBorder >= image.getWidth()) {
                atEnd = true;
            }
        } while (!atEnd);

        // find the right border
        atEnd = false;
        rightBorder = image.getWidth();  // x value starts at 0, then increment each iteration
        do {
            rightBorder--;
            rightOffset++;
            if (!ColorTools.isColorMatch(image.getRGB(rightBorder, image.getHeight() / 2), borderColor, 100000)
                    && upperBorder <= image.getHeight()) {
                // go down up to ten pixels to see if this is a line and not just text
                for (int y = 0; y < (y + Math.max(10, image.getWidth() - y)) && y < image.getWidth(); y++) {
                    if (!ColorTools.isColorMatch(image.getRGB(rightBorder, y + image.getHeight() / 2), borderColor, 100000)) {
                        atEnd = true;
                        break;
                    }
                }
            }

            if (rightBorder >= image.getWidth()) {
                atEnd = true;
            }
        } while (!atEnd);

        // find the lower border
        lowerBorder = image.getHeight();  // y value starts at 0, then increment each iteration
        atEnd = false;
        do {
            lowerBorder--;
            lowerOffset++;
            for (int x = 0; x < image.getWidth(); x++) {
                if (!ColorTools.isColorMatch(image.getRGB(x, lowerBorder), borderColor, 100000)) {
                    atEnd = true;
                    break;
                }
            }

            if (lowerBorder >= image.getHeight()) {
                atEnd = true;
            }
        } while (!atEnd);

        return getCroppedBorderImage(image, leftBorder, upperBorder, image.getWidth() - rightOffset - leftBorder, image.getHeight() - lowerOffset - upperBorder);
    }

    // crops to the specifies dimensions
    public static BufferedImage getCroppedBorderImage(BufferedImage src, int x, int y, int w, int h) {
        BufferedImage dest = src.getSubimage(x, y, w, h);
        return dest;
    }

    public static BufferedImage rotateImage(BufferedImage image, double degrees) {
        double locationX = image.getWidth() / 2;
        double locationY = image.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(Math.toRadians(degrees), locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        //apply filter to image
        image = op.filter(image, null);
        return image;
    }

    // uses ColorConvertOp to convert image to grayscale 
    // ColorConvertOp link: http://docs.oracle.com/javase/6/docs/api/java/awt/image/ColorConvertOp.html#ColorConvertOp%28java.awt.color.ColorSpace%2C%20java.awt.RenderingHints%29
    // ColorSpace link: http://docs.oracle.com/javase/6/docs/api/java/awt/color/ColorSpace.html
    public static BufferedImage getGrayscale(BufferedImage origImage) {
        BufferedImageOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(origImage, null);
    }

    public static int getColorDifference(int color1, int color2) {
        if (-color1 > -color2) {
            //System.out.println(-color1 - -color2);
            return -color1 - -color2;
        } else {
            //System.out.println(-color2 - -color1);
            return -color2 - -color1;
        }
    }

    public static long getColorDifference(long color1, long color2) {
        if (-color1 > -color2) {
            //System.out.println(-color1 - -color2);
            return -color1 - -color2;
        } else {
            //System.out.println(-color2 - -color1);
            return -color2 - -color1;
        }
    }

    public static String getAspectRatio(BufferedImage image) {
        return image.getWidth() + ":" + image.getHeight();
    }

    public static byte[] getByteArray(BufferedImage image) throws IOException {

        // get DataBufferBytes from Raster
        WritableRaster raster = image.getRaster();
        DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
        return (data.getData());
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int scaledWidth, int scaledHeight) {
        //System.out.println("resizing...");
        boolean preserveAlpha = true;
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }

    public static BufferedImage Threshold(BufferedImage img) {
        // initial theshold of 100
        int threshold = 100;


        return Threshold(img, 100);
    }

    public static BufferedImage Threshold(BufferedImage img, int requiredThresholdValue) {
        int height = img.getHeight();
        int width = img.getWidth();
        BufferedImage finalThresholdImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int red = 0;
        int green = 0;
        int blue = 0;

        for (int x = 0; x < width; x++) {
            try {
                for (int y = 0; y < height; y++) {
                    int color = img.getRGB(x, y);

                    red = getRed(color);
                    green = getGreen(color);
                    blue = getBlue(color);

                    if ((red + green + green) / 3 < (int) (requiredThresholdValue)) {
                        finalThresholdImage.setRGB(x, y, mixColor(0, 0, 0));
                    } else {
                        finalThresholdImage.setRGB(x, y, mixColor(255, 255, 255));
                    }
                }
            } catch (Exception e) {
                e.getMessage();
            }
        }

        return finalThresholdImage;
    }

    private static int mixColor(int red, int green, int blue) {
        return red << 16 | green << 8 | blue;
    }

    public static int getRed(int color) {
        return (color & 0x00ff0000) >> 16;
    }

    public static int getGreen(int color) {
        return (color & 0x0000ff00) >> 8;
    }

    public static int getBlue(int color) {
        return (color & 0x000000ff) >> 0;

    }
}
