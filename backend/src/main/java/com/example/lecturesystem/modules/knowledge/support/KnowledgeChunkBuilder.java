package com.example.lecturesystem.modules.knowledge.support;

import com.example.lecturesystem.modules.knowledge.entity.KnowledgeChunkEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunkBuilder {
    private static final int GENERIC_CHUNK_SIZE = 800;
    private static final int GENERIC_OVERLAP = 100;
    private static final int MAIN_MIN = 600;
    private static final int MAIN_MAX = 1200;
    private static final int MAIN_OVERLAP = 120;
    private static final int TOPIC_MIN = 400;
    private static final int TOPIC_MAX = 900;
    private static final int TOPIC_PROCESS_MAX = 1200;
    private static final int TOPIC_OVERLAP = 100;

    public List<KnowledgeChunkEntity> build(Long documentId,
                                            Long baseId,
                                            Long categoryId,
                                            String keywordText,
                                            ParsedDocResult parsedDocResult) {
        List<ParsedDocSection> sections = parsedDocResult == null ? List.of() : parsedDocResult.getSections();
        List<KnowledgeChunkEntity> result = new ArrayList<>();
        if (parsedDocResult != null && Boolean.FALSE.equals(parsedDocResult.getSearchable())) {
            return result;
        }
        if (sections == null || sections.isEmpty()) {
            return result;
        }
        String structuredDocType = parsedDocResult == null ? null : parsedDocResult.getStructuredDocType();
        if (structuredDocType == null || "GENERIC".equalsIgnoreCase(structuredDocType)) {
            return buildGenericChunks(documentId, baseId, categoryId, keywordText, sections);
        }
        int[] config = resolveChunkConfig(structuredDocType, sections);
        int chunkNo = 1;
        int sortNo = 1;
        List<ChunkBlock> blocks = buildBlocks(sections);
        for (ChunkBlock block : blocks) {
            List<String> pieces = splitContent(block.contentText(), config[0], config[1], config[2]);
            for (String piece : pieces) {
                KnowledgeChunkEntity entity = createChunkEntity(documentId, baseId, categoryId, keywordText, chunkNo++, sortNo++, block.prototype(), piece);
                result.add(entity);
            }
        }
        return result;
    }

    private List<KnowledgeChunkEntity> buildGenericChunks(Long documentId,
                                                          Long baseId,
                                                          Long categoryId,
                                                          String keywordText,
                                                          List<ParsedDocSection> sections) {
        List<KnowledgeChunkEntity> result = new ArrayList<>();
        int chunkNo = 1;
        int sortNo = 1;
        for (ParsedDocSection section : sections) {
            String content = normalize(section.getContentText());
            if (content == null) {
                continue;
            }
            for (String piece : splitContent(content, GENERIC_CHUNK_SIZE, GENERIC_CHUNK_SIZE, GENERIC_OVERLAP)) {
                result.add(createChunkEntity(documentId, baseId, categoryId, keywordText, chunkNo++, sortNo++, section, piece));
            }
        }
        return result;
    }

    private int[] resolveChunkConfig(String structuredDocType, List<ParsedDocSection> sections) {
        if ("MAIN_DOC_XM".equalsIgnoreCase(structuredDocType) || "MAIN_DOC_FJ".equalsIgnoreCase(structuredDocType)) {
            return new int[]{MAIN_MIN, MAIN_MAX, MAIN_OVERLAP};
        }
        boolean hasProcessSection = sections.stream().anyMatch(section -> "process".equalsIgnoreCase(section.getTopicType()));
        return new int[]{TOPIC_MIN, hasProcessSection ? TOPIC_PROCESS_MAX : TOPIC_MAX, TOPIC_OVERLAP};
    }

    private List<ChunkBlock> buildBlocks(List<ParsedDocSection> sections) {
        List<ChunkBlock> blocks = new ArrayList<>();
        ParsedDocSection currentPrototype = null;
        StringBuilder buffer = new StringBuilder();
        for (ParsedDocSection section : sections) {
            String content = normalize(section.getContentText());
            if (content == null || Boolean.FALSE.equals(section.getSearchable())) {
                continue;
            }
            if (currentPrototype == null) {
                currentPrototype = section;
                buffer.append(content);
                continue;
            }
            if (!canMerge(currentPrototype, section)) {
                blocks.add(new ChunkBlock(currentPrototype, buffer.toString()));
                currentPrototype = section;
                buffer = new StringBuilder(content);
                continue;
            }
            buffer.append("\n\n").append(content);
        }
        if (currentPrototype != null && buffer.length() > 0) {
            blocks.add(new ChunkBlock(currentPrototype, buffer.toString()));
        }
        return blocks;
    }

    private boolean canMerge(ParsedDocSection current, ParsedDocSection next) {
        return equalsNullable(current.getDocType(), next.getDocType())
                && equalsNullable(current.getRegionScope(), next.getRegionScope())
                && equalsNullable(current.getPolicyName(), next.getPolicyName())
                && equalsNullable(current.getChapterTitle(), next.getChapterTitle())
                && equalsNullable(current.getSectionTitle(), next.getSectionTitle())
                && equalsNullable(current.getTopicType(), next.getTopicType());
    }

    private List<String> splitContent(String content, int minSize, int maxSize, int overlap) {
        List<String> result = new ArrayList<>();
        String normalized = normalize(content);
        if (normalized == null) {
            return result;
        }
        if (normalized.length() <= maxSize) {
            result.add(normalized);
            return result;
        }
        String[] paragraphs = normalized.split("\\n+");
        StringBuilder buffer = new StringBuilder();
        String previousPiece = null;
        for (String paragraph : paragraphs) {
            String normalizedParagraph = normalize(paragraph);
            if (normalizedParagraph == null) {
                continue;
            }
            int nextLength = buffer.length() == 0 ? normalizedParagraph.length() : buffer.length() + 2 + normalizedParagraph.length();
            if (buffer.length() >= minSize && nextLength > maxSize) {
                previousPiece = buffer.toString();
                result.add(previousPiece);
                buffer = new StringBuilder(buildOverlap(previousPiece, overlap));
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(normalizedParagraph);
        }
        if (buffer.length() > 0) {
            result.add(buffer.toString());
        }
        return result;
    }

    private String buildOverlap(String piece, int overlap) {
        String normalized = normalize(piece);
        if (normalized == null || normalized.length() <= overlap) {
            return normalized == null ? "" : normalized;
        }
        return normalized.substring(normalized.length() - overlap);
    }

    private KnowledgeChunkEntity createChunkEntity(Long documentId,
                                                   Long baseId,
                                                   Long categoryId,
                                                   String keywordText,
                                                   int chunkNo,
                                                   int sortNo,
                                                   ParsedDocSection section,
                                                   String piece) {
        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setDocumentId(documentId);
        entity.setBaseId(baseId);
        entity.setCategoryId(categoryId);
        entity.setChunkNo(chunkNo);
        entity.setChunkType(section.getChunkType());
        entity.setHeadingPath(section.getHeadingPath());
        entity.setContentText(piece);
        entity.setKeywordText(keywordText);
        entity.setRegionScope(section.getRegionScope());
        entity.setDocType(section.getDocType());
        entity.setTopicType(section.getTopicType());
        entity.setPolicyName(section.getPolicyName());
        entity.setPolicyAliases(section.getPolicyAliases());
        entity.setPolicyNo(section.getPolicyNo());
        entity.setChapterTitle(section.getChapterTitle());
        entity.setSectionTitle(section.getSectionTitle());
        entity.setScenePriority(section.getScenePriority());
        entity.setSearchable(section.getSearchable() == null ? Boolean.TRUE : section.getSearchable());
        entity.setContentLength(piece.length());
        entity.setSortNo(sortNo);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private boolean equalsNullable(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft == null) {
            return normalizedRight == null;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    private record ChunkBlock(ParsedDocSection prototype, String contentText) {
    }
}
