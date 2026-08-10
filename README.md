# LLM Judge Pattern

Spring AI RAG demo that answers questions about a company recharge-leave policy and evaluates its own answers with a separate LLM judge (relevancy + groundedness against the retrieved context).

## How it works

- `POST /ai/ask` (`QuestionController`) takes a question, retrieves relevant chunks of `company-recharge.txt` from a [pgvector](https://github.com/pgvector/pgvector) store, and generates an answer with `gpt-4o-mini`.
- `EmbeddingsLoader` splits and embeds the policy document into pgvector on startup.
- [`JudgeService`](src/main/java/com/example/llmjudge/config/JudgeService.java) evaluates every answer with a stronger model (`gpt-4o`):
  - **Relevancy** – does the answer address the question, given the retrieved context?
  - **Groundedness** – the answer is decomposed into atomic claims, each classified as `fact` or `opinion`; only `fact` claims are checked against the retrieved context (via Spring AI's `FactCheckingEvaluator`), so recommendations/opinions can't be flagged as unsupported.

## Why LLM-as-judge? (EU AI Act regulatory context)

If a system is classified as high-risk under Regulation (EU) 2024/1689 (AI Act), it must, among other things, satisfy:

- Art. 9 – Risk management system – continuous identification and mitigation of risks throughout the lifecycle.
- Art. 10 – Data governance – quality, relevance, and representativeness of training/validation/testing data.
- Art. 15 – Accuracy, robustness, cybersecurity – the key article for validation: the system must achieve a declared level of accuracy (stated in the instructions for use), must be resilient to errors and drift, and, if it keeps learning, must eliminate the risk of feedback loops biasing outputs.
- Art. 17 – Quality management system – includes testing and validation procedures as part of the quality management system.
- Art. 14 – Human oversight – validation must not be fully autonomous without the possibility of human intervention.
- Art. 43 – Conformity assessment – a conformity assessment must be performed before placing the system on the market.

**LLM-as-judge is one technique** for meeting the validation/accuracy requirement of Art. 15 — the [`JudgeService`](src/main/java/com/example/llmjudge/config/JudgeService.java) in this project is a concrete implementation (relevancy + groundedness check with a stronger model), not a mandated method.

## Configuration

See `src/main/resources/application.properties`:

- `OPENAI_API_KEY` – required env var
- `spring.datasource.*` – local PostgreSQL + pgvector instance

## Running

```
./mvnw spring-boot:run
```
