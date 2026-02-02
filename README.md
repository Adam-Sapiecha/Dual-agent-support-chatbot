
## README


# Dual-Agent Support Chatbot (Java)

This project is a simple conversational support system with **two AI agents**
working inside one multi-turn chat.

- **Agent A (Technical Specialist)** answers technical questions using local docs
- **Agent B (Billing Specialist)** handles pricing and refund-related requests
- A router decides which agent should respond

---

## Features

### Technical Agent
- Uses local documentation files from `/docs`
- Answers must be grounded in docs
- If docs don’t cover the question, the agent asks for clarification

### Billing Agent
Supports basic billing capabilities:
- Plan pricing lookup
- Refund timeline explanation
- Opening a refund support case (email + reason required)

---

## Requirements

- Java 17+
- Maven Wrapper
- OpenAI API key

---

## Environment Variables

Set before running:

```powershell
$env:OPENAI_API_KEY="sk-xxxxx"
$env:OPENAI_MODEL="gpt-4o-mini"
````

---

## Run Locally

Build:

```powershell
.\mvnw.cmd clean package
```

Run:

```powershell
java -cp "target\classes;target\dependency\*" com.example.app.Main
```

---

## Run with Docker

Build:

```bash
docker build -t dual-agent-support .
```

Run:

```bash
docker run -it ^
  -e OPENAI_API_KEY="sk-xxxxx" ^
  -e OPENAI_MODEL="gpt-4o-mini" ^
  dual-agent-support
```

---

## Example Test Questions

### Technical

* `I get HTTP 401 when calling the API. What should I check?`
* `What is the webhook timeout and retry behavior?`
* `My webhook fails with INVALID_SIGNATURE. How do I fix it?`

### Billing

* `How much is the Pro plan?`
* `I want a refund`

### Outside Scope

* `Tell me a pancake recipe`

---

Author: Adam Sapiecha
Recruitment coding task submission

```
