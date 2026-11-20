package com.carbon.manage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.carbon.manage.entity.AiDiagLog;
import com.carbon.manage.mapper.AiDiagLogMapper;

/**
 * 异步 AI 诊断：调用本地 Ollama（Spring AI ChatClient），失败时走规则兜底并照常生成工单。
 * <p>独立 Bean 是为了让 @Async 代理生效（同类自调用不会走代理）。</p>
 */
@Slf4j
@Service
public class AiDiagnosisAsyncService {

    private final ChatClient chatClient;
    private final AiDiagLogMapper diagLogMapper;
    private final PromptBuilder promptBuilder;
    private final AiDiagPersistService persistService;

    public AiDiagnosisAsyncService(ChatClient.Builder chatClientBuilder,
                                   AiDiagLogMapper diagLogMapper,
                                   PromptBuilder promptBuilder,
                                   AiDiagPersistService persistService) {
        this.chatClient = chatClientBuilder.build();
        this.diagLogMapper = diagLogMapper;
        this.promptBuilder = promptBuilder;
        this.persistService = persistService;
    }

    @Async("aiExecutor")
    public void diagnose(Long logId) {
        AiDiagLog diagLog = diagLogMapper.selectById(logId);
        if (diagLog == null) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            String content = chatClient.prompt()
                    .system(PromptBuilder.SYSTEM_PROMPT)
                    .user(diagLog.getPromptSnapshot())
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("大模型返回内容为空");
            }
            long duration = System.currentTimeMillis() - start;
            persistService.saveSuccess(logId, content, content, diagLog, duration);
            log.info("AI 诊断完成 logId={}, 模型={}, 耗时={}ms", logId, diagLog.getModelName(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("AI 诊断失败 logId={}，启用规则兜底：{}", logId, e.getMessage());
            String fallback = promptBuilder.fallbackSuggestion(diagLog.getEnergyType());
            try {
                persistService.saveFallback(logId, fallback, truncate(e.getMessage()), diagLog, duration);
            } catch (Exception persistEx) {
                log.error("诊断兜底落库失败 logId={}", logId, persistEx);
                try {
                    persistService.markFailed(logId, truncate(persistEx.getMessage()), duration);
                } catch (Exception ignored) {
                    // 最终失败记录都写不进时，只能依赖日志排查
                }
            }
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 400 ? message.substring(0, 400) : message;
    }
}
