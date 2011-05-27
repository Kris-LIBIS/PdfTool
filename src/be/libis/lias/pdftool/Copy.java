package be.libis.lias.pdftool;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import be.libis.lias.toolbox.GeneralOptionsManager;
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

public class Copy {

  private static final Logger logger        = Logger.getLogger(Copy.class.getName());

  private static PdfName      blending_mode = PdfGState.BM_HARDLIGHT;
  private static float        opacity       = 0.1f;
  private static float        gap_ratio     = 0.5f;
  private static float        gap_size      = 0f;
  private static float        font_size     = 20f;
  private static float        text_rotation = 15.0f;
  private static BaseFont     bf;

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    CopyOptions options = new GeneralOptionsManager<CopyOptions>().processOptions(CopyOptions.class, args, logger);

    if (options == null)
      return; // error messages are printed by GeneralOptionsHandler

    if ((!options.isWatermarkText() && !options.isWatermarkImage())
        || (options.isWatermarkText() && options.isWatermarkImage())) {
      logger.severe("Either watermark text or watermark image must be specified.");
      return;
    }

    File source = options.getSourceFile();

    if (!source.exists()) {
      logger.severe("Source file '" + source.getAbsolutePath() + "' not found.");
      return;
    }

    if (!source.canRead()) {
      logger.severe("Source file '" + source.getAbsolutePath() + "' cannot be read.");
      return;
    }

    File target = options.getTargetFile();

    if (!target.exists())
      target.createNewFile();

    if (!target.canWrite()) {
      logger.severe("Target file '" + target.getAbsolutePath() + "' cannot be written.");
      return;
    }

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

    new Copy(source, target, options);
  }

  private class Size {
    public float real_width;
    public float real_height;
    public float gap_width;
    public float gap_height;
    public float total_width;
    public float total_height;

    public Size(List<String> watermark_text) {
      float cosine = (float) Math.abs(Math.cos(Math.toRadians(text_rotation)));
      float sine = (float) Math.abs(Math.sin(Math.toRadians(text_rotation)));
      float w = 0f;
      for (String text : watermark_text) {
        float _w = bf.getWidthPointKerned(text, font_size);
        if (_w > w)
          w = _w;
      }
      float h = bf.getAscentPoint(watermark_text.get(0), font_size)
          - bf.getDescentPoint(watermark_text.get(0), font_size) + (watermark_text.size() - 1) * (font_size * 1.5f);
      real_width = w * cosine + h * sine;
      real_height = w * sine + h * cosine;
      calculate_derived_sizes();
    }

    public Size(Image image) {
      real_width = image.getWidth();
      real_height = image.getHeight();
      calculate_derived_sizes();
    }

    private void calculate_derived_sizes() {
      gap_width = real_width * gap_ratio + gap_size;
      gap_height = real_height * gap_ratio + gap_size;
      total_width = real_width + gap_width;
      total_height = real_height + gap_height;
    }
  }

  Copy(File source, File target, CopyOptions options) {
    try {
      boolean do_watermark = options.isWatermarkText() || options.isWatermarkImage();

      PdfReader reader = new PdfReader(source.getAbsolutePath());
      if (options.isPageRanges()) {
        reader.selectPages(options.getPageRanges());
      }
      int n = reader.getNumberOfPages();

      Document document = new Document();
      PdfCopy copy = new PdfCopy(document, new FileOutputStream(target));
      copy.setPdfVersion(PdfCopy.VERSION_1_7);

      // Encryption
      String owner_password = new RandomString(32).nextString();
      String user_password = (options.isPassword() ? options.getPassword() : "");
      int permissions = 0 + (options.getAllowPrint() ? PdfCopy.ALLOW_PRINTING : 0)
          + (options.getAllowCopy() ? PdfCopy.ALLOW_COPY : 0)
          + (options.getAllowAssembly() ? PdfCopy.ALLOW_ASSEMBLY : 0)
          + (options.getAllowAnnotations() ? PdfCopy.ALLOW_MODIFY_ANNOTATIONS : 0);
      int encryption = PdfCopy.ENCRYPTION_AES_128 | PdfCopy.DO_NOT_ENCRYPT_METADATA;

      copy.setEncryption(user_password.getBytes(), owner_password.getBytes(), permissions, encryption);

      // The encryption should be defined before the document is opened
      document.open();
      
      // Metadata
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

      copy.createXmpMetadata();

      for (int index = 1; index <= n; index++) {
        PdfImportedPage page = copy.getImportedPage(reader, index);
        Rectangle dimensions = page.getBoundingBox();
        PdfCopy.PageStamp stamper = copy.createPageStamp(page);
        if (do_watermark) {
          PdfContentByte cb = stamper.getOverContent();
          cb.beginText();
          PdfGState gstate = new PdfGState();
          gstate.setFillOpacity(opacity);
          gstate.setStrokeOpacity(opacity);
          gstate.setBlendMode(blending_mode);
          cb.saveState();
          cb.setGState(gstate);

          if (watermark_text != null) {
            cb.setFontAndSize(bf, font_size);
            cb.setColorFill(BaseColor.BLACK);
          }

          float xl = dimensions.getLeft();
          float xr = dimensions.getRight();
          float yb = dimensions.getBottom();
          float yt = dimensions.getTop();

          AffineTransform transform = new AffineTransform();
          float x = xl + size.gap_width / 2f;
          while (x < xr) {
            float y = yb + size.gap_height / 2f;
            while (y < yt) {
              if (image != null) {
                cb.addImage(image, size.real_width, 0, 0, size.real_height, x, y);
              } else {
                transform.setToTranslation(x, y);
                transform.rotate(Math.toRadians(text_rotation));
                cb.setTextMatrix(transform);

                float local_y = 0;
                for (String text : watermark_text) {
                  cb.moveText(0, local_y);
                  cb.showTextKerned(text);
                  local_y -= font_size * 1.5f;
                  cb.newlineText();
                }
              }
              y += size.total_height;
            }
            x += size.total_width;
          }

          cb.restoreState();

          cb.endText();

          stamper.alterContents();
        }
        copy.addPage(page);
      }
      document.close();
      reader.close();
    } catch (Exception e) {
      System.out.format("Caught exception: %s", e.toString());
    }
  }

}
