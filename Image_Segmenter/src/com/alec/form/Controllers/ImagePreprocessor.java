package com.alec.form.Controllers;

import java.awt.image.BufferedImage;

public class ImagePreprocessor {
    public ImagePreprocessor() {
        
    }
    
    public static BufferedImage process(BufferedImage image) {
        // try to pull a check from the image by doing some preprocessing: autoCrop, auto deskew
        // auto crop
        image = ImageTools.autoCrop(image);
        // auto deskew
//        image = ImageTools.autoDeskew(image);
        // auto crop again after rotation has been fixed
        return ImageTools.autoCrop(image);
    }
}
