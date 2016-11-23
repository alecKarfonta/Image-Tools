package com.alec.form.Views;

import com.alec.form.Controllers.ExcelTools;
import com.alec.form.Controllers.ImagePreprocessor;
import com.alec.form.Controllers.ImageTools;
import com.alec.form.Models.Datapoint;
import com.alec.form.Models.FileSystemModel;
import com.alec.form.Models.Style;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;
import marvin.plugin.MarvinImagePlugin;
import marvin.util.MarvinPluginLoader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Blur extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private ArrayList<Style> knownCheckStyles;
    private Style openCheck;
    private BufferedImage openImage, displayImage;
    private final float minMatchDistance = 100; // threshold where checks will be considered the same style
    private File rootFolder;
    
    private int blurAmount = 10;
    private MarvinImagePlugin blurPlugin;
    
    
    
    public Blur() {
        initComponents();
        jpDatapoints.setLayout(new GridLayout(0, 2));
        readStyles();
        
        rootFolder = (File) jtrFolders.getModel().getRoot();
        jfcRootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        blurPlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.blur.gaussianBlur.jar");
        blurPlugin.setAttribute("radius", blurAmount);
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
        
        try {        
            File file = (File) jtrFolders.getLastSelectedPathComponent();            
            openImage = ImageIO.read(file);
            displayImage = openImage;
            openCheck = new Style();
            displayImage = ImageTools.copyImage(openImage);
            displayImage(displayImage, jlBefore);
            //openImage = ImagePreprocessor.process(openImage);
            openCheck.init(openImage);
            
            MarvinImage blurred = new MarvinImage(openImage);
            blurPlugin.process(new MarvinImage(openImage), blurred);
            blurred.update();
            displayImage(blurred.getBufferedImage(), jlAfter);
            
            
            // compare the check against all known checks
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
                
                // add the required datapoints to the datapoints panel
                if (openCheck.getSignature() != null) {
                    BufferedImage object1 = extractDatapointFromImage(openImage, openCheck.getSignature());
                    //object1 = ImageTools.resizeImage(openImage, WIDTH, blurAmount)
                    displayDatapoint(openCheck.getSignature());
                }
                if (openCheck.getAccount() != null) {
                    displayDatapoint(openCheck.getAccount());
                }
                if (openCheck.getAmount() != null) {
                    displayDatapoint(openCheck.getAmount());
                }
                if (openCheck.getCheckId() != null) {
                    displayDatapoint(openCheck.getCheckId());
                }
                
                // for each extra datapoint; add it to the datapoints panel                
                for (Datapoint datapoint : openCheck.extraDatapoints) {
                    displayDatapoint(datapoint);
                }
                
            } 
            
        } catch (IOException ex) {
            resetDisplay();
        }
        
        jpDatapoints.revalidate();
        jpDatapoints.repaint();
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
                    openImage = ImagePreprocessor.process(openImage);
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
                    Logger.getLogger(Blur.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        
//        XSSFWorkbook workbook = new XSSFWorkbook();
//        // name the excel file after the root folder
//        Sheet sheet = workbook.createSheet(rootFolder.getName());
//        
//        // add column titles
//        sheet.createRow(0);
//        sheet.getRow(0).createCell(1);
//        sheet.getRow(0).getCell(1).setCellValue("Signature");
//        
//        sheet.getRow(0).createCell(2);
//        sheet.getRow(0).getCell(2).setCellValue("Account");
//        
//        sheet.getRow(0).createCell(3);
//        sheet.getRow(0).getCell(3).setCellValue("Amount");
//        
//        sheet.getRow(0).createCell(4);
//        sheet.getRow(0).getCell(4).setCellValue("Check Id");
//        
//        sheet.setColumnWidth(1, 12000);
//        sheet.setColumnWidth(2, 17000);
//        sheet.setColumnWidth(3, 7000);
//        sheet.setColumnWidth(4, 5000);
//        int row = 1;
//        // for each file
//        for (File file : rootFolder.listFiles()) {             
//            // TODO: add support for other image formats, right now only jpg because of canny edge?
//            if (file.getName().contains(".jpg")) {
//                try {
//                    openImage = ImageIO.read(file);
//                    openCheck = new Style();    // need a new one every time?
//                    openImage = ImagePreprocessor.process(openImage);
//                    openCheck.init(openImage);
//                    // compare the check against all known checks
//                    // TODO: get the closest match
//                    float minDistance = minMatchDistance + 1; // min does not start below match threshold
//                    Style minDistanceCheck = null;
//                    // for each known check style
//                    for (Style knownCheck : knownCheckStyles) {
//                        //minDistance = knownCheck.compare(openCheck);
//                        // find the difference
//                        float distance = knownCheck.compare(openCheck);
//                        // if the distance is less than the min found so far
//                        if (distance < minDistance) {
//                            minDistance = distance;
//                            minDistanceCheck = knownCheck;
//                        }   
//                    }
//                    // if the minimum matched distance is in the threshold
//                    if (minDistance < minMatchDistance && minDistanceCheck != null) {
//                        // assign the check's style
//                        openCheck = minDistanceCheck;  
//                        
//                        // add the datapoints from the check to the excel file
//                        int col = 1;
//                        // add required datapoints to excel
//                        addDatapointToExcel(workbook, sheet, openCheck.getSignature(), row, col++);
//                        addDatapointToExcel(workbook, sheet, openCheck.getAccount(), row, col++);
//                        addDatapointToExcel(workbook, sheet, openCheck.getAmount(), row, col++);
//                        addDatapointToExcel(workbook, sheet, openCheck.getCheckId(), row, col++);
//                        
//                        // add extra datapoints to excel
//                        for (Datapoint datapoint : openCheck.extraDatapoints) {
//                            addDatapointToExcel(workbook, sheet, datapoint, row, col);
//                        }
//                        row++;
//                    }
//                } catch (IOException ex) {
//                    Logger.getLogger(ExtractPanel.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }
//        
//        ExcelTools.writeExcel(workbook, rootFolder.getName());
//        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
//        JOptionPane.showMessageDialog(this, "Excel file written: " + rootFolder.getName() + ".xlxs", "Success", 1);
//        
//        resetDisplay();
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
        
//         for each file
        for (File file : rootFolder.listFiles()) {             
            System.out.println(file.getName());
            // TODO: add support for other image formats, right now only jpg because of canny edge?
            if (file.getName().contains(".jpg") || file.getName().contains(".png")) {
                try {
                    openImage = ImageIO.read(file);
                    openCheck = new Style();    // need a new one every time?
                    openImage = ImagePreprocessor.process(openImage);
                    openCheck.init(openImage);
                    
                    // Randomize brightness
                    double brightnessChange = (Math.random() * maxBrightnessChange) * (Math.random() > 0.5f ? 1 : -1);
                    //ImageIcon alteredImage = alterBrightness(openImage, (float)(brightnessChange));
                    //ImageIcon alteredImage = alterBrightness(openImage, 1.0f);
                    
                    
                    MarvinImage image = MarvinImageIO.loadImage("./res/test.jpg");
                    MarvinImage backupImage = image.clone();    
                    MarvinImagePlugin     imagePlugin;
                    imagePlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.edge.edgeDetector.jar");
                    imagePlugin.process(image, image);
                            
                } catch (Exception ex) {
                    System.out.println(ex.toString());
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
            Logger.getLogger(Blur.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void onExtractSelected() {
        // TODO: this
    }
    
    public void resetDisplay() {
        jlBefore.setIcon(null);
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
   
    
    private void displayImage(BufferedImage image, JLabel label) {
        displayImage(image, label, null);
    }
    private void displayImage(BufferedImage image, JLabel label, String text) {
        label.setText(text);
        label.setIcon(new ImageIcon(ImageTools.resizeImage(image, label.getWidth(), label.getHeight())));
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jfcRootChooser = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jpPreview = new javax.swing.JPanel();
        jbChangeFolder = new javax.swing.JButton();
        sldBlur = new javax.swing.JSlider();
        jPanel2 = new javax.swing.JPanel();
        jlAfter = new javax.swing.JLabel();
        jlBefore = new javax.swing.JLabel();
        jpDatapoints = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtrFolders = new javax.swing.JTree();

        setBackground(new java.awt.Color(91, 95, 101));

        jPanel1.setBackground(new java.awt.Color(91, 95, 101));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 171, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 225, Short.MAX_VALUE)
        );

        jpPreview.setBackground(new java.awt.Color(91, 95, 101));

        jbChangeFolder.setText("Change Folder");
        jbChangeFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbChangeFolderActionPerformed(evt);
            }
        });

        sldBlur.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldBlurStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jpPreviewLayout = new javax.swing.GroupLayout(jpPreview);
        jpPreview.setLayout(jpPreviewLayout);
        jpPreviewLayout.setHorizontalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addGroup(jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpPreviewLayout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jbChangeFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(sldBlur, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jpPreviewLayout.setVerticalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addContainerGap(31, Short.MAX_VALUE)
                .addComponent(sldBlur, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jbChangeFolder)
                .addGap(18, 18, 18))
        );

        jPanel2.setBackground(new java.awt.Color(91, 95, 101));
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel2.setToolTipText("The currently select image file");

        jlAfter.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jlAfter.setToolTipText("The currently select image file");
        jlAfter.setMaximumSize(new java.awt.Dimension(500, 259));
        jlAfter.setMinimumSize(new java.awt.Dimension(500, 259));
        jlAfter.setPreferredSize(new java.awt.Dimension(500, 259));
        jlAfter.setSize(new java.awt.Dimension(500, 259));
        jlAfter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jlAfterMouseClicked(evt);
            }
        });

        jlBefore.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jlBefore.setToolTipText("The currently select image file");
        jlBefore.setMaximumSize(new java.awt.Dimension(500, 259));
        jlBefore.setMinimumSize(new java.awt.Dimension(500, 259));
        jlBefore.setPreferredSize(new java.awt.Dimension(500, 259));
        jlBefore.setSize(new java.awt.Dimension(500, 259));
        jlBefore.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jlBeforeMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jlBefore, javax.swing.GroupLayout.PREFERRED_SIZE, 692, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jlAfter, javax.swing.GroupLayout.DEFAULT_SIZE, 674, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jlBefore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jlAfter, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jpDatapointsLayout = new javax.swing.GroupLayout(jpDatapoints);
        jpDatapoints.setLayout(jpDatapointsLayout);
        jpDatapointsLayout.setHorizontalGroup(
            jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 504, Short.MAX_VALUE)
        );
        jpDatapointsLayout.setVerticalGroup(
            jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 157, Short.MAX_VALUE)
        );

        jtrFolders.setModel(new FileSystemModel(new File("images")));
        jtrFolders.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jtrFolders.setToolTipText("Select an image to preview. ");
        jtrFolders.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jtrFoldersValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jtrFolders);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jpDatapoints, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(22, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jpDatapoints, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jpPreview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jtrFoldersValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jtrFoldersValueChanged
        onFileSelect();
    }//GEN-LAST:event_jtrFoldersValueChanged

    private void jlBeforeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlBeforeMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jlBeforeMouseClicked

    private void jbChangeFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbChangeFolderActionPerformed
        onChangeRoot();
    }//GEN-LAST:event_jbChangeFolderActionPerformed

    private void jlAfterMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlAfterMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jlAfterMouseClicked

    private void sldBlurStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldBlurStateChanged
        System.out.println("Set blur amound");
        this.blurAmount = sldBlur.getValue();
        blurPlugin.setAttribute("radius", this.blurAmount);
        
    }//GEN-LAST:event_sldBlurStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton jbChangeFolder;
    private javax.swing.JFileChooser jfcRootChooser;
    private javax.swing.JLabel jlAfter;
    private javax.swing.JLabel jlBefore;
    private javax.swing.JPanel jpDatapoints;
    private javax.swing.JPanel jpPreview;
    private javax.swing.JTree jtrFolders;
    private javax.swing.JSlider sldBlur;
    // End of variables declaration//GEN-END:variables
}
