# Job Agent Learning

A Spring Boot application combining Spring AI, Ollama, Jev AI, and MySQL to search stored job listings and analyze resume-to-job fit.

> **Current limitation:** The supplied code searches job records already stored in MySQL. It does not yet fetch live jobs from Naukri or submit applications. Those capabilities require a permitted job-source integration and a separate application workflow.

## Features

- Search jobs in MySQL by keyword, location, and minimum salary.
- Extract search criteria from natural-language requests with Ollama.
- Use Spring AI tool calling to invoke Java job-search methods.
- Score resume/job fit through the Jev API.
- Generate detailed resume analysis with Ollama.
- Expose REST APIs for job search and resume analysis.

## Technology

- Java and Maven (use versions configured in `pom.xml`)
- Spring Boot and Spring AI `ChatClient`
- Ollama (currently configured as `llama3.2`)
- Jev AI API
- MySQL
- Postman or another HTTP client

## Architecture

```text
Client / Postman
       |
       v
Spring Boot controllers
       |
       +--> /api/agent/jobs --> ChatClient (Ollama) --> JobSearchTools
       |                                                |
       |                                                v
       |                                      JobRepository --> MySQL
       |
       +--> /api/agent/chat --> Ollama extracts criteria --> JobSearchTools
       |
       +--> /api/resume/match
       |       +--> JevService --> Jev API (score/category)
       |       +--> ResumeAnalysisService --> Ollama
       |
       +--> /api/resume/agent-analysis
               +--> Ollama ChatClient --> JevResumeScoringTool --> Jev API
```

## Prerequisites

Install Java, Maven, MySQL, and [Ollama](https://ollama.com/). Obtain a Jev API key.

Check installations:

```bash
java -version
mvn -version
ollama --version
mysql --version
```

## 1. Configure MySQL

Create the database:

```sql
CREATE DATABASE jobagent;
```

Example `src/main/resources/application.yml` datasource configuration:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jobagent?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      connection-timeout: 10000
      initialization-fail-timeout: 10000
  jpa:
    open-in-view: false
```

Set the database password in the shell used to start Spring Boot:

```bash
export DB_PASSWORD='your-local-mysql-password'
```

Ensure the `Job` entity, `JobRepository`, and database schema agree. Insert some job records before testing search endpoints. `JobSearchTools` currently filters database rows; it does not retrieve live listings from Naukri.

## 2. Configure Jev

The supplied `JevService` sends requests to:

```text
https://jev-ai.org/api/v1/systemone/
```

It sends model identifier `jev-1.13`, the resume and job description in `state`, and questions for the match score, job category, and detailed analysis.

Set the API key:

```bash
export JEV_API_KEY='your-jev-api-key'
```

Configure the property in `application.yml`:

```yaml
jev:
  api-key: ${JEV_API_KEY}
```

Never commit API keys to Git. Rotate a key if it has been exposed.

**Important:** `jev-1.13` is the model identifier sent to the Jev service. It is not an Ollama model created with a `Modelfile`.

## 3. Install and test the Ollama base model

Download the configured model:

```bash
ollama pull llama3.2
ollama list
ollama run llama3.2
```

Type a prompt to verify it responds, then exit the interactive session.

## 4. Create a custom Ollama model

A custom Ollama model can have a project-specific name and system prompt. This configures a model on top of an existing model; it **does not train or fine-tune new model weights**.

### Step 1: Create a `Modelfile`

Create a file named `Modelfile` in the project root, alongside `pom.xml`:

```text
FROM llama3.2

PARAMETER temperature 0.2

SYSTEM """
You are a careful job-search and resume-analysis assistant.
Use only facts provided in the resume, job description, or job records.
Do not invent jobs, skills, employers, salaries, or experience.
Distinguish between a skill that is missing and a skill that is simply
not mentioned.
For job searches, use available job-search tools instead of inventing
job listings.
"""
```

### Step 2: Build the custom model

In a terminal, change to the directory containing the `Modelfile`, then run:

```bash
ollama create job-agent-model -f Modelfile
ollama list
```

You should see `job-agent-model` in the model list.

### Step 3: Test it

```bash
ollama run job-agent-model
```

Try asking it to explain how to evaluate a Java Spring Boot role against a resume without inventing candidate experience.

### Step 4: Configure Spring AI

In `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: job-agent-model
          temperature: 0.2
```

The configured name must match the name used in `ollama create`. Restart Spring Boot after changing the YAML. To keep using the original model, set `model: llama3.2`.

### Step 5: Verify Ollama over HTTP

```bash
curl http://localhost:11434/api/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "job-agent-model",
    "messages": [
      {
        "role": "user",
        "content": "Say hello and explain your role as a job-search assistant."
      }
    ],
    "stream": false
  }'
```

If it fails, check that Ollama is running, the model exists in `ollama list`, and port `11434` is reachable.

## 5. How Jev and Ollama work together

### Jev scoring

`JevService.evaluateResume(resume, jobDescription)` checks that `JEV_API_KEY` is configured, sends the resume and job description to Jev, and parses the JSON response.

`JevResumeScoringTool.scoreResume(...)` reads:

- `answers.java_match.noul`
- `answers.job_category.choice`
- `id` (response ID)

It returns the probability, percentage, category, and response ID.

### Ollama analysis

`ResumeAnalysisService` asks Ollama to identify:

- Matching technical skills.
- Missing or unverified requirements.
- Relevant experience.
- Alignment with job responsibilities.
- Resume improvement suggestions.

`ResumeAgentService` uses another flow: it asks Ollama to invoke `scoreResumeWithJev` and then write a combined analysis. Tool calling depends on the chosen model and Spring AI configuration supporting it. If tool invocation is unreliable, call Jev directly from Java and combine the results in application code.

The resume endpoints follow different flows:

- `/api/resume/match`: Java calls Jev directly, then calls Ollama for detailed analysis.
- `/api/resume/agent-analysis`: Ollama is asked to call the Jev scoring tool.

Use `/api/resume/match` when Jev scoring must happen deterministically without relying on the LLM to choose a tool.

## 6. REST API endpoints

The project has been configured to use port `7096`.

Base URL: `http://localhost:7096`

### A. Structured job search

`POST /api/agent/jobs`

```json
{
  "intent": "SEARCH",
  "jobTitle": "Java",
  "location": "Noida",
  "minSalaryLpa": 20
}
```

The `SEARCH` intent asks the chat model to call `searchJobs`, which filters existing MySQL records.

### B. Job details

`POST /api/agent/jobs`

```json
{
  "intent": "DETAILS",
  "jobTitle": "Senior Java Developer"
}
```

The `DETAILS` intent calls `getJobDetails` using an exact, case-insensitive title match.

### C. Natural-language job search

`POST /api/agent/chat`

```json
{
  "message": "Find Java developer jobs in Noida with minimum salary of 20 LPA"
}
```

Ollama extracts the keyword, location, and minimum salary into JSON. The controller parses it into `JobSearchCriteria` and calls `JobSearchTools.findMatchingJobs`.

### D. Resume match and analysis

`POST /api/resume/match`

```json
{
  "resume": "Paste candidate resume text here",
  "jobDescription": "Paste the complete job description here"
}
```

The response includes the Jev match probability, percentage, category, recommendation, response ID, and detailed Ollama analysis.

### E. Resume agent analysis with tool calling

`POST /api/resume/agent-analysis`

```json
{
  "resume": "Paste candidate resume text here",
  "jobDescription": "Paste the complete job description here"
}
```

This endpoint asks Ollama to call `scoreResumeWithJev` before producing its analysis.

For each request, use the header:

```text
Content-Type: application/json
```

## 7. Run the application

From the project root:

```bash
./mvnw spring-boot:run
```

If the Maven wrapper is unavailable:

```bash
mvn spring-boot:run
```

You can also run `JobAgentLearningApplication` from IntelliJ IDEA.

Before testing, confirm that MySQL and Ollama are running, the `jobagent` database contains job records, the configured Ollama model exists, and `JEV_API_KEY` and `DB_PASSWORD` are set in the environment used to launch Spring Boot.

## 8. Suggested next development steps

1. Add a job-source adapter using an authorized API or permitted integration.
2. Store source job ID, canonical URL, date found, and description.
3. Deduplicate jobs before saving them.
4. Score each new job against the resume with Jev.
5. Generate a detailed explanation with Ollama.
6. Persist the score and analysis to avoid unnecessary repeated calls.
7. Add application statuses such as `SAVED`, `READY_FOR_REVIEW`, `APPLIED`, `INTERVIEW`, and `REJECTED`.
8. Generate tailored resume versions and draft application answers for review.
9. Submit applications only through an authorized application workflow, with user approval where required.

## 9. Troubleshooting

| Symptom | Checks |
|---|---|
| Ollama connection refused | Start Ollama and verify port `11434` is reachable. |
| Model not found | Run `ollama list` and check the configured model name. |
| Jev API key missing | Set `JEV_API_KEY` in the environment used to launch Spring Boot. |
| Jev response parsing fails | Check the actual response schema and expected `answers` fields; never log the API key. |
| No jobs returned | Confirm MySQL contains jobs and check keyword, location, and salary filters. |
| LLM does not invoke a tool | Confirm model tool-calling support and Spring AI registration; use a direct Java call for mandatory Jev scoring. |
| Port conflict | Check whether another process is using port `7096`. |

## Security notes

- Keep API keys and database credentials out of source control.
- Avoid logging full resumes or personal application data.
- Validate request fields and set reasonable request-size limits.
- Treat job descriptions and other external text as untrusted input.
- An AI match score does not guarantee an interview.
- Do not automate login or job submission unless the job site permits the integration.
