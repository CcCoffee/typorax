package com.example.typorax.util;

import com.example.typorax.llm.BigModelNew;
import java.util.concurrent.CompletableFuture;

public class AIRewriteUtil {

    public static CompletableFuture<String> rewriteText(String inputText) {
        CompletableFuture<String> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                String prompt = "请对以下文本进行语法修正、润色和格式化，仅返回最终结果，不要有任何解释：\n\n" + inputText;
                String result = BigModelNew.getAIResponse(prompt);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }
}