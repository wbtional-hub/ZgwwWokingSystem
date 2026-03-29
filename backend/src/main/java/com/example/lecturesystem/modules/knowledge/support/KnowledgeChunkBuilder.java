package com.example.lecturesystem.modules.knowledge.support;

import com.example.lecturesystem.modules.knowledge.entity.KnowledgeChunkEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunkBuilder {
    private static final int CHUNK_SIZE = 800;

    public List<KnowledgeChunkEntity> build(Long documentId,
                                            Long baseId,
                                            Long categoryId,
                                            String keywordText,
                                            List<ParsedDocSection> sections) {
        List<KnowledgeChunkEntity> result = new ArrayList<>();
        int chunkNo = 1;
        int sortNo = 1;
        for (ParsedDocSection section : sections) {
            String content = section.getContentText();
            if (content == null || content.isBlank()) {
                continue;
            }
            int start = 0;
            while (start < content.length()) {
                int end = Math.min(start + CHUNK_SIZE, content.length());
                String piece = content.substring(start, end);
                KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
                entity.setDocumentId(documentId);
                entity.setBaseId(baseId);
                entity.setCategoryId(categoryId);
                entity.setChunkNo(chunkNo++);
                entity.setChunkType(section.getChunkType());
                entity.setHeadingPath(section.getHeadingPath());
                entity.setContentText(piece);
                entity.setKeywordText(keywordText);
                entity.setContentLength(piece.length());
                entity.setSortNo(sortNo++);
                entity.setCreateTime(LocalDateTime.now());
                result.add(entity);
                start = end;
            }
        }
        return result;
    }
}
