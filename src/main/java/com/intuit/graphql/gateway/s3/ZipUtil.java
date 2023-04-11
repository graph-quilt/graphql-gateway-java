package com.intuit.graphql.gateway.s3;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

  private ZipUtil() {
  }

  public static ZipInputStream createZipInputStream(byte[] zipInByteArray) {
    ByteArrayInputStream zippedFileInputStream = new ByteArrayInputStream(
        zipInByteArray);

    return new ZipInputStream(zippedFileInputStream);
  }

  public static List<FileEntry> uncompress(byte[] zipInByteArray) {
    byte[] buffer = new byte[1024];
    List<FileEntry> items = new ArrayList<>();

    try (ZipInputStream zis = createZipInputStream(zipInByteArray)) { // auto-closable
      ZipEntry ze;
      while ((ze = zis.getNextEntry()) != null) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int len;
        while ((len = zis.read(buffer)) > 0) {
          outputStream.write(buffer, 0, len);
        }
        items.add(ImmutableFileEntry.of(ze.getName(), outputStream.toByteArray()));
        zis.closeEntry();
      }

      if (items.isEmpty()) {
        throw new IllegalArgumentException("No zip entries found.");
      }

      return items;
    } catch (IOException e) {
      throw new RuntimeException("Error uncompressing zip file.", e);
    }
  }

}
