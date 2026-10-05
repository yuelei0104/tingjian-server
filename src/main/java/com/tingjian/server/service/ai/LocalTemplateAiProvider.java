package com.tingjian.server.service.ai;

import com.tingjian.server.dto.AiContextMessageRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocalTemplateAiProvider implements AiExpressionProvider {
    @Override
    public AiProviderResult suggest(AiProviderInput input) {
        return local(input, "LOCAL_TEMPLATE");
    }

    public static AiProviderResult fallback(AiProviderInput input) {
        return local(input, "LOCAL_FALLBACK");
    }

    private static AiProviderResult local(AiProviderInput input, String provider) {
        String source = input.sourceText() == null ? "" : input.sourceText().strip();
        String suggestion = switch (input.action()) {
            case REPLY -> reply(input);
            case POLITE -> polite(source);
            case CONCISE -> concise(source);
            case FORMAL -> formal(source);
            case TRANSLATE_ZH -> translateZh(source);
            case TRANSLATE_EN -> translateEn(source);
        };
        return new AiProviderResult(suggestion, provider, true);
    }

    private static String reply(AiProviderInput input) {
        String latest = input.context().stream()
                .filter(message -> "OTHER".equals(message.speaker()))
                .reduce((first, second) -> second)
                .map(AiContextMessageRequest::content)
                .orElse("");
        if (latest.contains("?") || latest.contains("？")) {
            return "好的，我确认后尽快回复您。";
        }
        return latest.isBlank() ? "好的，我明白了。" : "好的，我已了解，谢谢。";
    }

    private static String polite(String source) {
        if (source.isBlank()) return "麻烦您再说明一下，谢谢。";
        if (source.endsWith("谢谢。") || source.endsWith("谢谢！")) return source;
        return source.replace("你", "您") + " 谢谢。";
    }

    private static String concise(String source) {
        if (source.isBlank()) return "好的。";
        String[] parts = source.split("[。！？!?；;\\n]", 2);
        return parts[0].strip() + (containsChinese(parts[0]) ? "。" : ".");
    }

    private static String formal(String source) {
        if (source.isBlank()) return "我已了解相关内容。";
        return "关于此事，我的回复是：" + source;
    }

    private static String translateZh(String source) {
        return switch (source.toLowerCase(Locale.ROOT)) {
            case "hello" -> "你好";
            case "thank you", "thanks" -> "谢谢";
            case "please wait" -> "请稍等";
            default -> source.isBlank() ? "请输入需要翻译的内容" : source;
        };
    }

    private static String translateEn(String source) {
        return switch (source) {
            case "你好" -> "Hello";
            case "谢谢" -> "Thank you";
            case "请稍等" -> "Please wait";
            default -> source.isBlank() ? "Please enter text to translate" : source;
        };
    }

    private static boolean containsChinese(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }
}
