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

public class ExtractPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private ArrayList<Style> knownCheckStyles;
    private Style openCheck;
    private BufferedImage openImage, displayImage;
    private float minMatchDistance = 100; // threshold where checks will be considered the same style
    private File rootFolder;
    
    public ExtractPanel() {
        initComponents();
        jpDatapoints.setLayout(new GridLayout(0, 2));
        readStyles();
        
        rootFolder = (File) jtrFolders.getModel().getRoot();
        jfcRootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }
    
    public void onTabSwitchIn() {
        readStyles();
        resetDisplay();
    }
    
    public void selectImage(File imageFile) {
        try {        
            File file = (File) jtrFolders.getLastSelectedPathComponent();            
            openImage = ImageIO.read(file);
            displayImage = openImage;
            openCheck = new Style();
            //displayImage = ImageTools.copyImage(openImage);
            displayImage(displayImage, jlImage);
            //openImage = ImagePreprocessor.process(openImage);
            openCheck.init(openImage);
            // compare the check against all known checks
            // TODO: get the closest match
            float minDistance = minMatchDistance + 1; // min does not start below match threshold
            Style minDistanceCheck = null;
            // for each known check style
            for (Style knownCheck : knownCheckStyles) {
                // find the difference
                float distance = knownCheck.compare(openCheck);
                // if the distance is less than the min found so far
                if (distance < minDistance) {
                    minDistance = distance;
                    minDistanceCheck = knownCheck;
                }   
            }
            
            
            // if the minimum matched distance is in the threshold
            if (minDistance < minMatchDistance) {
                // assign the check's style
                openCheck = minDistanceCheck;
                
                // Show style name
                lblStyle.setText(minDistanceCheck.getStyle());

                // add the required datapoints to the datapoints panel
                if (openCheck.getSignature() != null) {
                    displayDatapoint(openImage, openCheck.getSignature());
                }
                if (openCheck.getAccount() != null) {
                    displayDatapoint(openImage, openCheck.getAccount());
                }
                if (openCheck.getAmount() != null) {
                    displayDatapoint(openImage, openCheck.getAmount());
                }
                if (openCheck.getCheckId() != null) {
                    displayDatapoint(openImage, openCheck.getCheckId());
                }
                
                // for each extra datapoint; add it to the datapoints panel                
                for (Datapoint datapoint : openCheck.extraDatapoints) {
                    displayDatapoint(openImage, datapoint);
                }
                
            } else {
                
                // Show No Match
                lblStyle.setText("No Match");
            }
            
        } catch (IOException ex) {
            resetDisplay();
        }
    }
    
    public void onFileSelect() {
        resetDisplay();
        System.out.println("jtrFolderValueChanged(): open " + jtrFolders.getLastSelectedPathComponent());
        //jtrFolders.getSe
        // make sure the selection is not a directory
        if (jtrFolders.getLastSelectedPathComponent() == null ||
                !jtrFolders.getModel().isLeaf(jtrFolders.getLastSelectedPathComponent())) {
            System.out.println("jtrFolderValueChanged(): directory selected");
            rootFolder = (File) jtrFolders.getModel().getRoot();
            
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
        
        selectImage(new File(filename));
        
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
    
    public void displayDatapoint(BufferedImage image, Datapoint datapoint) {
        int x1 = datapoint.getPoint1().x;
        int y1 = datapoint.getPoint1().y;
        int x2 = datapoint.getPoint2().x - datapoint.getPoint1().x;
        int y2 = datapoint.getPoint2().y - datapoint.getPoint1().y;
        
        // Make sure in range
        x1 = Math.max(0, x1);
        x2 = Math.min(image.getWidth()-1, x2);
        y1 = Math.max(0, y1);
        y2 = Math.min(image.getHeight()-1, y2);
        
        
        BufferedImage subimage = ImageTools.getCroppedBorderImage(
                        openImage, 
                        x1, y1,       // x,y
                        x2, y2     // w
                        );
        
        
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
        
        jlDatapoint.setIcon(new ImageIcon(subimage));    // h\
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
                    Logger.getLogger(ExtractPanel.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ExtractPanel.class.getName()).log(Level.SEVERE, null, ex);
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
        jbPullDatapoints = new javax.swing.JButton();
        jbPullDatapoints1 = new javax.swing.JButton();
        jpPreview = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jlImage = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jspDatapoints = new javax.swing.JScrollPane();
        jpDatapoints = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblStyle = new javax.swing.JLabel();

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

        jbPullDatapoints.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jbPullDatapoints.setText("Extract All Datapoints");
        jbPullDatapoints.setToolTipText("Search the current folder for any known forms and export their data to an excel file.");
        jbPullDatapoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbPullDatapointsActionPerformed(evt);
            }
        });

        jbPullDatapoints1.setFont(new java.awt.Font("Inconsolata", 0, 14)); // NOI18N
        jbPullDatapoints1.setText("Generate Random Brightness");
        jbPullDatapoints1.setToolTipText("Search the current folder for any known forms and export their data to an excel file.");
        jbPullDatapoints1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbPullDatapoints1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2)
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jbPullDatapoints1, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jbChangeFolder, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jbPullDatapoints, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(20, 20, 20))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbChangeFolder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbPullDatapoints)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbPullDatapoints1)
                .addContainerGap(46, Short.MAX_VALUE))
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
            .addComponent(jlImage, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jlImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
            .addGap(0, 393, Short.MAX_VALUE)
        );

        jspDatapoints.setViewportView(jpDatapoints);

        jScrollPane1.setViewportView(jspDatapoints);

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Preview");

        lblStyle.setBackground(new java.awt.Color(255, 255, 255));
        lblStyle.setForeground(new java.awt.Color(255, 255, 255));
        lblStyle.setText("Data Found");

        javax.swing.GroupLayout jpPreviewLayout = new javax.swing.GroupLayout(jpPreview);
        jpPreview.setLayout(jpPreviewLayout);
        jpPreviewLayout.setHorizontalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addGroup(jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 893, Short.MAX_VALUE)
                    .addGroup(jpPreviewLayout.createSequentialGroup()
                        .addGroup(jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(lblStyle))
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
                .addComponent(lblStyle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jbPullDatapointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbPullDatapointsActionPerformed
        onExtractAll();
    }//GEN-LAST:event_jbPullDatapointsActionPerformed

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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton jbChangeFolder;
    private javax.swing.JButton jbPullDatapoints;
    private javax.swing.JButton jbPullDatapoints1;
    private javax.swing.JFileChooser jfcRootChooser;
    private javax.swing.JLabel jlImage;
    private javax.swing.JPanel jpDatapoints;
    private javax.swing.JPanel jpPreview;
    private javax.swing.JScrollPane jspDatapoints;
    private javax.swing.JTree jtrFolders;
    private javax.swing.JLabel lblStyle;
    // End of variables declaration//GEN-END:variables
}
