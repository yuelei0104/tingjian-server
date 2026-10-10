package com.tingjian.aispeech;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/ai/expression")
public class ExpressionController {
    private final AgentModelProvider provider;

    public ExpressionController(AgentModelProvider provider) {
        this.provider = provider;
    }

    @PostMapping
    public ExpressionResponse suggest(@Valid @RequestBody ExpressionRequest request) {
        AgentModelInput input = input(request);
        try {
            AgentModelResult result = provider.complete(input);
            if (result != null && !result.fallback()
                    && result.text() != null && !result.text().isBlank()) {
                return new ExpressionResponse(
                        AgentText.sanitize(result.text(), 1000), result.provider(), result.fallback());
            }
        } catch (RuntimeException ignored) {
            // A deterministic local response keeps the accessibility flow usable.
        }
        return fallback(request);
    }

    private static AgentModelInput input(ExpressionRequest request) {
        String instruction = switch (request.action()) {
            case "REPLY" -> "根据对话上下文生成一条自然、简短、礼貌的回复。";
            case "POLITE" -> "将文字改写得更礼貌，含义保持不变。";
            case "CONCISE" -> "将文字压缩得更简洁，保留关键信息。";
            case "FORMAL" -> "将文字改写为正式表达，含义保持不变。";
            case "TRANSLATE_ZH" -> "将文字翻译为自然中文。";
            case "TRANSLATE_EN" -> "将文字翻译为自然英文。";
            default -> throw new IllegalArgumentException("unsupported expression action");
        };
        return new AgentModelInput(
                "你是听见App的无障碍沟通助手。仅输出可直接展示或发送的文本，不解释过程，"
                        + "不使用Markdown，不编造事实，不输出隐私或密钥。输出语言："
                        + request.language() + "。",
                instruction + (request.sourceText().isBlank()
                        ? "" : "\n待处理文字：" + AgentText.sanitize(request.sourceText(), 500)),
                AgentText.context(request.context()), "", 0.25, 500);
    }

    private static ExpressionResponse fallback(ExpressionRequest request) {
        String source = AgentText.sanitize(request.sourceText(), 500);
        String result = switch (request.action()) {
            case "REPLY" -> request.context().stream()
                    .filter(message -> "OTHER".equals(message.speaker()))
                    .reduce((first, second) -> second)
                    .map(message -> message.content().contains("?") || message.content().contains("？")
                            ? "好的，我确认后尽快回复您。" : "好的，我已了解，谢谢。")
                    .orElse("好的，我明白了。");
            case "POLITE" -> source.isBlank() ? "麻烦您再说明一下，谢谢。"
                    : source.replace("你", "您") + " 谢谢。";
            case "CONCISE" -> source.isBlank() ? "好的。"
                    : source.split("[。！？!?；;\\n]", 2)[0].strip() + "。";
            case "FORMAL" -> source.isBlank() ? "我已了解相关内容。" : "关于此事，我的回复是：" + source;
            case "TRANSLATE_ZH", "TRANSLATE_EN" -> source;
            default -> source;
        };
        return new ExpressionResponse(result, "LOCAL_EXPRESSION_FALLBACK", true);
    }

    public record ExpressionRequest(
            @NotBlank @Pattern(regexp = "REPLY|POLITE|CONCISE|FORMAL|TRANSLATE_ZH|TRANSLATE_EN")
            String action,
            @NotBlank @Pattern(regexp = "中英混合|中文|English") String language,
            @Size(max = 500) String sourceText,
            @NotNull @Size(max = 20) List<@Valid AgentContextMessage> context) {

        public ExpressionRequest {
            sourceText = sourceText == null ? "" : sourceText;
            context = context == null ? List.of() : List.copyOf(context);
        }
    }

    public record ExpressionResponse(String suggestion, String provider, boolean fallback) {
    }
}
