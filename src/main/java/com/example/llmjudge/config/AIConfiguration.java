package com.example.llmjudge.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfiguration {
    private static final String QA_TEMPLATE = """
            <query>

            Context information is below.

            ---------------------
            <question_answer_context>
            ---------------------

            Given the context information and no prior knowledge, answer the query.

            Follow these rules:

            1. If the answer is not in the context, reply with something like: "I can only answer based on the information I have available in my knowledge base." Do not invent or use any external knowledge.
            2. Avoid statements like "Based on the context..." or "The provided information...".
            """;

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore pgVectorStore) {
        var promptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .template(QA_TEMPLATE)
                .build();

        var ragAdvisor = QuestionAnswerAdvisor.builder(pgVectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.3)
                        .topK(5)
                        .build())
                .promptTemplate(promptTemplate)
                .build();

        return chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ragAdvisor)
                .build();
    }

    @Bean
    @Qualifier("judgeChatClient")
    public ChatClient judgeChatClient() {
        OpenAiChatModel gpt5Model = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(System.getenv("OPENAI_API_KEY"))
                        .model("gpt-4o")
                        .temperature(0.0)
                        .topP(1.0)
                        .build())
                .build();

        return ChatClient.builder(gpt5Model).build();
    }
}
