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
public class TrainPanel extends javax.swing.JPanel {
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
    
    private float minMatchDistance = 100; // threshold where checks will be considered the same style
    
    public TrainPanel() {
        initComponents();
        // color the required buttons
        jbSetSignature.setBackground(Constants.signatureColor);
        jbSetAccount.setBackground(Constants.accountColor);
        jbSetCheckId.setBackground(Constants.checkIdColor);
        jbSetAmount.setBackground(Constants.amountColor);
        
        // Init display values
        lblMatchThreshold.setText("" + this.minMatchDistance);
        
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
        
        jpDatapoints.setLayout(new GridLayout(0, 3));
        
        rootFolder = (File) jtrFolders.getModel().getRoot();
        jfcRootChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Init directory
        jfcRootChooser.setCurrentDirectory(new File ("/Users/alec/Programming/Kaggle/Nature/"));
    }
    
    // export the list of styles
    public void onBuildList() {
        try {
            Gson gson = new Gson();
            String filePath = "data/styles.json";
            FileWriter fileWriter = new FileWriter(filePath);
            String gString = gson.toJson(knownCheckStyles);
            //System.out.println(gString);
            fileWriter.write(gString);
            fileWriter.close();
            
            JOptionPane.showMessageDialog(null, "Styles file saved: " + filePath, "Saved", 1);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Styles could not be saved!");
        }
    }
    
    // add the newly defined check to the styles list
    public void onAddStyle() {
        String styleName = jtaStyleName.getText();
        // check if stylename was empty
        if (styleName == null || "".equals(styleName)) {
            System.out.println("onAddStyle(): no style name entered");
            jtaStyleName.requestFocus();
            jtaStyleName.setToolTipText("Please enter a name for the new style");
            jtaStyleName.setBackground(Color.yellow);
            return;
        }        
        // check if style name already exists
        for (Style check : knownCheckStyles) {
            if (check.getStyle().equals(styleName)) {
                System.out.println("onAddStyle(): check style name already exists in list");
                int response = JOptionPane.showConfirmDialog(this, "Style already exists. Overwrite?");
                if (response != JOptionPane.CANCEL_OPTION || response != JOptionPane.CLOSED_OPTION) {
                    jtaStyleName.requestFocus();
                    return;
                } 
            }
        }
        // Set the name
        openCheck.setStyle(styleName);
        // Add style to list
        knownCheckStyles.add(openCheck);
        // Save file
        String filename = "data/" + styleName + ".jpg";
        openCheck.setFilename(filename);
        File outputFile = new File(filename);
                                
        try {
            ImageIO.write(openImage, "jpg", outputFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
                                
        
        // Reset display
        jtaStyleName.setBackground(Color.white);
        resetDisplay();
        JOptionPane.showMessageDialog(this, "New Style Added: " + styleName, "Added", 1);
    }
    
    // display the image and enable creation of datapoints
    public void onFileSelect() {
        System.out.println("jtrFolderValueChanged(): open " + jtrFolders.getLastSelectedPathComponent());
        resetDisplay();
        // make sure the selection is not a directory
        if (jtrFolders.getModel() != null && !jtrFolders.getModel().isLeaf(jtrFolders.getLastSelectedPathComponent())) {
            System.out.println("jtrFolderValueChanged(): directory selected");
            return;
        }
        // check if file is an image
        String filename = jtrFolders.getLastSelectedPathComponent().toString().toLowerCase();
        if (    // if the name does not contain an image extension dont try to open it (important for working with many images)
            !(
                filename.contains(".jpg")
                || filename.contains(".png")
            )
        ) {
            System.out.println("jtrFolderValueChanged(): file was not an image");
            return;
        }

        try {
            File file = (File) jtrFolders.getLastSelectedPathComponent();
            openImage = ImageIO.read(file);
            openCheck = new Style();
            // openImage = ImagePreprocessor.process(openImage);
            openCheck.init(openImage);
            
            // Check if already matches a style
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
            if (minDistance < minMatchDistance && minDistanceCheck != null) {
                // assign the check's style
                openCheck = minDistanceCheck;
                
                // Show style name
                lblStyle.setText(minDistanceCheck.getStyle() + " " + minDistance);

                displayDatapoint(openImage, minDistanceCheck.getSignature(), "Object 1");
                displayDatapoint(openImage, minDistanceCheck.getAccount(), "Object 2");
                displayDatapoint(openImage, minDistanceCheck.getAmount(), "Object 3");
                
                // Redraw datapoints
                jpDatapoints.revalidate();
                jpDatapoints.repaint();

            } else {
                
                // Show No Match
                lblStyle.setText("No Match");
            }
            
            // make a new image so we dont alter our comparision image
            displayImage = ImageTools.copyImage(openImage);
            jbSetAccount.setEnabled(true);
            jbSetAmount.setEnabled(true);
            jbSetCheckId.setEnabled(true);
            jbSetSignature.setEnabled(true);
            jbAddDatapoint.setEnabled(true);
            // display the check
            displayImage(openImage, jlImage);
        } catch (IOException ex) {
            System.out.println("onFileSelect(): fail");
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
    
    public void displayDatapoint(BufferedImage image, Datapoint datapoint) {
        
        String text = datapoint.getName() ;
        
        displayDatapoint(image, datapoint, text);
        
    }
    
    public void displayDatapoint(BufferedImage image, Datapoint datapoint, String text) {
        int x1 = datapoint.getPoint1().x;
        int y1 = datapoint.getPoint1().y;
        int x2 = datapoint.getPoint2().x - datapoint.getPoint1().x;
        int y2 = datapoint.getPoint2().y - datapoint.getPoint1().y;
        
        // Make sure in range
        x1 = Math.max(0, x1);
        x2 = Math.min(image.getWidth()-1-x1, x2);
        y1 = Math.max(0, y1);
        y2 = Math.min(image.getHeight()-1-y1, y2);
        
        System.out.println("Sub image [" + image.getWidth() + "," + image.getHeight() + "] - [" + x1 + "," + y1 + "," + x2 + "," + y2  + "]");
        BufferedImage subimage = ImageTools.getCroppedBorderImage(
                        image, 
                        x1, y1,       // x,y
                        x2, y2     // w
                        );
        
        
        // ocr the amount field
        String ocrText = null;
        if (datapoint.getName().contains("Amount")) {
            //ocrText = ocr.readImage(image);
        }
        JLabel jlDatapoint = new JLabel(text);
        jlDatapoint.setForeground(Color.white);
        jlDatapoint.setVerticalTextPosition(JLabel.TOP);
        jlDatapoint.setHorizontalTextPosition(JLabel.CENTER);
        
        jlDatapoint.setIcon(new ImageIcon(subimage));    // h\
        jpDatapoints.add(jlDatapoint);
        
        
        
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
                jtaStyleName.requestFocus();
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
        displayImage(displayImage, jlImage);
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
                displayImage(displayImage, jlImage);
            }
        }

        BasicStroke stroke = new BasicStroke(3);
        g2d.setStroke(stroke);
        g2d.drawRect(clickOnePoint.x, clickOnePoint.y, 
                clickTwoPoint.x - clickOnePoint.x, clickTwoPoint.y - clickOnePoint.y);

        displayImage(displayImage, jlImage);

        // reset the click points
        clickOnePoint = null;
        clickTwoPoint = null;
        isMakingDatapoint = false;
    }
    
    private void resetDisplay() {
        jlImage.setIcon(null);
        jtaStyleName.setText("");
        clickOnePoint = null;
        clickTwoPoint = null;
        jbSetAccount.setEnabled(false);
        jbSetAmount.setEnabled(false);
        jbSetCheckId.setEnabled(false);
        jbSetSignature.setEnabled(false);
        jbAddDatapoint.setEnabled(false);
        jpDatapoints.removeAll();
        jpDatapoints.revalidate();
        jpDatapoints.repaint();
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
        jbChangeFolder = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtrFolders = new javax.swing.JTree();
        jbExport = new javax.swing.JButton();
        jtaStyleName = new javax.swing.JTextField();
        jbClear = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jbAddKnownCheck = new javax.swing.JButton();
        jbShowList = new javax.swing.JButton();
        jbAddDatapoint = new javax.swing.JButton();
        jbSetSignature = new javax.swing.JButton();
        jbSetAccount = new javax.swing.JButton();
        jbSetAmount = new javax.swing.JButton();
        jbSetCheckId = new javax.swing.JButton();
        lblStyle = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jlImage = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jpDatapoints = new javax.swing.JPanel();
        sldMatchThreshold = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        lblMatchThreshold = new javax.swing.JLabel();

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

        jbChangeFolder.setText("Change Folder");
        jbChangeFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbChangeFolderActionPerformed(evt);
            }
        });

        jtrFolders.setModel(new FileSystemModel(new File("images/stock"
            + "")));
jtrFolders.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
    public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
        jtrFoldersValueChanged(evt);
    }
    });
    jScrollPane2.setViewportView(jtrFolders);

    jbExport.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbExport.setText("Save Styles");
    jbExport.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbExportActionPerformed(evt);
        }
    });

    jtaStyleName.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jtaStyleName.setToolTipText("Enter a name to identify this style of check.");

    jbClear.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbClear.setText("Clear Styles");
    jbClear.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbClearActionPerformed(evt);
        }
    });

    jLabel1.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jLabel1.setForeground(new java.awt.Color(255, 255, 255));
    jLabel1.setText("Style Name:");
    jLabel1.setToolTipText("Enter a name to identify this style of check.");

    jbAddKnownCheck.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbAddKnownCheck.setText("Add Style");
    jbAddKnownCheck.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbAddKnownCheckActionPerformed(evt);
        }
    });

    jbShowList.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbShowList.setText("Show Styles");
    jbShowList.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbShowListActionPerformed(evt);
        }
    });

    jbAddDatapoint.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbAddDatapoint.setText("Object 5");
    jbAddDatapoint.setEnabled(false);
    jbAddDatapoint.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbAddDatapointActionPerformed(evt);
        }
    });

    jbSetSignature.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbSetSignature.setText("Object 1");
    jbSetSignature.setEnabled(false);
    jbSetSignature.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbSetSignatureActionPerformed(evt);
        }
    });

    jbSetAccount.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbSetAccount.setText("Object 2");
    jbSetAccount.setEnabled(false);
    jbSetAccount.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbSetAccountActionPerformed(evt);
        }
    });

    jbSetAmount.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbSetAmount.setText("Object 3");
    jbSetAmount.setEnabled(false);
    jbSetAmount.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbSetAmountActionPerformed(evt);
        }
    });

    jbSetCheckId.setFont(new java.awt.Font("Inconsolata", 1, 14)); // NOI18N
    jbSetCheckId.setText("Object 4");
    jbSetCheckId.setEnabled(false);
    jbSetCheckId.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jbSetCheckIdActionPerformed(evt);
        }
    });

    jScrollPane1.setBackground(new java.awt.Color(91, 95, 101));
    jScrollPane1.setBorder(null);
    jScrollPane1.setForeground(new java.awt.Color(91, 95, 101));

    jlImage.setBackground(new java.awt.Color(91, 95, 101));
    jlImage.setForeground(new java.awt.Color(91, 95, 101));
    jlImage.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    jlImage.setToolTipText("");
    jlImage.setVerticalAlignment(javax.swing.SwingConstants.TOP);
    jlImage.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    jlImage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    jlImage.setMaximumSize(null);
    jlImage.setMinimumSize(new java.awt.Dimension(500, 259));
    jlImage.setOpaque(true);
    jlImage.setPreferredSize(new java.awt.Dimension(500, 259));
    jlImage.setSize(new java.awt.Dimension(500, 259));
    jlImage.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
    jlImage.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            jlImageMouseClicked(evt);
        }
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            jlImageMouseEntered(evt);
        }
    });
    jScrollPane1.setViewportView(jlImage);

    jpDatapoints.setBackground(new java.awt.Color(51, 51, 51));

    javax.swing.GroupLayout jpDatapointsLayout = new javax.swing.GroupLayout(jpDatapoints);
    jpDatapoints.setLayout(jpDatapointsLayout);
    jpDatapointsLayout.setHorizontalGroup(
        jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 1371, Short.MAX_VALUE)
    );
    jpDatapointsLayout.setVerticalGroup(
        jpDatapointsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 512, Short.MAX_VALUE)
    );

    jScrollPane3.setViewportView(jpDatapoints);

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane3)
                .addComponent(jScrollPane1))
            .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 802, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane3))
    );

    jScrollPane4.setViewportView(jPanel1);

    sldMatchThreshold.setMaximum(500);
    sldMatchThreshold.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            sldMatchThresholdStateChanged(evt);
        }
    });

    jLabel2.setText("Match Threshold");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jbChangeFolder, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jbExport, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(jbAddKnownCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jtaStyleName, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jbShowList, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbClear, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(lblStyle, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(6, 6, 6)))
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jbSetSignature)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jbSetAccount))
                        .addComponent(jbAddDatapoint)
                        .addComponent(sldMatchThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jbSetAmount)
                                .addComponent(jLabel2))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jbSetCheckId)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(lblMatchThreshold, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGap(21, 21, 21)))))))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 1391, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jpDatapointsDELETE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(68, 68, 68))
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jbAddDatapoint, jbSetAccount, jbSetAmount, jbSetCheckId, jbSetSignature});

    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jpDatapointsDELETE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
            .addContainerGap())
        .addGroup(layout.createSequentialGroup()
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jbChangeFolder)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lblStyle, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jtaStyleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel1))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jbAddKnownCheck)
                .addComponent(jbExport))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jbShowList)
                .addComponent(jbClear))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jbSetSignature)
                .addComponent(jbSetAccount))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jbSetAmount)
                .addComponent(jbSetCheckId))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jbAddDatapoint)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(lblMatchThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(sldMatchThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );
    }// </editor-fold>//GEN-END:initComponents

    private void jbAddKnownCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAddKnownCheckActionPerformed
        onAddStyle();
    }//GEN-LAST:event_jbAddKnownCheckActionPerformed

    private void jbExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbExportActionPerformed
        onBuildList();
    }//GEN-LAST:event_jbExportActionPerformed

    private void jbClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbClearActionPerformed
        onClear();
    }//GEN-LAST:event_jbClearActionPerformed

    private void jbShowListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbShowListActionPerformed
        onShow();
    }//GEN-LAST:event_jbShowListActionPerformed

    private void jlImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlImageMouseClicked
        //System.out.println("( " + evt.getX() + " , " + evt.getY() + " )");
        onImageClick(evt.getX(), evt.getY());
    }//GEN-LAST:event_jlImageMouseClicked

    private void jlImageMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jlImageMouseEntered

    }//GEN-LAST:event_jlImageMouseEntered

    private void jbAddDatapointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbAddDatapointActionPerformed
        if (openCheck != null && !isMakingDatapoint) {
            isMakingDatapoint = true;
        }
    }//GEN-LAST:event_jbAddDatapointActionPerformed

    private void jbSetSignatureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSetSignatureActionPerformed
        isMakingSignature = true;
        isMakingDatapoint = true;
        clickOnePoint = null; 
        clickTwoPoint = null;
    }//GEN-LAST:event_jbSetSignatureActionPerformed

    private void jbSetAccountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSetAccountActionPerformed
        isMakingAccount = true;
        isMakingDatapoint = true;
        clickOnePoint = null; 
        clickTwoPoint = null;
    }//GEN-LAST:event_jbSetAccountActionPerformed

    private void jbSetAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSetAmountActionPerformed
        isMakingAmount = true;
        isMakingDatapoint = true;
        clickOnePoint = null; 
        clickTwoPoint = null;
    }//GEN-LAST:event_jbSetAmountActionPerformed

    private void jbSetCheckIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSetCheckIdActionPerformed
        isMakingCheckId = true;
        isMakingDatapoint = true;
        clickOnePoint = null; 
        clickTwoPoint = null;
    }//GEN-LAST:event_jbSetCheckIdActionPerformed

    private void jtrFoldersValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jtrFoldersValueChanged
        onFileSelect();
    }//GEN-LAST:event_jtrFoldersValueChanged

    private void jbChangeFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbChangeFolderActionPerformed
        onChangeRoot();
    }//GEN-LAST:event_jbChangeFolderActionPerformed

    private void sldMatchThresholdStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldMatchThresholdStateChanged
        float value = sldMatchThreshold.getValue();
        this.minMatchDistance = value;
        lblMatchThreshold.setText("" + this.minMatchDistance);
    }//GEN-LAST:event_sldMatchThresholdStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JButton jbAddDatapoint;
    private javax.swing.JButton jbAddKnownCheck;
    private javax.swing.JButton jbChangeFolder;
    private javax.swing.JButton jbClear;
    private javax.swing.JButton jbExport;
    private javax.swing.JButton jbSetAccount;
    private javax.swing.JButton jbSetAmount;
    private javax.swing.JButton jbSetCheckId;
    private javax.swing.JButton jbSetSignature;
    private javax.swing.JButton jbShowList;
    private javax.swing.JFileChooser jfcRootChooser;
    private javax.swing.JLabel jlImage;
    private javax.swing.JPanel jpDatapoints;
    private javax.swing.JPanel jpDatapointsDELETE;
    private javax.swing.JTextField jtaStyleName;
    private javax.swing.JTree jtrFolders;
    private javax.swing.JLabel lblMatchThreshold;
    private javax.swing.JLabel lblStyle;
    private javax.swing.JSlider sldMatchThreshold;
    // End of variables declaration//GEN-END:variables
}
