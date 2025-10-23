package com.example.demo.service;

import com.example.demo.model.AskResponse;
import com.example.demo.service.IntentDetector.Hint;
import com.example.demo.service.IntentDetector.Intent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Value("${rag.top.k:4}")
    private int topK;

    @Value("${rag.threshold:0.35}")
    private double minScore;

    private final VectorStore store;
    private final GeminiRestService gemini;

    public RagService(VectorStore store, GeminiRestService gemini) {
        this.store = store;
        this.gemini = gemini;
    }

    public AskResponse ask(String question) throws Exception {
        if (store.size() == 0) {
            return new AskResponse(
                    "Chưa upload dữ liệu PDF.",
                    List.of());
        }

        Hint hint = IntentDetector.detect(question);

        float[] q = gemini.embed(question);
        int oversample = Math.max(topK * 3, topK + 2);
        List<VectorStore.Scored> firstPass = store.topK(q, oversample);

        if (firstPass.isEmpty() || firstPass.get(0).score() < minScore) {
            return new AskResponse("Tôi không biết.", List.of());
        }

        List<VectorStore.Scored> rescored = new ArrayList<>();
        String sec = hint.sectionHint == null ? null : hint.sectionHint.toLowerCase(Locale.ROOT);

        for (var s : firstPass) {
            double boost = 0.0;
            String textLower = s.text().toLowerCase(Locale.ROOT);

            if (sec != null) {
                if (textLower.contains(sec))
                    boost += 0.08;
                if (sec.contains("mở đầu") &&
                        (textLower.contains("mở đầu") || textLower.contains("introduction"))) {
                    boost += 0.06;
                }
            }
            if (hint.intent == Intent.DEFINE && hint.term != null &&
                    textLower.contains(hint.term.toLowerCase(Locale.ROOT))) {
                boost += 0.12;
            }
            rescored.add(new VectorStore.Scored(s.id(), s.text(), s.score() + boost));
        }
        rescored.sort(Comparator.comparingDouble(VectorStore.Scored::score).reversed());

        if (rescored.isEmpty() || rescored.get(0).score() < minScore) {
            return new AskResponse("Tôi không biết.", List.of());
        }

        var hits = rescored.subList(0, Math.min(topK, rescored.size()));

        StringBuilder ctx = new StringBuilder();
        for (VectorStore.Scored s : hits) {
            ctx.append("\n[Chunk #").append(s.id())
                    .append(" / score=").append(String.format("%.3f", s.score()))
                    .append("]\n").append(s.text()).append("\n");
        }

        String system = switch (hint.intent) {
            case SUMMARY -> """
                    You answer using ONLY the provided context. Give a concise **overview** in Vietnamese.
                    If not enough info in context, say you don't know.
                    """;
            case BULLET_SUMMARY -> """
                    You answer using ONLY the provided context. Return a **bullet list** in Vietnamese.
                    Each bullet must be one sentence. If not enough info, say you don't know.
                    """;
            case DEFINE -> """
                    You answer using ONLY the provided context. Provide a **precise definition** in Vietnamese.
                    If a direct definition is present, quote briefly (<= 1-2 sentences). Otherwise say you don't know.
                    """;
            case COMPARE -> """
                    You answer using ONLY the provided context. Provide a **comparison** in Vietnamese:
                    - Strengths/When to use of A
                    - Strengths/When to use of B
                    If missing info for either, state so explicitly.
                    """;
            default -> """
                    You answer using ONLY the provided context. Be concise in Vietnamese.
                    If not found in context, say you don't know.
                    """;
        };

        String formatPart = switch (hint.intent) {
            case BULLET_SUMMARY ->
                (hint.bullets != null ? "Return exactly " + hint.bullets + " bullets.\n" : "Return 3-6 bullets.\n");
            case DEFINE -> (hint.term != null ? "Term to define: \"" + hint.term + "\"\n" : "");
            case COMPARE -> "If the question mentions two methods A and B, structure bullets per method.\n";
            default -> "";
        };

        String prompt = system
                + "\n\n=== CONTEXT START ===\n" + ctx + "=== CONTEXT END ===\n\n"
                + formatPart + "Question: " + question + "\nAnswer:";

        try {
            String answer = gemini.chat(prompt);
            return new AskResponse(
                    answer,
                    hits.stream().map(s -> new AskResponse.SourceScore(s.id(), s.score())).toList());
        } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
            return new AskResponse("AI đang quá tải. Bạn hãy đặt lại câu hỏi.", List.of());
        }
    }
}
