package com.example.llmjudge.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class JudgeService {

    private static final String DECOMPOSE_PROMPT = """
            Break the following text into a list of atomic, independently verifiable factual claims.
            If the text already contains a single claim, return a single-element list.

            Text:
            {text}

            Respond with a JSON array of strings, one per claim.
            """;

    private final ChatClient judgeChatClient;

    public EvaluationResult evaluate(String question, String answer, List<Document> context) {

        // 1. Relevancy – is the answer relevant to the question + context? (requires a question)
        RelevancyEvaluator relevancyEvaluator = new RelevancyEvaluator(judgeChatClient.mutate());
        EvaluationRequest relevancyRequest = new EvaluationRequest(question, context, answer);
        EvaluationResponse relevancy = relevancyEvaluator.evaluate(relevancyRequest);

        // 2. Groundedness – is every claim in the answer supported by the context? (question irrelevant here)
        GroundednessResult groundedness = checkGroundedness(answer, context);

        return new EvaluationResult(
                relevancy.isPass(),
                groundedness.grounded(),
                groundedness.score(),
                relevancy.getFeedback(),
                groundedness.feedback(),
                groundedness.claims()
        );
    }

    // claim doesn't have to be an answer to a question – also works for a standalone claim
    public GroundednessResult checkGroundedness(String claim, List<Document> context) {
        List<String> atomicClaims = decompose(claim);
        FactCheckingEvaluator groundednessEvaluator = FactCheckingEvaluator.builder(judgeChatClient.mutate()).build();

        List<ClaimVerdict> verdicts = atomicClaims.stream()
                .map(c -> {
                    EvaluationResponse response = groundednessEvaluator.evaluate(new EvaluationRequest(context, c));
                    return new ClaimVerdict(c, response.isPass(), response.getFeedback());
                })
                .toList();

        boolean grounded = verdicts.stream().allMatch(ClaimVerdict::grounded);
        double score = verdicts.stream().filter(ClaimVerdict::grounded).count() / (double) verdicts.size();
        String feedback = verdicts.stream()
                .filter(v -> !v.grounded())
                .map(v -> "\"%s\" – %s".formatted(v.claim(), v.feedback()))
                .collect(Collectors.joining("; "));

        return new GroundednessResult(grounded, score, verdicts,
                feedback.isEmpty() ? "All claims are supported by the context." : feedback);
    }

    private List<String> decompose(String text) {
        return judgeChatClient.mutate()
                .build()
                .prompt()
                .user(u -> u.text(DECOMPOSE_PROMPT).param("text", text))
                .call()
                .entity(new ParameterizedTypeReference<List<String>>() {});
    }

    public record ClaimVerdict(String claim, boolean grounded, String feedback) {}

    public record GroundednessResult(
            boolean grounded,
            double score,
            List<ClaimVerdict> claims,
            String feedback
    ) {}

    public record EvaluationResult(
            boolean relevant,
            boolean grounded,
            double groundednessScore,
            String relevancyFeedback,
            String groundednessFeedback,
            List<ClaimVerdict> claimVerdicts
    ) {}
}