package be.libis.lias.pdftool;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
//import java.util.logging.Logger;

import be.libis.lias.toolbox.RandomString;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.awt.geom.*;

/**
 * Class that combines all operations for copying PDF documents.
 * 
 * The class is capable of creating a copy of a PDF and:
 * <ul>
 * <li>apply a watermark image or text, including many custom settings
 * <li>protect the PDF against printing, copying or modification
 * <li>protect the PDF with a password
 * <li>select a subset of the pages to be copied
 * <li>change the metadata info
 * </ul>
 */
public class Copy {

//  private static final Logger logger        = Logger.getLogger(Copy.class.getName());

  private static PdfName      blending_mode = PdfGState.BM_HARDLIGHT;
  private static float        opacity       = 0.1f;
  private static float        gap_ratio     = 0.5f;
  private static float        gap_size      = 0f;
  private static float        font_size     = 20f;
  private static float        text_rotation = 15.0f;
  private static BaseFont     bf;

  /**
   * Main application entry point.
   * 
   * The method parses the command line arguments and checks them for common mistakes.
   * The real work is left to the class constructor which is call at the end.
   * 
   * @param args the command line arguments
   * @throws IOException
   */

  /**
   * Helper class to calculate image and text box sizes.
   * 
   * This class takes the text size and rotation into account. It also deals with
   * gap sizes.
   */
  private class Size {
    public float real_width; /** the width of the object's bounding box */ 
    public float real_height; /** the height of the object's bounding box */
    public float gap_width; /** the width of the whitespace around the object */
    public float gap_height; /** the height of the whitespace around the object */
    public float total_width; /** the total width of the object including the whitespace */
    public float total_height; /** the total height of the object including the whitespace */
    public float start_height = 0f; /** the extra height from where to start the object */

    /**
     * Constructor for a text box
     * 
     * @param watermark_text
     */
    public Size(List<String> watermark_text) {
      float cosine = (float) Math.abs(Math.cos(Math.toRadians(text_rotation)));
      float sine = (float) Math.abs(Math.sin(Math.toRadians(text_rotation)));
      float w = 0f;
      for (String text : watermark_text) {
        float _w = bf.getWidthPointKerned(text, font_size);
        if (_w > w)
          w = _w;
      }
      // this is the heigth that the next lines take
      start_height = (watermark_text.size() - 1) * (font_size * 1.5f); 
      float h = bf.getAscentPoint(watermark_text.get(0), font_size)
          - bf.getDescentPoint(watermark_text.get(0), font_size) + start_height;
      real_width = w * cosine + h * sine;
      real_height = w * sine + h * cosine;
      calculate_derived_sizes();
    }

    /**
     * Constructor for an image
     * 
     * @param image
     */
    public Size(Image image) {
      real_width = image.getWidth();
      real_height = image.getHeight();
      calculate_derived_sizes();
    }

    /**
     * method that calculates the gap and total sizes once the object's bounding box size is known
     */
    private void calculate_derived_sizes() {
      gap_width = real_width * gap_ratio + gap_size;
      gap_height = real_height * gap_ratio + gap_size;
      total_width = real_width + gap_width;
      total_height = real_height + gap_height;
    }
  }

  /**
   * Constructor that performs the real work.
   * 
   * @param source The PDF to be copied
   * @param target The PDF to be created
   * @param options The command line options
   */
  public Copy(File source, File target, CopyOptions options) {
    try {
      
      if (options.isOpacity())
        opacity = options.getOpacity();
      if (options.isGapRatio())
        gap_ratio = options.getGapRatio();
      if (options.isGapSize())
        gap_size = options.getGapSize();
      if (options.isFontSize())
        font_size = options.getFontSize();
      if (options.isTextRotation())
        text_rotation = options.getTextRotation();

      // Create reader on the source PDF and select the pages that were requested
      PdfReader reader = new PdfReader(source.getAbsolutePath());
      if (options.isPageRanges()) {
        reader.selectPages(options.getPageRanges());
      }
      int n = reader.getNumberOfPages();

      // Create a document and copier for the target PDF
      Document document = new Document();
      PdfCopy copy = new PdfCopy(document, new FileOutputStream(target));
      copy.setPdfVersion(PdfCopy.VERSION_1_6);

      // Set encryption and compression on the target PDF
      String owner_password = new RandomString(32).nextString();
      String user_password = (options.isPassword() ? options.getPassword() : "");
      int permissions = 0 + (options.getAllowPrint() ? PdfCopy.ALLOW_PRINTING : 0)
          + (options.getAllowCopy() ? PdfCopy.ALLOW_COPY : 0)
          + (options.getAllowAssembly() ? PdfCopy.ALLOW_ASSEMBLY : 0)
          + (options.getAllowAnnotations() ? PdfCopy.ALLOW_MODIFY_ANNOTATIONS : 0);
      int encryption = PdfCopy.ENCRYPTION_AES_128 | PdfCopy.DO_NOT_ENCRYPT_METADATA;

      copy.setEncryption(user_password.getBytes(), owner_password.getBytes(), permissions, encryption);
      
      copy.setFullCompression();

      // The encryption should be defined before the document is opened
      document.open();
      
      // Collect metadata from the source PDF, then overwrite with the command line options
      // and set the resulting metadata info on the target PDF
      HashMap<String, String> info = reader.getInfo();
      String key = "Title";
      if (options.isTitle())
        info.put(key, options.getTitle());
      if (info.containsKey(key))
        document.addTitle(info.get(key));
      key = "Subject";
      if (options.isSubject())
        info.put(key, options.getSubject());
      if (info.containsKey(key))
        document.addSubject(info.get(key));
      key = "Keywords";
      if (options.isKeywords())
        info.put(key, options.getKeywords());
      if (info.containsKey(key))
        document.addKeywords(info.get(key));
      key = "Creator";
      if (options.isCreator())
        info.put(key, options.getCreator());
      if (info.containsKey(key))
          document.addCreator(info.get(key));
      key = "Author"; 
      if (options.isAuthor())
        info.put(key, options.getAuthor());
      if (info.containsKey(key))
        document.addAuthor(info.get(key));

      // Initialize the watermark objects
      Image image = null;
      List<String> watermark_text = null;
      Size size = null;

      if (options.isWatermarkText()) {
        bf = BaseFont.createFont("data/FreeSansBold.ttf", BaseFont.WINANSI, BaseFont.EMBEDDED);
        watermark_text = options.getWatermarkText();
        size = new Size(watermark_text);
      } else if (options.isWatermarkImage()) {
        image = Image.getInstance(options.getWatermarkImage().getAbsolutePath());
        size = new Size(image);
      }

      // required for ???
      copy.createXmpMetadata();

      // Process each page from the reader selection
      for (int index = 1; index <= n; index++) {
        PdfImportedPage page = copy.getImportedPage(reader, index);
        PdfCopy.PageStamp stamper = copy.createPageStamp(page);
        // Process watermark objects if required
        if (size != null) {
          // get over content to put watermark on top of all other objects
          PdfContentByte canvas = stamper.getOverContent();
          
          // create and set the graphics state
          canvas.beginText();
          PdfGState pdfGState = new PdfGState();
          pdfGState.setFillOpacity(opacity);
          pdfGState.setStrokeOpacity(opacity);
          pdfGState.setBlendMode(blending_mode);
          canvas.saveState();
          canvas.setGState(pdfGState);

          // set font for text watermark
          if (watermark_text != null) {
            canvas.setFontAndSize(bf, font_size);
            canvas.setColorFill(BaseColor.BLACK);
          }

          // get the page dimensions
          Rectangle dimensions = page.getBoundingBox();
          float xl = dimensions.getLeft();
          float xr = dimensions.getRight();
          float yb = dimensions.getBottom();
          float yt = dimensions.getTop();

          // affine transformation is actually ony required for text watermark
          // but we create it here to avoid the costly object creation for each text instancee
          AffineTransform af = new AffineTransform();
          
          // the two nested loops create the matrix of watermark objects on the page
          float x = xl + size.gap_width / 2f;
          while (x < xr) {
            float y = yb + size.gap_height / 2f + size.start_height;
            while (y < yt) {
              if (image != null) {
                canvas.addImage(image, size.real_width, 0, 0, size.real_height, x, y);
              } else {
                // Text position and rotation is performed by the affine transformation matrix
            	  af.translate(x, y);
            	  af.rotate(Math.toRadians(text_rotation));
            	  canvas.transform(af);
                af.setToTranslation(x, y);
                af.rotate(Math.toRadians(text_rotation));
                canvas.setTextMatrix(af);

                float delta_y = - font_size * 1.5f;
                for (String text : watermark_text) {
                  canvas.showTextKerned(text);
                  canvas.newlineText();
                  canvas.moveText(0, delta_y);
                }
              }
              y += size.total_height;
            }
            x += size.total_width;
          }

          canvas.restoreState();

          canvas.endText();

          stamper.alterContents();
        }
        copy.addPage(page);
      }
      document.close();
      reader.close();
    } catch (Exception e) {
      System.out.format("Caught exception: %s", e.toString());
      e.printStackTrace(System.out);
    }
  }

}
