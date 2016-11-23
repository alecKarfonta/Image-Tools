package com.alec.form.Views;

// This class simply holds the different tabs available to the user

import javax.swing.UIManager;

// TODO: add different user levels

public class CBIRE extends javax.swing.JFrame {
    ExtractPanel extractPanel;
    TrainPanel addStylesPanel;
    GeneratePanel generatePanel;
    Scene scenePanel;
    Blur blurPanel;
    OutlinePanel outlinePanel;
    StylesPanel stylesPanel;
    
    public CBIRE() {
        initComponents();
        extractPanel = new ExtractPanel();
        addStylesPanel = new TrainPanel();
        generatePanel = new GeneratePanel();
        scenePanel = new Scene();
        blurPanel = new Blur();
        outlinePanel = new OutlinePanel();
        stylesPanel = new StylesPanel();
        
        jtpTabs.addTab("Generate", generatePanel );
        jtpTabs.addTab("Train", addStylesPanel );
        jtpTabs.addTab("Styles", stylesPanel );
        jtpTabs.addTab("Extract", extractPanel );
        jtpTabs.addTab("Scene", scenePanel );
        jtpTabs.addTab("Blur", blurPanel );
        jtpTabs.addTab("Outline", outlinePanel );
        
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jtpTabs = new javax.swing.JTabbedPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CBIRE 0.1");
        setAutoRequestFocus(false);
        setBackground(new java.awt.Color(255, 255, 255));

        jtpTabs.setBackground(new java.awt.Color(255, 255, 255));
        jtpTabs.setBorder(null);
        jtpTabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jtpTabsStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jtpTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jtpTabs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jtpTabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jtpTabsStateChanged
        
        int index = jtpTabs.getSelectedIndex();
        System.out.println("Tab changed to: " + jtpTabs.getTitleAt(index));
        if ((jtpTabs.getTitleAt(index)).contains("Extract")) {
            extractPanel.onTabSwitchIn();
        }
    }//GEN-LAST:event_jtpTabsStateChanged


    public static void main(String args[]) {
        
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
                UIManager.setLookAndFeel("com.jtattoo.plaf.acryl.AcrylLookAndFeel");
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CBIRE.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CBIRE.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CBIRE.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CBIRE.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>


        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CBIRE().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jtpTabs;
    // End of variables declaration//GEN-END:variables
}
