# LLM Judge

Spring AI RAG demo that answers questions about a company recharge-leave policy and evaluates its own answers with a separate LLM judge (relevancy + groundedness against the retrieved context).

## How it works

- `POST /ai/ask` (`QuestionController`) takes a question, retrieves relevant chunks of `company-recharge.txt` from a pgvector store, and generates an answer with `gpt-4o-mini`.
- `EmbeddingsLoader` splits and embeds the policy document into pgvector on startup.
- `JudgeService` evaluates every answer with a stronger model (`gpt-4o`):
  - **Relevancy** – does the answer address the question, given the retrieved context?
  - **Groundedness** – the answer is decomposed into atomic claims, each classified as `fact` or `opinion`; only `fact` claims are checked against the retrieved context (via Spring AI's `FactCheckingEvaluator`), so recommendations/opinions can't be flagged as unsupported.

## Configuration

See `src/main/resources/application.properties`:

- `OPENAI_API_KEY` – required env var
- `spring.datasource.*` – local PostgreSQL + pgvector instance

## Running

```
./mvnw spring-boot:run
```
