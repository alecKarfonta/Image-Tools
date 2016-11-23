package com.alec.form.Controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelTools {
   
    public static Workbook openWorkbook(String filePath) throws FileNotFoundException, IOException, InvalidFormatException {
            // create an input stream to read the existing excel file
            InputStream inp = new FileInputStream(filePath);
            
            // create the workbook from the input stream
            Workbook wb = WorkbookFactory.create(inp);
            
            return wb;
    }
    
    public static boolean writeExcel(Workbook wb, String filePath) {
        
        //Write the Excel file
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(filePath + ".xlsx");
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        
        return true;
    }
    
    public static void addPictureToExcel(Workbook wb, Sheet sheet, BufferedImage image, int row, int col) throws FileNotFoundException, IOException {
                // make a new row, for resizing later
                sheet.createRow(row);
                sheet.getRow(row).createCell(col);
        
                //Returns an object that handles instantiating concrete classes
                CreationHelper helper = wb.getCreationHelper();

                //Creates the top-level drawing patriarch.
                Drawing drawing = sheet.createDrawingPatriarch();

                //Create an anchor that is attached to the worksheet
                ClientAnchor anchor = helper.createClientAnchor();
                /**/
                //Get the contents of an InputStream as a byte[].
                ByteArrayOutputStream baos = new ByteArrayOutputStream();   
                ImageIO.write(image, "png", baos);
                baos.flush();
                byte[] bytes = baos.toByteArray();
                baos.close();
                
                //Adds a picture to the workbook
                int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                
                //set top-left corner for the image
                anchor.setCol1(col);
                anchor.setRow1(row);
                
                //Creates a picture
                Picture pict = drawing.createPicture(anchor, pictureIdx);
                
                /**/
                //System.out.println("height: " + (short)image.getHeight());
                
                float points = image.getWidth() * 72 / 96;
                
                sheet.getRow(row).setHeightInPoints((short)image.getHeight());
                //Reset the image to the original size
                //sheet.autoSizeColumn(col, true);
                
                pict.resize();
    }
     
}
