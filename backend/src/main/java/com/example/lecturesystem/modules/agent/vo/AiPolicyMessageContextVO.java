package com.example.lecturesystem.modules.agent.vo;

import java.time.LocalDateTime;

public record AiPolicyMessageContextVO(Long id,
                                       String role,
                                       String content,
                                       String citedChunkIds,
                                       LocalDateTime createTime) {
}
