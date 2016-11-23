package com.alec.form.Models;

import com.alec.form.Controllers.ImageTools;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;

// used to identify an image and important regions of the image
public class Style implements Serializable {
    private static final long serialVersionUID = 1L;

    private String style;
    private JointHistogram histogram;
    private String filename;
    public Datapoint signature, account, checkId, amount;   // every check style must have these 4 set
    public ArrayList<Datapoint> extraDatapoints;

    public Style() {
        // do nothing, use init so you can return buff image
    }
    // init with the image trying to be identified, 
    // return the image after preprocessing is done to extract a check from the image
    public void init(BufferedImage image) {
        // normalize size (can no longer be displayed correctly)
        BufferedImage normImage = ImageTools.resizeImage(image, 200, 100);
        // now build the image's histogram
        histogram = new JointHistogram();
        histogram.extract(normImage);
        extraDatapoints = new ArrayList<Datapoint>();
    }
   
    public float compare(Style otherCheck) {
        // find the difference between this check's histogram and the other's
        float difference = otherCheck.getHistogram().getDistance(histogram);
        // if the difference was with a certain value return true
        return difference;
    }
    
    public void addDatapoint(Datapoint newPoint) {
        extraDatapoints.add(newPoint);        
    }

    public void setSignature(Datapoint signature) {
        this.signature = signature;
    }

    public void setAccount(Datapoint account) {
        this.account = account;
    }

    public void setCheckId(Datapoint checkId) {
        this.checkId = checkId;
    }

    public void setAmount(Datapoint amount) {
        this.amount = amount;
    }
    
    
    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyle() {
        return style;
    }

     // check whether the check has been fully defined
    public boolean isSet() {
        if (signature != null && account != null && checkId != null && amount != null ) {
            return true;
        } else {
            return false;
        }
    }

    public Datapoint getSignature() {
        return signature;
    }

    public Datapoint getAccount() {
        return account;
    }

    public Datapoint getCheckId() {
        return checkId;
    }

    public Datapoint getAmount() {
        return amount;
    }
    
    public JointHistogram getHistogram () {
        return histogram;
    }
    
    public void setFilename(String filename) {
        this.filename = filename; 
    }
    
    public String getFilename() {
        return this.filename;
    }
}
