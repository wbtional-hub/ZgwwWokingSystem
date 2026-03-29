package com.example.lecturesystem.modules.agent.support;

import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KnowledgeCitationContext {
    private final List<KnowledgeSearchResultVO> chunks = new ArrayList<>();

    public List<KnowledgeSearchResultVO> getChunks() {
        return chunks;
    }

    public String toPromptContext() {
        if (chunks.isEmpty()) {
            return "未检索到相关知识，请按谨慎原则回答，并明确提示依据不足。";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeSearchResultVO chunk = chunks.get(index);
            builder.append("[资料").append(index + 1).append("] ")
                    .append(chunk.getDocTitle() == null ? "未命名文档" : chunk.getDocTitle())
                    .append(" | ")
                    .append(chunk.getHeadingPath() == null ? "未标记标题路径" : chunk.getHeadingPath())
                    .append(" | Chunk#")
                    .append(chunk.getChunkNo() == null ? 0 : chunk.getChunkNo())
                    .append("\n")
                    .append(chunk.getSnippet() == null ? "" : chunk.getSnippet())
                    .append("\n\n");
        }
        return builder.toString();
    }

    public String toChunkIds() {
        return chunks.stream().map(item -> String.valueOf(item.getChunkId())).collect(Collectors.joining(","));
    }
}