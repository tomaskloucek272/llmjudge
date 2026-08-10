package com.example.llmjudge.mvc;

import com.example.llmjudge.config.JudgeService;
import com.example.llmjudge.record.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class QuestionController {

    private final ChatClient chatClient;
    private final JudgeService judgeService;

    @PostMapping("/ai/ask")
    public String ask(@RequestBody Question question){
        var chatResponse = chatClient.prompt()
                .user(question.getQuestion())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, question.getConversationId()))
                .call()
                .chatResponse();

        String answer = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .orElse("Empty answer");


        List<Document> context = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getMetadata)
                .map(metadata -> metadata.get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS))
                .map(docs -> (List<Document>) docs)
                .orElse(new ArrayList<>());

        System.out.println(judgeService.evaluate(question.getQuestion(), answer, context));

        return answer;
    }
}
