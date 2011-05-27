package be.libis.lias.pdftool;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;
import be.libis.lias.toolbox.GeneralOptions;

public interface CopyOptions extends GeneralOptions {
  @Option(shortName = "i", longName = "file_input", description = "Input PDF file")
  File getSourceFile();

  @Option(shortName = "o", longName = "file_output", description = "Output PDF file")
  File getTargetFile();

  @Option(longName = "md_title", description = "PDF Metadata title value")
  String getTitle();

  boolean isTitle();

  @Option(longName = "md_author", description = "PDF Metadata author value")
  String getAuthor();

  boolean isAuthor();

  @Option(longName = "md_subject", description = "PDF Metadata subject value")
  String getSubject();

  boolean isSubject();

  @Option(longName = "md_keywords", description = "PDF Metadata keywords value")
  String getKeywords();

  boolean isKeywords();

  @Option(longName = "md_creator", description = "PDF Metadata creator value")
  String getCreator();

  boolean isCreator();

  @Option(longName = "allow_print", description = "Allow the user to print the document")
  boolean getAllowPrint();

  @Option(longName = "allow_copy", description = "Allow the user to copy/extract the text and graphics")
  boolean getAllowCopy();

  @Option(longName = "allow_assembly", description = "Allow the user to insert, remove and rotate pages and add bookmarks")
  boolean getAllowAssembly();

  @Option(longName = "allow_annotations", description = "Allow the user to add or modify text annotations")
  boolean getAllowAnnotations();

  @Option(longName = "encryption_password", description = "PDF Encryption password")
  String getPassword();

  boolean isPassword();

  @Option(longName = "encryption_keyfile", description = "PDF Encryption keyfile")
  String getKeyFile();

  boolean isKeyFile();

  @Option(longName = "ranges", description = "Page range selection")
  String getPageRanges();

  boolean isPageRanges();

  @Option(longName = "wm_text", description = "Watermark text - each value will be printed on a separate line")
  List<String> getWatermarkText();

  boolean isWatermarkText();

  @Option(longName = "wm_image", description = "Watermark image")
  File getWatermarkImage();

  boolean isWatermarkImage();

  @Option(longName = "wm_opacity", description = "Opacity - specify as fraction [0.1]", defaultValue = "0.1", pattern = "0+(\\.[0-9]+)?|1(\\.0*)?")
  float getOpacity();

  boolean isOpacity();

  @Option(longName = "wm_gap_ratio", description = "Percentage of total width/height to use as blank padding - specify as fraction [0.5]", defaultValue = "0.5", pattern = "0+(\\.[0-9]+)?|1(\\.0*)?")
  float getGapRatio();

  boolean isGapRatio();

  @Option(longName = "wm_gap_size", description = "Amount of blank padding to add (points = 1/72 inch) [0]", defaultValue = "0")
  float getGapSize();

  boolean isGapSize();

  @Option(longName = "wm_font_size", description = "Font size for the watermark text [20]", defaultValue = "20")
  float getFontSize();

  boolean isFontSize();

  @Option(longName = "wm_text_rotation", description = "Rotation of the watermark text (in degrees) [15]", defaultValue = "15")
  float getTextRotation();

  boolean isTextRotation();
}
