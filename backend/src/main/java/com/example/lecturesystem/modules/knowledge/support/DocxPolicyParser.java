package com.example.lecturesystem.modules.knowledge.support;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocxPolicyParser {
    static {
        // Internal policy compilations can legitimately contain many embedded docx entries.
        ZipSecureFile.setMaxFileCount(20000L);
    }

    public ParsedDocResult parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            ParsedDocResult result = new ParsedDocResult();
            List<ParsedDocSection> sections = new ArrayList<>();
            String currentHeading = "\u6b63\u6587";
            int sectionNo = 1;

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = normalize(paragraph.getText());
                    if (text == null) {
                        continue;
                    }
                    if (result.getTitle() == null) {
                        result.setTitle(text);
                    }
                    if (isHeading(paragraph, text)) {
                        currentHeading = text;
                        continue;
                    }
                    ParsedDocSection section = new ParsedDocSection();
                    section.setSectionNo(sectionNo++);
                    section.setHeadingPath(currentHeading);
                    section.setChunkType("PARAGRAPH");
                    section.setContentText(text);
                    sections.add(section);
                    if (result.getSummary() == null && text.length() >= 20) {
                        result.setSummary(text.length() > 120 ? text.substring(0, 120) : text);
                    }
                    continue;
                }
                if (element instanceof XWPFTable table) {
                    String text = normalizeTable(table);
                    if (text == null) {
                        continue;
                    }
                    ParsedDocSection section = new ParsedDocSection();
                    section.setSectionNo(sectionNo++);
                    section.setHeadingPath(currentHeading);
                    section.setChunkType("TABLE");
                    section.setContentText(text);
                    sections.add(section);
                }
            }

            result.setSections(sections);
            if (result.getTitle() == null) {
                result.setTitle("\u672a\u547d\u540d\u6587\u6863");
            }
            if (result.getSummary() == null) {
                result.setSummary(result.getTitle());
            }
            return result;
        }
    }

    private boolean isHeading(XWPFParagraph paragraph, String text) {
        String style = paragraph.getStyle();
        return (style != null && style.toLowerCase().contains("heading"))
                || (text.length() <= 30 && text.matches("^[\\u4e00-\\u9fa5A-Za-z0-9\\(\\)\\uff08\\uff09\\u4e00\\u4e8c\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u5341].*"));
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace('\u3000', ' ').trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeTable(XWPFTable table) {
        List<String> rows = new ArrayList<>();
        table.getRows().forEach(row -> {
            List<String> cells = new ArrayList<>();
            row.getTableCells().forEach(cell -> {
                String text = normalize(cell.getText());
                if (text != null) {
                    cells.add(text);
                }
            });
            if (!cells.isEmpty()) {
                rows.add(String.join(" | ", cells));
            }
        });
        return rows.isEmpty() ? null : String.join("\n", rows);
    }
}