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
import java.util.LinkedHashMap;
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
import static marvin.MarvinPluginCollection.mergePhotos;

public class Scene extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private ArrayList<Style> knownCheckStyles;
    private Style openCheck;
    private BufferedImage openImage, displayImage;
    private final float minMatchDistance = 100; // threshold where checks will be considered the same style
    private File rootFolder;
    
    private float matchThreshold = 0.5f;
    private LinkedHashMap<String,ArrayList<String>> scenes ;
    
    // Camera background
    private MarvinImagePlugin cameraBackgroundPlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.background.determineFixedCameraBackground");
    // Scene plugin
    private int sceneThreshold = 100;
    private MarvinImagePlugin scenePlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.background.determineSceneBackground");
    
    
    private int mergeThreshold = 38;
    private MarvinImagePlugin mergePlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.combine.mergePhotos");
    private MarvinImage mergedOutput;
            
            
    public Scene() {
        initComponents();
        jpDatapoints.setLayout(new GridLayout(0, 2));
        readStyles();
        
        rootFolder = (File) jtrFolders.getModel().getRoot();
        jfcRootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Init directory
        jfcRootChooser.setCurrentDirectory(new File ("/Users/alec/Programming/Kaggle/Nature/"));
        
        scenes = new LinkedHashMap<String,ArrayList<String>>();
        
        scenePlugin.setAttribute("threshold", sceneThreshold);
        
        
        // Init value labels
        lblMatchThreshold.setText("" + (int)this.matchThreshold);
        lblSceneThreshold.setText("" + (int)this.sceneThreshold);
        lblMergeThreshold.setText("" + (int)this.mergeThreshold);
        
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
        System.out.println("jtrFolders : " + jtrFolders.getName());
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
        
        File selectedFile = (File) jtrFolders.getLastSelectedPathComponent();            
        try {
            openImage = ImageIO.read(selectedFile);
            //displayImage = openImage;
            openCheck = new Style();
            //displayImage = ImageTools.copyImage(openImage);
            //openImage = ImagePreprocessor.process(openImage);
            openCheck.init(openImage);
            System.out.println("openCheck.getStyle() = " + openCheck.getStyle());
            //displayImage(displayImage, jlImage, openCheck.getStyle());
            
            
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
            }
            
            
            // Show before
            displayImage(openImage, jlBefore, openCheck.getStyle());
            
        } catch (IOException ex) {
            Logger.getLogger(Scene.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ArrayList<String> matchedFileNames = new ArrayList<String>();
        ArrayList<MarvinImage> sceneImages = new ArrayList<MarvinImage>();
        scenes = new LinkedHashMap<String,ArrayList<String>>();
        int fileIndex = 1;
//         for each file
        for (File file : rootFolder.listFiles()) {    
            System.out.println("file " + fileIndex + " = " + file);
            float percentDone = (float)(fileIndex) / (float)(rootFolder.listFiles().length) * 100.0f;
            pgProgress.setValue((int)percentDone);
                    
            
            // TODO: add support for other image formats, right now only jpg because of canny edge?
            if (file.getName().contains(".jpg") || file.getName().contains(".png")) {
                try {
                    BufferedImage testImage = ImageIO.read(file);
                    Style testImageStyle = new Style();    // need a new one every time?
                    //testImage = ImagePreprocessor.process(testImage);
                    testImageStyle.init(testImage);
                    
                    float distance = openCheck.compare(testImageStyle);
                    System.out.println("distance = " + distance);
                        
                    // if the minimum matched distance is in the threshold
                    if (distance < this.matchThreshold) {
                        matchedFileNames.add(file.getName());
                        
                        sceneImages.add(new MarvinImage(testImage));
                    } 
                } catch (IOException ex) {
                    Logger.getLogger(Scene.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            fileIndex += 1;
        }
        System.out.println("matchedFileNames: \n" + matchedFileNames + "\n\n");
        scenes.put(openCheck.getStyle(), matchedFileNames);
        
        
        // Merge scene images
        mergePlugin.setAttribute("threshold", mergeThreshold);
        // Show number of images in scene
        System.out.println("sceneImages.size() = " + sceneImages.size());
        
        try {
            if (!sceneImages.isEmpty()) {
                
                // Detect scene camera background
                MarvinImage sceneBackgroundImage = sceneImages.get(0).clone();
                scenePlugin.process(sceneImages, sceneBackgroundImage);
                sceneBackgroundImage.update();
                addDatapoint(sceneBackgroundImage.getBufferedImage(), "Scene Background");
                
                
                // Detect fixed camera background
                MarvinImage backgroundImage = sceneImages.get(0).clone();
                cameraBackgroundPlugin.process(sceneImages, backgroundImage);
                backgroundImage.update();
                addDatapoint(backgroundImage.getBufferedImage(), "Camera Background");
                
                
                // Show after
                displayImage(sceneBackgroundImage.getBufferedImage(), jlAfter, "Scene Background");
                
                // Merge scene
                //mergedOutput = sceneImages.get(0).clone();

                //mergePhotos(sceneImages, mergedOutput, mergeThreshold);
                //mergedOutput.update();
                //addDatapoint(mergedOutput.getBufferedImage(), "Merged");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        try {
            for (int index = 0; index < matchedFileNames.size(); index++) {
                String matchedFilename = matchedFileNames.get(index);
                MarvinImage matchedImage = sceneImages.get(index);
                addDatapoint(matchedImage.getBufferedImage(), matchedFilename);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
                        
        
        
        jpDatapoints.revalidate();
        jpDatapoints.repaint();
        System.out.println("scenes: \n" + scenes.toString() + "\n\n");
        
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
        
        if (openImage == null || openCheck.getStyle() == null) {
            return;
        }
        
        ArrayList<String> matchedFieldNames = new ArrayList<String>();
        
//         for each file
        for (File file : rootFolder.listFiles()) {             
            // TODO: add support for other image formats, right now only jpg because of canny edge?
            if (file.getName().contains(".jpg") || file.getName().contains(".png")) {
                try {
                    BufferedImage testImage = ImageIO.read(file);
                    openCheck = new Style();    // need a new one every time?
                    //testImage = ImagePreprocessor.process(testImage);
                    openCheck.init(testImage);
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
                        // Save file in folder for scene
                        String[] oldName = file.getName().split("\\.");
                        String newName = "scenes/" + minDistanceCheck.getStyle() + "/" + oldName[0] + ".jpg";
                        String filename = "output/"  + rootFolder.getName() + "/" + newName;
                        File outputFile = new File(filename);
                        if (!outputFile.exists()) {
                            outputFile.mkdirs();
                        }

                        ImageIO.write(testImage, "jpg", outputFile);

                        
                    } else {
                        System.out.println("Error: no match for " + file.getName());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Scene.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        resetDisplay();
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
            Logger.getLogger(Scene.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void onExtractSelected() {
        // TODO: this
    }
    
    public void resetDisplay() {
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
        jScrollPane2 = new javax.swing.JScrollPane();
        jtrFolders = new javax.swing.JTree();
        jbChangeFolder = new javax.swing.JButton();
        jbPullDatapoints = new javax.swing.JButton();
        sldThreshold = new javax.swing.JSlider();
        jButton1 = new javax.swing.JButton();
        sldMergeThreshold = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        sldSceneThreshold = new javax.swing.JSlider();
        lblMatchThreshold = new javax.swing.JLabel();
        lblMergeThreshold = new javax.swing.JLabel();
        lblSceneThreshold = new javax.swing.JLabel();
        pgProgress = new javax.swing.JProgressBar();
        jpPreview = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jspDatapoints = new javax.swing.JScrollPane();
        jpDatapoints = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jlAfter = new javax.swing.JLabel();
        jlBefore = new javax.swing.JLabel();

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
        jbPullDatapoints.setText("Find Similar");
        jbPullDatapoints.setToolTipText("Search the current folder for any known forms and export their data to an excel file.");
        jbPullDatapoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbPullDatapointsActionPerformed(evt);
            }
        });

        sldThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldThresholdStateChanged(evt);
            }
        });

        jButton1.setText("Clear Scenes");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        sldMergeThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldMergeThresholdStateChanged(evt);
            }
        });

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Merge Threshold");

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Match Threshold");

        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Scene Threshold");

        sldSceneThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldSceneThresholdStateChanged(evt);
            }
        });

        lblMatchThreshold.setText("10");

        lblMergeThreshold.setText("10");

        lblSceneThreshold.setText("10");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jbChangeFolder, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                    .addComponent(jScrollPane2)
                    .addComponent(sldThreshold, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jbPullDatapoints, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sldSceneThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(sldMergeThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblMatchThreshold))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblMergeThreshold))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblSceneThreshold))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(pgProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jbChangeFolder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbPullDatapoints)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lblMatchThreshold))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(lblMergeThreshold))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldMergeThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(lblSceneThreshold))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldSceneThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pgProgress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addComponent(jButton1))
        );

        jpPreview.setBackground(new java.awt.Color(91, 95, 101));

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
            .addGap(0, 2114, Short.MAX_VALUE)
        );
        jpDatapointsLayout.setVerticalGroup(
            jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 764, Short.MAX_VALUE)
        );

        jspDatapoints.setViewportView(jpDatapoints);

        jScrollPane1.setViewportView(jspDatapoints);

        javax.swing.GroupLayout jpPreviewLayout = new javax.swing.GroupLayout(jpPreview);
        jpPreview.setLayout(jpPreviewLayout);
        jpPreviewLayout.setHorizontalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1069, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 12, Short.MAX_VALUE))
        );
        jpPreviewLayout.setVerticalGroup(
            jpPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpPreviewLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 426, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(51, 51, 51));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jlBefore, javax.swing.GroupLayout.PREFERRED_SIZE, 503, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlAfter, javax.swing.GroupLayout.PREFERRED_SIZE, 513, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jlAfter, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
                    .addComponent(jlBefore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jpPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jbPullDatapointsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbPullDatapointsActionPerformed
        onExtractAll();
    }//GEN-LAST:event_jbPullDatapointsActionPerformed

    private void jtrFoldersValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jtrFoldersValueChanged
        onFileSelect();
    }//GEN-LAST:event_jtrFoldersValueChanged

    private void jbChangeFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbChangeFolderActionPerformed
        onChangeRoot();
    }//GEN-LAST:event_jbChangeFolderActionPerformed

    private void sldThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldThresholdStateChanged
        float oldValue = this.matchThreshold;
        this.matchThreshold = sldThreshold.getValue() / 10.0f;
        
        lblMatchThreshold.setText("" + this.matchThreshold);
        System.out.println("Distance Threshold : " + oldValue + " => " +  this.matchThreshold);
    }//GEN-LAST:event_sldThresholdStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        scenes = new LinkedHashMap<String,ArrayList<String>>();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void sldMergeThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldMergeThresholdStateChanged
        float oldValue = this.mergeThreshold;
        // Save value
        this.mergeThreshold = sldMergeThreshold.getValue();
        // Update label with value
        lblMergeThreshold.setText("" + (int)this.mergeThreshold);
        System.out.println("Merge Threshold : " + oldValue + " => " +  this.mergeThreshold);
    }//GEN-LAST:event_sldMergeThresholdStateChanged

    private void sldSceneThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldSceneThresholdStateChanged
        // Get value from slider
        int value = sldSceneThreshold.getValue();
        // Save value
        this.sceneThreshold = value;        
        // Update label with value
        lblSceneThreshold.setText("" + (int)this.sceneThreshold);
        scenePlugin.setAttribute("threshold", value);
    }//GEN-LAST:event_sldSceneThresholdStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton jbChangeFolder;
    private javax.swing.JButton jbPullDatapoints;
    private javax.swing.JFileChooser jfcRootChooser;
    private javax.swing.JLabel jlAfter;
    private javax.swing.JLabel jlBefore;
    private javax.swing.JPanel jpDatapoints;
    private javax.swing.JPanel jpPreview;
    private javax.swing.JScrollPane jspDatapoints;
    private javax.swing.JTree jtrFolders;
    private javax.swing.JLabel lblMatchThreshold;
    private javax.swing.JLabel lblMergeThreshold;
    private javax.swing.JLabel lblSceneThreshold;
    private javax.swing.JProgressBar pgProgress;
    private javax.swing.JSlider sldMergeThreshold;
    private javax.swing.JSlider sldSceneThreshold;
    private javax.swing.JSlider sldThreshold;
    // End of variables declaration//GEN-END:variables
}
