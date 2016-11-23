package com.alec.form.Views;

import com.alec.form.Controllers.ExcelTools;
import com.alec.form.Controllers.ImagePreprocessor;
import com.alec.form.Controllers.ImageTools;
import com.alec.form.Models.Datapoint;
import com.alec.form.Models.FileSystemModel;
import com.alec.form.Models.Style;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import static ij.measure.CurveFitter.f;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;
import marvin.plugin.MarvinImagePlugin;
import marvin.util.MarvinPluginLoader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GeneratePanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private ArrayList<Style> knownCheckStyles;
    private Style openCheck;
    private BufferedImage openImage, displayImage;
    private final float minMatchDistance = 100; // threshold where checks will be considered the same style
    private File rootFolder;
    
    private boolean isGenerateOriginal = true;
    private boolean isGenerateBrightness = true;
    private boolean isGenerateContrast = true;
    private boolean isGenerateGray = false;
    private boolean isGenerateEdges =false;
    private boolean isGenerateInvert = false;
    private boolean isGenerateEqual = true;
    
    
    private MarvinImagePlugin brightnessPlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.color.brightnessAndContrast.jar");
    private MarvinImagePlugin invertPlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.color.invert.jar");
    private MarvinImagePlugin equalPlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.equalization.histogramEqualization.jar");
    private MarvinImagePlugin grayPlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.color.grayScale.jar");
    private MarvinImagePlugin edgePlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.edge.edgeDetector.jar");

    private int contrastRange = 50;
    private int brightnessRange = 100;
    private int generatedCount = 1;
    
    
    public GeneratePanel() {
        initComponents();
        jpDatapoints.setLayout(new GridLayout(0, 2));
        readStyles();
        
        rootFolder = (File) jtrFolders.getModel().getRoot();
        jfcRootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Init directory
        jfcRootChooser.setCurrentDirectory(new File ("/Users/alec/Programming/Kaggle/Nature/"));
    }
    
    public void onTabSwitchIn() {
        readStyles();
        resetDisplay();
    }
    
    public void onFileSelect() {
        resetDisplay();
        System.out.println("jtrFolderValueChanged(): open " + jtrFolders.getLastSelectedPathComponent());
        // make sure the selection is not a directory
        if (jtrFolders.getLastSelectedPathComponent() != null &&
                !jtrFolders.getModel().isLeaf(jtrFolders.getLastSelectedPathComponent())) {
            System.out.println("jtrFolderValueChanged(): directory selected");
            return;
        }
        // make sure the file is an image
        String filename = jtrFolders.getLastSelectedPathComponent().toString().toLowerCase();
        
        
        if (    // if the name does not contain an image extension dont try to open it 
                // (important for working with many images)
                !(
                    filename.contains(".jpg") 
                    || filename.contains(".png")
                    // TODO: can only use jpg becuase of canny algo?
                    //|| filename.contains(".bmp")
                    //|| filename.contains(".gif")
                    // TODO: use apache pdfbox to al so handle pdfs
                )
            ) {
            System.out.println("jtrFolderValueChanged(): file was not an image");
            return;
        }
        
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        File file = (File) jtrFolders.getLastSelectedPathComponent();       

        MarvinImage image = MarvinImageIO.loadImage(file.getPath());
        MarvinImage backupImage = image.clone();    
        MarvinImagePlugin     imagePlugin;

        openImage = image.getBufferedImage();
        openCheck = new Style();
        openCheck.init(openImage);

        displayImage(openImage, jlImage);

        JLabel jlDatapoint = null;

        
        // Brightness
        if (isGenerateBrightness) {
            // Reset image
            image = backupImage;
            backupImage = image.clone();  
            brightnessPlugin.setAttribute("brightness", 0);
            brightnessPlugin.setAttribute("contrast", 0);

            int randomBrightness = (int)(brightnessRange * Math.random());
            randomBrightness *= (Math.random() > 0.5 ? 1 : -1);

            brightnessPlugin.setAttribute("brightness", randomBrightness);

            brightnessPlugin.process(image, image);
            image.update();

            jlDatapoint = new JLabel("Brightness");
            jlDatapoint.setForeground(Color.white);
            jlDatapoint.setVerticalTextPosition(JLabel.TOP);
            jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

            jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image.getBufferedImage(), 400, 400)))); 
            jpDatapoints.add(jlDatapoint);
        } 

        // Contrast
        if (isGenerateContrast) {
            // Reset image
            image = backupImage;
            backupImage = image.clone();  
            brightnessPlugin.setAttribute("brightness", 0);
            brightnessPlugin.setAttribute("contrast", 0);

            int randomContrast = (int)(contrastRange * Math.random());
            randomContrast *= (Math.random() > 0.5 ? 1 : -1);

            brightnessPlugin.setAttribute("contrast", randomContrast);

            brightnessPlugin.process(image, image);
            image.update();

            jlDatapoint = new JLabel("Contrast");
            jlDatapoint.setForeground(Color.white);
            jlDatapoint.setVerticalTextPosition(JLabel.TOP);
            jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

            jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image.getBufferedImage(), 400, 400)))); 
            jpDatapoints.add(jlDatapoint);
        }

        if (isGenerateEdges) {
            edgePlugin.process(image, image);

            image.update();

            jlDatapoint = new JLabel("Edges");
            jlDatapoint.setForeground(Color.white);
            jlDatapoint.setVerticalTextPosition(JLabel.TOP);
            jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

            jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image.getBufferedImage(), 400, 400))));    // h\
            jpDatapoints.add(jlDatapoint);
        }

        if (isGenerateGray) {
            // Reset image
            image = backupImage;
            backupImage = image.clone();  
            // Gray

            grayPlugin.process(image, image);
            image.update();

            jlDatapoint = new JLabel("Gray");
            jlDatapoint.setForeground(Color.white);
            jlDatapoint.setVerticalTextPosition(JLabel.TOP);
            jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

            jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image.getBufferedImage(), 400, 400)))); 
            jpDatapoints.add(jlDatapoint);
        }

        // Invert
        if (isGenerateInvert) {
            // Reset image
            image = backupImage;
            backupImage = image.clone();  


            invertPlugin.process(image, image);
            image.update();

            jlDatapoint = new JLabel("Invert");
            jlDatapoint.setForeground(Color.white);
            jlDatapoint.setVerticalTextPosition(JLabel.TOP);
            jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

            jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image.getBufferedImage(), 400, 400)))); 
            jpDatapoints.add(jlDatapoint);
        }

        // Equal
        if (isGenerateEqual) {
            String label = "Equal";


            // Reset image
            image = backupImage;
            backupImage = image.clone();  

            equalPlugin.process(image, image);
            image.update();

            addDatapoint(image.getBufferedImage(), label);

        }

            
        jpDatapoints.revalidate();
        jpDatapoints.repaint();
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    public void addDatapoint(BufferedImage image, String label) {
        
        JLabel jlDatapoint = new JLabel(label);
        jlDatapoint.setForeground(Color.white);
        jlDatapoint.setVerticalTextPosition(JLabel.TOP);
        jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);

        jlDatapoint.setIcon(new ImageIcon((ImageTools.resizeImage(image, 400, 400)))); 
        jpDatapoints.add(jlDatapoint);
    }
    
    public void onChangeRoot() {
        int response = jfcRootChooser.showOpenDialog(this);
        if (response == JFileChooser.APPROVE_OPTION) {
            resetDisplay();
            rootFolder = jfcRootChooser.getSelectedFile();
        }
        FileSystemModel fileModel = new FileSystemModel(rootFolder);
        jtrFolders.setModel(fileModel);
    }
    
    public void displayDatapoint(Datapoint datapoint) {
        BufferedImage image = ImageTools.getCroppedBorderImage(
                        openImage, 
                        datapoint.getPoint1().x, datapoint.getPoint1().y,       // x,y
                        datapoint.getPoint2().x - datapoint.getPoint1().x,      // w
                        datapoint.getPoint2().y - datapoint.getPoint1().y);
        
        
        // ocr the amount field
        String ocrText = null;
        if (datapoint.getName().contains("Amount")) {
            //ocrText = ocr.readImage(image);
        }
        String text = datapoint.getName() + ((ocrText != null) ? ": " + ocrText : "" );
        JLabel jlDatapoint = new JLabel(text);
        jlDatapoint.setForeground(Color.white);
        jlDatapoint.setVerticalTextPosition(JLabel.TOP);
        jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);
        
        jlDatapoint.setIcon(new ImageIcon(image));    // h\
        jpDatapoints.add(jlDatapoint);
        
        
        
    }
    
    // go through each file in the root folder checking if it is an image 
    // matching a known style, then extract datapoints from the image,
    // finally take all datapoints from every file and build an excel file with this data
    public void onExtractAll() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        System.out.println("Extracting images from " + rootFolder.getName() );
//         for each file
        for (File file : rootFolder.listFiles()) {             
            // TODO: add support for other image formats, right now only jpg because of canny edge?
            if (file.getName().contains(".jpg") || file.getName().contains(".png")) {
                try {
                    openImage = ImageIO.read(file);
                    openCheck = new Style();    // need a new one every time?
                    //openImage = ImagePreprocessor.process(openImage);
                    openCheck.init(openImage);
                    // compare the check against all known checks
                    // TODO: get the closest match
                    float minDistance = minMatchDistance + 1; // min does not start below match threshold
                    Style minDistanceCheck = null;
                    // for each known check style
                    for (Style knownCheck : knownCheckStyles) {
                        //minDistance = knownCheck.compare(openCheck);
                        // find the difference
                        float distance = knownCheck.compare(openCheck);
                        // if the distance is less than the min found so far
                        if (distance < minDistance) {
                            minDistance = distance;
                            minDistanceCheck = knownCheck;
                        }   
                    }
                    // if the minimum matched distance is in the threshold
                    if (minDistance < minMatchDistance && minDistanceCheck != null) {
                        // assign the check's style
                        openCheck = minDistanceCheck;  
                        
                        if (openCheck.getSignature() != null) {
                            BufferedImage object1 = extractDatapointFromImage(openImage, openCheck.getSignature());
                            if (object1 != null) {
                                System.out.println("(file.getName() = " + file.getName());
                                String[] oldName = file.getName().split("\\.");
                                if (oldName.length > 1) {
                                    System.out.println("(oldName[0] = " + oldName[1]);
                                    String newName = oldName[0] + "_0." + oldName[1];
                                    System.out.println("Exporting datapoint " + newName);
                                    File outputfile = new File("output/" + rootFolder.getName() + "/" + newName);
                                    ImageIO.write(object1, "png", outputfile);
                                }
                            }
                        }
                        
                        if (openCheck.getAccount() != null) {
                            BufferedImage object2 = extractDatapointFromImage(openImage, openCheck.getAccount());
                            if (object2 != null) {
                                System.out.println("(file.getName() = " + file.getName());
                                String[] oldName = file.getName().split("\\.");
                                if (oldName.length > 1) {
                                    System.out.println("(oldName[0] = " + oldName[1]);
                                    String newName = oldName[0] + "_1." + oldName[1];
                                    System.out.println("Exporting datapoint " + newName);
                                    File outputfile = new File("output/"  + rootFolder.getName() + "/" + newName);
                                    ImageIO.write(object2, "png", outputfile);
                                }
                            }
                        }
                        
                        if (openCheck.getAmount() != null) {
                            BufferedImage object3 = extractDatapointFromImage(openImage, openCheck.getAmount());
                            if (object3 != null) {
                                System.out.println("(file.getName() = " + file.getName());
                                String[] oldName = file.getName().split("\\.");
                                if (oldName.length > 1) {
                                    System.out.println("(oldName[0] = " + oldName[1]);
                                    String newName = oldName[0] + "_2." + oldName[1];
                                    System.out.println("Exporting datapoint " + newName);
                                    File outputfile = new File("output/"  + rootFolder.getName() + "/" + newName);
                                    ImageIO.write(object3, "png", outputfile);
                                }
                            }
                        }
                        
                        if (openCheck.getCheckId() != null) {
                            BufferedImage object4 = extractDatapointFromImage(openImage, openCheck.getCheckId());
                            if (object4 != null) {
                                System.out.println("(file.getName() = " + file.getName());
                                String[] oldName = file.getName().split("\\.");
                                if (oldName.length > 1) {
                                    System.out.println("(oldName[0] = " + oldName[1]);
                                    String newName = oldName[0] + "_3." + oldName[1];
                                    System.out.println("Exporting datapoint " + newName);
                                    File outputfile = new File("output/"  + rootFolder.getName() + "/" + newName);
                                    ImageIO.write(object4, "png", outputfile);
                                }
                            }
                        }
                       
                    } else {
                        System.out.println("Error: no matching chek");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GeneratePanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        
    }
    
    private BufferedImage blurImage(BufferedImage image, Datapoint datapoint) {
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
    
    
    public void generateRandomBrightness() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        System.out.println("Generating images from " + rootFolder.getName() );
        
        File saveFolder = new File(rootFolder.getName() + "/randomBrightnesss/");
        
        // If folder does not exist
        if (!saveFolder.exists() ) {
            // Make it
            saveFolder.mkdir();
        }
        
        double maxBrightnessChange = 0.1f;
        // If root folder does not exist
        File outputFolder = new File("output/" + (rootFolder.getName()));
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        
        Iterator writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = (ImageWriter)writers.next();   
        
//         for each file
        int fileIndex = 0;
        for (File file : rootFolder.listFiles()) {    
            fileIndex += 1;
            float percentDone = (float)(fileIndex) / (float)(rootFolder.listFiles().length) * 100.0f;
            pgProgress.setValue((int)percentDone);
            System.out.println(fileIndex + "/" +  rootFolder.listFiles().length + " " + percentDone + "%");
            System.out.println("File : " +  file.getName());
            
            
            // TODO: add support for other image formats, right now only jpg because of canny edge?
            if (file.getName().contains(".jpg") || file.getName().contains(".png")) {
                try {
                    //openImage = ImageIO.read(file);
                    //openCheck = new Style();    // need a new one every time?
                    //openImage = ImagePreprocessor.process(openImage);
                    //openCheck.init(openImage);
                    
                    MarvinImage image = MarvinImageIO.loadImage(file.getPath());
                    MarvinImage backupImage = image.clone();    

                    JLabel jlDatapoint = null;

                    // Original
                    if (isGenerateOriginal) {
                        
                            String[] oldName = file.getName().split("\\.");
                            if (oldName.length > 1) {
                                String newName = oldName[0] + "_original.jpg";
                                String filename = "output/"  + rootFolder.getName() + "/" + newName;
                                File outputFile = new File(filename);
                                
                                ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                                
                            }
                    }
                    // Brightness
                    if (isGenerateBrightness) {
                        
                        for (int index = 0; index < generatedCount; index++) {
                            // Reset image
                            image = backupImage;
                            backupImage = image.clone(); 
                            brightnessPlugin.setAttribute("brightness", 0);
                            brightnessPlugin.setAttribute("contrast", 0);

                            
                            int randomBrightness = (int)(brightnessRange * Math.random());
                            randomBrightness *= (Math.random() > 0.5 ? 1 : -1);


                            brightnessPlugin.setAttribute("brightness", randomBrightness);
                            brightnessPlugin.process(image, image);
                            image.update();

                            String[] oldName = file.getName().split("\\.");
                            if (oldName.length > 1) {
                                String newName = oldName[0] + "_brightness_" + index + ".jpg";
                                String filename = "output/"  + rootFolder.getName() + "/" + newName;
                                File outputFile = new File(filename);
                                
                                ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                                
                            }
                        }
                                


                    } 

                    // Contrast
                    if (isGenerateContrast) {
                        
                        for (int index = 0; index < generatedCount; index++) {
                            // Reset image
                            image = backupImage;
                            backupImage = image.clone();  
                            brightnessPlugin.setAttribute("brightness", 0);
                            brightnessPlugin.setAttribute("contrast", 0);

                            
                            int randomContrast = (int)(contrastRange * Math.random());
                            randomContrast *= (Math.random() > 0.5 ? 1 : -1);

                            brightnessPlugin.setAttribute("contrast", randomContrast);

                            brightnessPlugin.process(image, image);
                            image.update();
                            
                            String[] oldName = file.getName().split("\\.");
                            if (oldName.length > 1) {
                                String newName = oldName[0] + "_contrast_" + index + ".jpg";
                                String filename = "output/"  + rootFolder.getName() + "/" + newName;
                                
                                File outputFile = new File(filename);
                                ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                            }
                        }

                    }

                    if (isGenerateEdges) {
                        edgePlugin.process(image, image);

                        image.update();
                        
                        String[] oldName = file.getName().split("\\.");
                        if (oldName.length > 1) {
                            String newName = oldName[0] + "_edges." + oldName[1];
                                String filename = "output/"  + rootFolder.getName() + "/" + newName;
                                
                                File outputFile = new File(filename);
                                ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                        }
                    }

                    // Gray
                    if (isGenerateGray) {
                        // Reset image
                        image = backupImage;
                        backupImage = image.clone();  
                        
                        // Init plugin
                        
                        grayPlugin.process(image, image);
                        image.update();
                        
                        String[] oldName = file.getName().split("\\.");
                        if (oldName.length > 1) {
                            String newName = oldName[0] + "_gray" + ".jpg";
                            String filename = "output/"  + rootFolder.getName() + "/" + newName;

                            File outputFile = new File(filename);
                            ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                        }

                    }

                    // Invert
                    if (isGenerateInvert) {
                        // Reset image
                        image = backupImage;
                        backupImage = image.clone();  

                        invertPlugin.process(image, image);
                        image.update();

                        String[] oldName = file.getName().split("\\.");
                        if (oldName.length > 1) {
                            String newName = oldName[0] + "_inverted" + ".jpg";
                            String filename = "output/"  + rootFolder.getName() + "/" + newName;

                            File outputFile = new File(filename);
                            ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                        }
                    }
                    
                    // Equal
                    if (isGenerateEqual) {
                        // Reset image
                        image = backupImage;
                        backupImage = image.clone();  

                        
                        equalPlugin.process(image, image);
                        image.update();

                        String[] oldName = file.getName().split("\\.");
                        if (oldName.length > 1) {
                            String newName = oldName[0] + "_equal" + ".jpg";
                            String filename = "output/"  + rootFolder.getName() + "/" + newName;

                            File outputFile = new File(filename);
                            ImageIO.write((ImageTools.resizeImage(image.getBufferedImage(), image.getWidth(), image.getHeight())), "jpg", outputFile);
                        }
                    }
                            
                } catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }                
                
            }
            
            
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        
    }
    
    private ImageIcon alterBrightness(BufferedImage bufferedImage, float brightness) {
        RescaleOp rescaleOp = new RescaleOp(brightness, 0, null);
        return new ImageIcon(rescaleOp.filter(bufferedImage, null));
       }
    
    public void addDatapointToExcel(XSSFWorkbook workbook, Sheet sheet, Datapoint datapoint, int row, int col) {
        try {
            BufferedImage datapointImage = ImageTools.getCroppedBorderImage(
                openImage, 
                datapoint.getPoint1().x, datapoint.getPoint1().y,         // x, y
                    datapoint.getPoint2().x - datapoint.getPoint1().x,    // w 
                    datapoint.getPoint2().y - datapoint.getPoint1().y);   // h
            ExcelTools.addPictureToExcel(workbook, sheet, datapointImage, row, col);
        } catch (IOException ex) {
            Logger.getLogger(GeneratePanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void onExtractSelected() {
        // TODO: this
    }
    
    public void resetDisplay() {
        jlImage.setIcon(null);
        openCheck = null;
        openImage = null;
        jpDatapoints.removeAll();
        jpDatapoints.revalidate();
        jpDatapoints.repaint();
    }
    
    private void readStyles() {
        try {
            // read the known check styles json
            Gson gson = new Gson();
            BufferedReader reader = new BufferedReader(new FileReader(Constants.stylesPath));
            knownCheckStyles = gson.fromJson(reader, new TypeToken<ArrayList<Style>>(){}.getType());
            // if knownCheckStyles could not be loaded
            if (knownCheckStyles == null) {
                System.out.println("readStyles(): could not read knownCheckStyles.json");
                knownCheckStyles = new ArrayList<Style>();
                //return;
            }
        } catch (FileNotFoundException ex) {
            knownCheckStyles = new ArrayList<Style>();
        }
    }
   
    
    // given an image and a jlabel, size the image to fit in the label
    private void displayImage(BufferedImage image, JLabel label) {
        label.setText(null);
        label.setIcon(new ImageIcon(ImageTools.resizeImage(image, label.getWidth(), label.getHeight())));
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jfcRootChooser = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtrFolders = new javax.swing.JTree();
        jbChangeFolder = new javax.swing.JButton();
        jbPullDatapoints1 = new javax.swing.JButton();
        chbBrightness = new javax.swing.JCheckBox();
        chbGray = new javax.swing.JCheckBox();
        chbEdges = new javax.swing.JCheckBox();
        chbInvert = new javax.swing.JCheckBox();
        sldCount = new javax.swing.JSlider();
        lblCount = new javax.swing.JLabel();
        jpPreview = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jlImage = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jspDatapoints = new javax.swing.JScrollPane();
        jpDatapoints = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        chbContrast = new javax.swing.JCheckBox();
        chbEqual = new javax.swing.JCheckBox();
        pgProgress = new javax.swing.JProgressBar();

        setBackground(new java.awt.Color(91, 95, 101));

        jPanel1.setBackground(new java.awt.Color(91, 95, 101));

        jtrFolders.setModel(new FileSystemModel(new File("images")));
        jtrFolders.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jtrFolders.setToolTipText("Select an image to preview. ");
        jtrFolders.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jtrFoldersValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jtrFolders);

        jbChangeFolder.setText("Change Folder");
        jbChangeFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbChangeFolderActionPerformed(evt);
            }
        });

        jbPullDatapoints1.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jbPullDatapoints1.setText("Generate");
        jbPullDatapoints1.setToolTipText("Search the current folder for any known forms and export their data to an excel file.");
        jbPullDatapoints1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbPullDatapoints1ActionPerformed(evt);
            }
        });

        chbBrightness.setBackground(new java.awt.Color(0, 0, 0));
        chbBrightness.setForeground(new java.awt.Color(255, 255, 255));
        chbBrightness.setSelected(true);
        chbBrightness.setText("Brightness");
        chbBrightness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chbBrightnessActionPerformed(evt);
            }
        });

        chbGray.setBackground(new java.awt.Color(0, 0, 0));
        chbGray.setForeground(new java.awt.Color(255, 255, 255));
        chbGray.setText("Gray");
        chbGray.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chbGrayActionPerformed(evt);
            }
        });

        chbEdges.setBackground(new java.awt.Color(0, 0, 0));
        chbEdges.setForeground(new java.awt.Color(255, 255, 255));
        chbEdges.setText("Edges");
        chbEdges.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chbEdgesActionPerformed(evt);
            }
        });

        chbInvert.setBackground(new java.awt.Color(0, 0, 0));
        chbInvert.setForeground(new java.awt.Color(255, 255, 255));
        chbInvert.setText("Invert");
        chbInvert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chbInvertActionPerformed(evt);
            }
        });

        sldCount.setMaximum(20);
        sldCount.setMinimum(1);
        sldCount.setToolTipText("");
        sldCount.setValue(1);
        sldCount.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldCountStateChanged(evt);
            }
        });

        lblCount.setBackground(new java.awt.Color(0, 0, 0));
        lblCount.setForeground(new java.awt.Color(255, 255, 255));
        lblCount.setText("1");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jbPullDatapoints1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jbChangeFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(chbBrightness)
                                    .addComponent(chbEdges))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(chbInvert)
                                    .addComponent(chbGray)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addComponent(lblCount, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(sldCount, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbChangeFolder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbPullDatapoints1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldCount, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(lblCount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chbBrightness)
                    .addComponent(chbGray))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chbEdges)
                    .addComponent(chbInvert)))
        );

        jpPreview.setBackground(new java.awt.Color(91, 95, 101));

        jPanel2.setBackground(new java.awt.Color(91, 95, 101));
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel2.setToolTipText("The currently select image file");

        jlImage.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jlImage.setToolTipText("The currently select image file");
        jlImage.setMaximumSize(new java.awt.Dimension(500, 259));
        jlImage.setMinimumSize(new java.awt.Dimension(500, 259));
        jlImage.setPreferredSize(new java.awt.Dimension(500, 259));
        jlImage.setSize(new java.awt.Dimension(500, 259));
        jlImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jlImageMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jlImage, javax.swing.GroupLayout.PREFERRED_SIZE, 414, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jlImage, javax.swing.GroupLayout.PREFERRED_SIZE, 267, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jScrollPane1.setBackground(new java.awt.Color(91, 95, 101));
        jScrollPane1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jspDatapoints.setBackground(new java.awt.Color(91, 95, 101));
        jspDatapoints.setBorder(null);
        jspDatapoints.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jspDatapoints.setToolTipText("");

        jpDatapoints.setBackground(new java.awt.Color(91, 95, 101));

        javax.swing.GroupLayout jpDatapointsLayout = new javax.swing.GroupLayout(jpDatapoints);
        jpDatapoints.setLayout(jpDatapointsLayout);
        jpDatapointsLayout.setHorizontalGroup(
            jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 874, Short.MAX_VALUE)
        );
        jpDatapointsLayout.setVerticalGroup(
            jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 418, Short.MAX_VALUE)
        );

        jspDatapoints.setViewportView(jpDatapoints);

        jScrollPane1.setViewportView(jspDatapoints);

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Preview");

        jLabel2.setBackground(new java.awt.Color(255, 255, 255));
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Data Found");

        javax.swing.GroupLayout jpPreviewLayout = new javax.swing.GroupLayout(jpPreview);
        jpPreview.setLayout(jpPreviewLayout);
        jpPreviewLayout.setHorizontalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addGroup(jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE)
                    .addGroup(jpPreviewLayout.createSequentialGroup()
                        .addGroup(jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jpPreviewLayout.setVerticalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
                .addContainerGap())
        );

        chbContrast.setBackground(new java.awt.Color(0, 0, 0));
        chbContrast.setForeground(new java.awt.Color(255, 255, 255));
        chbContrast.setSelected(true);
        chbContrast.setText("Contrast");
        chbContrast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chbContrastActionPerformed(evt);
            }
        });

        chbEqual.setBackground(new java.awt.Color(0, 0, 0));
        chbEqual.setForeground(new java.awt.Color(255, 255, 255));
        chbEqual.setSelected(true);
        chbEqual.setText("Equal");
        chbEqual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chbEqualActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(chbContrast)
                        .addGap(18, 18, 18)
                        .addComponent(chbEqual))
                    .addComponent(pgProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(chbContrast)
                            .addComponent(chbEqual))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pgProgress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jtrFoldersValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jtrFoldersValueChanged
        onFileSelect();
    }//GEN-LAST:event_jtrFoldersValueChanged

    private void jlImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlImageMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jlImageMouseClicked

    private void jbChangeFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbChangeFolderActionPerformed
        onChangeRoot();
    }//GEN-LAST:event_jbChangeFolderActionPerformed

    private void jbPullDatapoints1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbPullDatapoints1ActionPerformed
        generateRandomBrightness();
    }//GEN-LAST:event_jbPullDatapoints1ActionPerformed

    private void chbBrightnessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chbBrightnessActionPerformed
        this.isGenerateBrightness = chbBrightness.isSelected();
    }//GEN-LAST:event_chbBrightnessActionPerformed

    private void chbGrayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chbGrayActionPerformed
        this.isGenerateGray = chbGray.isSelected();

    }//GEN-LAST:event_chbGrayActionPerformed

    private void chbEdgesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chbEdgesActionPerformed
        System.out.println(evt.getActionCommand());
        this.isGenerateEdges = chbEdges.isSelected();
    }//GEN-LAST:event_chbEdgesActionPerformed

    private void chbInvertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chbInvertActionPerformed
        this.isGenerateInvert = chbInvert.isSelected();
    }//GEN-LAST:event_chbInvertActionPerformed

    private void chbContrastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chbContrastActionPerformed
        this.isGenerateContrast = chbContrast.isSelected();
    }//GEN-LAST:event_chbContrastActionPerformed

    private void chbEqualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chbEqualActionPerformed
        this.isGenerateEqual = chbEqual.isSelected();
    }//GEN-LAST:event_chbEqualActionPerformed

    private void sldCountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldCountStateChanged
        int value = sldCount.getValue();
        this.generatedCount = value;
        lblCount.setText("" + value);
    }//GEN-LAST:event_sldCountStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox chbBrightness;
    private javax.swing.JCheckBox chbContrast;
    private javax.swing.JCheckBox chbEdges;
    private javax.swing.JCheckBox chbEqual;
    private javax.swing.JCheckBox chbGray;
    private javax.swing.JCheckBox chbInvert;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton jbChangeFolder;
    private javax.swing.JButton jbPullDatapoints1;
    private javax.swing.JFileChooser jfcRootChooser;
    private javax.swing.JLabel jlImage;
    private javax.swing.JPanel jpDatapoints;
    private javax.swing.JPanel jpPreview;
    private javax.swing.JScrollPane jspDatapoints;
    private javax.swing.JTree jtrFolders;
    private javax.swing.JLabel lblCount;
    private javax.swing.JProgressBar pgProgress;
    private javax.swing.JSlider sldCount;
    // End of variables declaration//GEN-END:variables
}
