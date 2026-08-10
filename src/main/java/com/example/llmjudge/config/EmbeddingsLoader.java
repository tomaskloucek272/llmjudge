package com.example.llmjudge.config;

import com.knuddels.jtokkit.api.EncodingType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingsLoader {
    private final VectorStore vectorStore;

    @Value("${app.resource}")
    private Resource pdfResource;

    @PostConstruct
    void loadIfEmpty() {
        TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
        List<Document> documents = documentReader.get();
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .withPunctuationMarks(List.of('.', '!', '?', ';', '\n', '…'))
                .withEncodingType(EncodingType.CL100K_BASE)
                .build();
        List<Document> splitDocuments = splitter.apply(documents);
        vectorStore.add(splitDocuments);
    }
}
