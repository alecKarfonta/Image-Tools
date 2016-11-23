package com.alec.form.Views;

import com.alec.form.Controllers.ImageTools;
import com.alec.form.Models.Datapoint;
import com.alec.form.Models.FileSystemModel;
import com.alec.form.Models.Style;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

// this panel allows the user to add new styles of check to the styles file
public class StylesPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private File rootFolder;
    private ArrayList<Style> knownCheckStyles;
    private Style openCheck;
    private BufferedImage openImage, displayImage;
    private boolean isMakingDatapoint = false, 
            isMakingSignature = false,
            isMakingAccount = false,
            isMakingAmount = false,
            isMakingCheckId = false;
    private Point clickOnePoint, clickTwoPoint;
    
    public StylesPanel() {
        initComponents();
        System.out.println("StylesPanel.init()");
        // Set grid layout
        jpDatapoints.setLayout(new GridLayout(0, 4));
        
        // try to load the styles file
        try {
            // read the known check styles json
            Gson gson = new Gson();
            BufferedReader reader = new BufferedReader(new FileReader(Constants.stylesPath));
            knownCheckStyles = gson.fromJson(reader, new TypeToken<ArrayList<Style>>(){}.getType());
            // if knownCheckStyles could not be loaded
            if (knownCheckStyles == null) {
                System.out.println("AddStylesPanel(): could not read " + Constants.stylesPath);
                knownCheckStyles = new ArrayList<Style>();
                return;
            }
        } catch (FileNotFoundException ex) {
            knownCheckStyles = new ArrayList<Style>();
        }
        
        
        
        // Show styles
        for (Style check : knownCheckStyles) {
            
            File file = new File(check.getFilename());     
            try {
                BufferedImage image = ImageIO.read(file);
                BufferedImage level1 = extractDatapointFromImage(image, check.getSignature());
                BufferedImage level2 = extractDatapointFromImage(image, check.getAccount());
                BufferedImage level3 = extractDatapointFromImage(image, check.getAmount());
                addDatapoint(image, check.getStyle());
                addDatapoint(level1, "level1");
                addDatapoint(level2, "level2");
                addDatapoint(level3, "level3");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    // Add datapoint
    public void addDatapoint(BufferedImage image, String label) {
        
        JLabel jlDatapoint = new JLabel(label);
        jlDatapoint.setForeground(Color.white);
        jlDatapoint.setVerticalTextPosition(JLabel.TOP);
        jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

        jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image, 400, 400)))); 
        jpDatapoints.add(jlDatapoint);
    }
    
    // Extract datapoint
    private BufferedImage extractDatapointFromImage(BufferedImage image, Datapoint datapoint) {
        try {
            BufferedImage datapointImage = ImageTools.getCroppedBorderImage(
                    image, 
                    datapoint.getPoint1().x, datapoint.getPoint1().y,         // x, y
                        datapoint.getPoint2().x - datapoint.getPoint1().x,    // w 
                        datapoint.getPoint2().y - datapoint.getPoint1().y);   // h
            return datapointImage;
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
        
        
    }
    
    // display the list of styles
    public void onShow() {
        StringBuilder text = new StringBuilder();
        // check if the list is empty
        if (knownCheckStyles.isEmpty()) {
            JOptionPane.showMessageDialog(null, "List empty or nor loaded.");
            return;
        }
        for (Style check : knownCheckStyles) {
            text.append(check.getStyle());
            text.append('\n');
            for (Datapoint datapoint : check.extraDatapoints) {
                text.append("---");
                text.append(datapoint.getName());
                text.append('\n');
            }
        }
        
        JOptionPane.showMessageDialog(null, text.toString());
    }
    
    // clear the list of styles
    public void onClear() {
        int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to clear the list?");
        if (response == JOptionPane.OK_OPTION) {
            knownCheckStyles.clear();
        }        
    }

    // when user clicks the image, check if making a datapoint
    public void onImageClick(int x, int y) {
        // if the user is making a datapoint
        if (isMakingDatapoint) {
            // first click
            if (clickOnePoint == null) {
                clickOnePoint = new Point(x,y);
            // second click
            } else if (clickTwoPoint == null) {
                clickTwoPoint = new Point(x,y);
                createDatapoint();
                // set the focus on the style name input
                //jtaStyleName.requestFocus();
            }   
        }
    }
    
    private void createDatapoint() {
        // keep the display image prior to drawing the datapoint so that we can revert
        BufferedImage revertImage = ImageTools.copyImage(displayImage);
        // TODO: draw the datapoint outline on a separate transparent image and overlay
        // open a graphics2d object for drawing rectangles
        Graphics2D g2d = displayImage.createGraphics();

        // TODO: use range scaling to represent the points as percentages of total image, 
        //      so that data can be extracted at any resolution or size transform
        displayImage(displayImage, jpDatapoints);
        String name = null;

        // TODO: fill in the rest of this \
        // check if the user is entering a required datapoint
        if (isMakingSignature) {
            openCheck.setSignature(new Datapoint("Signature", clickOnePoint, clickTwoPoint));
            g2d.setColor(Constants.signatureColor);
            isMakingSignature = false;
        } else if (isMakingAmount) {
            openCheck.setAmount(new Datapoint("Amount", clickOnePoint, clickTwoPoint));
            g2d.setColor(Constants.amountColor);
            isMakingAmount = false;
        } else if (isMakingCheckId) {
            openCheck.setCheckId(new Datapoint("CheckId", clickOnePoint, clickTwoPoint));
            g2d.setColor(Constants.checkIdColor);
            isMakingCheckId = false;
        } else if (isMakingAccount) {
            openCheck.setAccount(new Datapoint("Account", clickOnePoint, clickTwoPoint));
            g2d.setColor(Constants.accountColor);
            isMakingAccount = false;
        // get a name for the new datapoint, adding an extra datapoint
        } else  {
            name = JOptionPane.showInputDialog(null, "Please enter a name for the datapoint: ", "Datapoint Creation", JOptionPane.OK_CANCEL_OPTION);
            g2d.setColor(Color.orange);// if the user enters a name create the datapiint 
            if (name != null && !name.isEmpty()) {
                Datapoint datapoint = new Datapoint(name, clickOnePoint, clickTwoPoint);
            // datapoint.extractHistogram(openImage);
                openCheck.addDatapoint(datapoint);
                g2d.setColor(Color.LIGHT_GRAY);
            // else the user cancelled so revert to old image
            } else {
                displayImage = revertImage;
                displayImage(displayImage, jpDatapoints);
            }
        }

        BasicStroke stroke = new BasicStroke(3);
        g2d.setStroke(stroke);
        g2d.drawRect(clickOnePoint.x, clickOnePoint.y, 
                clickTwoPoint.x - clickOnePoint.x, clickTwoPoint.y - clickOnePoint.y);

        displayImage(displayImage, jpDatapoints);

        // reset the click points
        clickOnePoint = null;
        clickTwoPoint = null;
        isMakingDatapoint = false;
    }
    
    private void resetDisplay() {
        jpDatapoints.setIcon(null);
        clickOnePoint = null;
        clickTwoPoint = null;
    }
    
    public void onChangeRoot() {
        int response = jfcRootChooser.showOpenDialog(this);
        if (response == JFileChooser.APPROVE_OPTION) {
            resetDisplay();
            rootFolder = jfcRootChooser.getSelectedFile();
        }
        FileSystemModel fileModel = new FileSystemModel(rootFolder);
    }
    
    private void displayImage(BufferedImage image, JLabel label) {
        label.setText(null);
        // set the image to the label's size
        //label.setIcon(new ImageIcon(ImageTools.resizeImage(image, label.getWidth(), label.getHeight())));
        // set the label to the image's size (for accruate datapoint creation)
        Dimension dim = new Dimension(image.getWidth(), image.getHeight());
        label.setPreferredSize(dim);
        label.setIcon(new ImageIcon(image));
        //label.setSize(label.getPreferredSize());
        //label.validate();
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jfcRootChooser = new javax.swing.JFileChooser();
        jpDatapointsDELETE = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jpDatapoints = new javax.swing.JLabel();

        setBackground(new java.awt.Color(91, 95, 101));

        jpDatapointsDELETE.setBackground(new java.awt.Color(91, 95, 101));
        jpDatapointsDELETE.setForeground(new java.awt.Color(91, 95, 101));

        javax.swing.GroupLayout jpDatapointsDELETELayout = new javax.swing.GroupLayout(jpDatapointsDELETE);
        jpDatapointsDELETE.setLayout(jpDatapointsDELETELayout);
        jpDatapointsDELETELayout.setHorizontalGroup(
            jpDatapointsDELETELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jpDatapointsDELETELayout.setVerticalGroup(
            jpDatapointsDELETELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jScrollPane1.setBackground(new java.awt.Color(91, 95, 101));
        jScrollPane1.setBorder(null);
        jScrollPane1.setForeground(new java.awt.Color(91, 95, 101));

        jpDatapoints.setBackground(new java.awt.Color(91, 95, 101));
        jpDatapoints.setForeground(new java.awt.Color(91, 95, 101));
        jpDatapoints.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jpDatapoints.setToolTipText("");
        jpDatapoints.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jpDatapoints.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jpDatapoints.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jpDatapoints.setMaximumSize(null);
        jpDatapoints.setMinimumSize(new java.awt.Dimension(500, 259));
        jpDatapoints.setOpaque(true);
        jpDatapoints.setPreferredSize(null);
        jpDatapoints.setSize(new java.awt.Dimension(500, 259));
        jpDatapoints.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jpDatapoints.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jpDatapointsMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jpDatapointsMouseEntered(evt);
            }
        });
        jScrollPane1.setViewportView(jpDatapoints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1324, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpDatapointsDELETE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 922, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jpDatapointsDELETE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jpDatapointsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jpDatapointsMouseClicked
        //System.out.println("( " + evt.getX() + " , " + evt.getY() + " )");
        onImageClick(evt.getX(), evt.getY());
    }//GEN-LAST:event_jpDatapointsMouseClicked

    private void jpDatapointsMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jpDatapointsMouseEntered

    }//GEN-LAST:event_jpDatapointsMouseEntered


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JFileChooser jfcRootChooser;
    private javax.swing.JLabel jpDatapoints;
    private javax.swing.JPanel jpDatapointsDELETE;
    // End of variables declaration//GEN-END:variables
}
