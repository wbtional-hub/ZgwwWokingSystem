package com.example.lecturesystem.modules.knowledge.support;

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
    public ParsedDocResult parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            ParsedDocResult result = new ParsedDocResult();
            List<ParsedDocSection> sections = new ArrayList<>();
            String currentHeading = "正文";
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
                result.setTitle("未命名文档");
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
                || (text.length() <= 30 && text.matches("^[一二三四五六七八九十0-9A-Za-z（(].*"));
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
