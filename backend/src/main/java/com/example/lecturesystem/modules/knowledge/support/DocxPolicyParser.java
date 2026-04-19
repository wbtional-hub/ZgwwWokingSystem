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
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class DocxPolicyParser {
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^(第[一二三四五六七八九十百]+[章节部分篇]|[一二三四五六七八九十百]+、|\\d+\\.\\s*|\\d+、).*");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^(\\(?[一二三四五六七八九十]+\\)|（[一二三四五六七八九十]+）|\\d+\\.\\d+|[①②③④⑤⑥⑦⑧⑨⑩]).*");

    static {
        // Internal policy compilations can legitimately contain many embedded docx entries.
        ZipSecureFile.setMaxFileCount(20000L);
    }

    public ParsedDocResult parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            ParsedDocResult result = new ParsedDocResult();
            List<ParsedDocSection> sections = new ArrayList<>();
            String currentChapter = "正文";
            String currentSection = null;
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
                    int headingLevel = resolveHeadingLevel(paragraph, text);
                    if (headingLevel == 1) {
                        currentChapter = text;
                        currentSection = null;
                        continue;
                    }
                    if (headingLevel == 2) {
                        currentSection = text;
                        continue;
                    }
                    ParsedDocSection section = new ParsedDocSection();
                    section.setSectionNo(sectionNo++);
                    section.setChapterTitle(currentChapter);
                    section.setSectionTitle(currentSection);
                    section.setHeadingPath(buildHeadingPath(currentChapter, currentSection));
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
                    section.setChapterTitle(currentChapter);
                    section.setSectionTitle(currentSection);
                    section.setHeadingPath(buildHeadingPath(currentChapter, currentSection));
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

    private int resolveHeadingLevel(XWPFParagraph paragraph, String text) {
        String style = paragraph.getStyle();
        String lowerStyle = style == null ? "" : style.toLowerCase(Locale.ROOT);
        if (lowerStyle.contains("heading 1") || lowerStyle.contains("heading1") || lowerStyle.contains("title")) {
            return 1;
        }
        if (lowerStyle.contains("heading 2") || lowerStyle.contains("heading2")) {
            return 2;
        }
        if (text.length() > 40) {
            return 0;
        }
        if (CHAPTER_PATTERN.matcher(text).matches()) {
            return 1;
        }
        if (SECTION_PATTERN.matcher(text).matches()) {
            return 2;
        }
        if (lowerStyle.contains("heading")) {
            return 2;
        }
        return 0;
    }

    private String buildHeadingPath(String chapterTitle, String sectionTitle) {
        String chapter = normalize(chapterTitle);
        String section = normalize(sectionTitle);
        if (chapter == null && section == null) {
            return "正文";
        }
        if (section == null) {
            return chapter;
        }
        if (chapter == null) {
            return section;
        }
        return chapter + " / " + section;
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
