package dev.sebi.knowledgeopshub.documents;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractService {

  public record PageText(int pageNumber, String text) {}

  public List<PageText> extractPerPage(Path pdfPath) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
      int pages = doc.getNumberOfPages();
      PDFTextStripper stripper = new PDFTextStripper();
      List<PageText> out = new ArrayList<>(pages);

      for (int i = 1; i <= pages; i++) {
        stripper.setStartPage(i);
        stripper.setEndPage(i);
        String text = stripper.getText(doc);
        text = normalize(text);
        out.add(new PageText(i, text));
      }
      return out;
    }
  }

  private String normalize(String s) {
    if (s == null) return "";
    // limpia espacios absurdos sin “romper” el contenido
    return s.replace("\u0000", "")
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
  }
}
