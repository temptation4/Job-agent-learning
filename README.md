# AI Job Search & Resume Matching Agent — Jev AI, Custom Ollama Model & LLM Integration

An AI-powered Job Search and Resume Matching Agent built using **Java, Spring Boot, Spring AI, Jev AI, Ollama, a custom LLM, and MySQL**.

This project demonstrates how to integrate an external AI API such as Jev AI with a locally running Large Language Model (LLM), create a custom Ollama model using a `Modelfile`, and build AI-powered job search and resume analysis capabilities in a Java backend application.

## Key Highlights

This project focuses on three core areas:

- **Jev AI integration:** Evaluate resume-job compatibility and obtain match scores through the Jev AI API.
- **Custom LLM development:** Create and run a custom Ollama model using a `Modelfile` with instructions tailored to job search and resume analysis.
- **Spring AI and LLM integration:** Use Spring AI's `ChatClient` and tool calling to generate resume analysis, identify skill gaps, suggest improvements, and search jobs stored in MySQL.

## Features

- Job search by keyword, location, and minimum salary.
- Natural-language job search using an LLM.
- Spring AI tool calling to invoke Java methods.
- Jev AI integration for resume-job match scoring.
- Resume analysis using a local Ollama model.
- Custom Ollama model creation using a `Modelfile`.
- Integration of the custom model with Spring AI.
- Skill gap identification and resume improvement suggestions.
- REST APIs for job search and resume analysis.
- MySQL-backed job data retrieval.
- Separate service integrations for Jev AI scoring and LLM-generated analysis.

> **Current limitation:** Job search currently filters records stored in MySQL. Live Naukri job discovery and automated job submission have not yet been implemented.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Backend programming language |
| Spring Boot | REST APIs and application framework |
| Spring AI | LLM integration and tool calling |
| Jev AI | Resume-job match scoring |
| Ollama | Local LLM runtime |
| Llama 3.2 | Base model for the custom Ollama model |
| Custom Ollama model | Job search and resume analysis instructions |
| MySQL | Job data storage |
| Spring Data JPA | Database access |
| Maven | Dependency management and build |
| Postman | API testing |

## Architecture

```text
                    Client / Postman
                           |
                           v
                 Spring Boot REST APIs
                           |
             +-------------+-------------+
             |                           |
             v                           v
        Job Search                 Resume Analysis
             |                           |
             v                           v
     Spring AI ChatClient          Resume Services
             |                           |
             v                    +------+------+
      Custom Ollama Model         |             |
      job-agent-model              v             v
             |                  Jev AI       Ollama LLM
             v                  API          Analysis
       JobSearchTools
             |
             v
       JobRepository
             |
             v
           MySQL
```

The application combines a local LLM with an external AI scoring API:

- **Ollama** runs the custom local model and generates text-based analysis.
- **Jev AI** provides resume-job matching information through its API.
- **Spring AI** connects the application to the local chat model and supports tool calling.
- **MySQL** stores job records used by the job search functionality.

## 1. Prerequisites

Install the following tools before running the application:

- Java compatible with the project's `pom.xml`
- Maven
- MySQL
- Ollama
- Jev AI API key
- Postman
- Git (optional, for version control)

Verify the installations:

```bash
java -version
mvn -version
ollama --version
mysql --version
```

## 2. Clone or Open the Project

If the repository is already on your computer, navigate to its root directory:

```bash
cd ~/Downloads/job-agent-learning
```

Verify the project files:

```bash
ls
```

You should see files such as `pom.xml`, `src`, and `application.yml` under the resources directory.

## 3. Configure MySQL

Create the database:

```sql
CREATE DATABASE jobagent;
```

Configure the datasource in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jobagent?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    open-in-view: false
```

Set your local database password in the terminal:

```bash
export DB_PASSWORD='your-local-mysql-password'
```

Ensure that the following components are configured correctly:

- `Job` entity
- `JobRepository`
- Database schema and JPA settings

Add job records to MySQL before testing job search. The search APIs operate on the available database records rather than retrieving live job listings.

## 4. Configure Jev AI

The project integrates with the Jev AI API:

```text
https://jev-ai.org/api/v1/systemone/
```

The current implementation uses the model identifier `jev-1.13` and sends the resume, job description, and scoring questions to the API.

Configure your Jev AI API key as an environment variable:

```bash
export JEV_API_KEY='your-jev-api-key'
```

Add the corresponding configuration to `application.yml`:

```yaml
jev:
  api-key: ${JEV_API_KEY}
```

The application uses Jev AI to obtain resume matching information, including a match probability or percentage and job category, as supported by the API response.

**Important security notes:**

- Never commit your API key to GitHub.
- Do not put real API keys in `application.yml`.
- If a real key has previously been exposed, rotate it before publishing the repository.
- The Jev model identifier is separate from the local Ollama model.

## 5. Install and Run Ollama

Ollama allows you to run an LLM locally.

Download and install it from:

https://ollama.com/download

### Step 1: Download the base model

```bash
ollama pull llama3.2
```

### Step 2: Verify the model

```bash
ollama list
```

### Step 3: Run the base model

```bash
ollama run llama3.2
```

Test it with a prompt:

```text
Explain how to compare a Java developer's resume with a job description.
```

This verifies that the base model is available before creating your custom model.

## 6. Create a Custom Ollama Model

This project uses a custom model named `job-agent-model`.

The model is based on `llama3.2`, with a custom system prompt and generation parameters.

**Important:** Creating a model from a `Modelfile` customizes the existing model's configuration. It does not train or fine-tune new model weights.

### Step 1: Navigate to the project root

```bash
cd ~/Downloads/job-agent-learning
```

Create a file named `Modelfile` alongside `pom.xml`:

```bash
touch Modelfile
```

### Step 2: Add the model configuration

Open `Modelfile` in your editor and add:

```text
FROM llama3.2

PARAMETER temperature 0.2

SYSTEM """
You are an AI job-search and resume-analysis assistant.

Help users find relevant jobs and evaluate resumes against job descriptions.

Use only the information provided in the resume, job description,
and available job records.

Do not invent jobs, skills, employers, salaries, or professional experience.

Identify matching skills, missing or unverified requirements,
relevant experience, and resume improvement suggestions.

For job searches, use available job-search tools instead of inventing
job listings.
"""
```

### Understanding the Modelfile

- `FROM llama3.2`: Selects the base model.
- `PARAMETER temperature 0.2`: Controls response variability; a lower value generally makes responses more consistent.
- `SYSTEM`: Defines the assistant's default role and behavior.

### Step 3: Build the custom model

Run the following command from the directory containing the `Modelfile`:

```bash
ollama create job-agent-model -f Modelfile
```

Ollama creates a model named `job-agent-model` using the base model and your configuration.

### Step 4: Verify the custom model

```bash
ollama list
```

You should see `job-agent-model` in the list.

### Step 5: Run the custom model

```bash
ollama run job-agent-model
```

Test it with:

```text
I am a Java backend developer with Spring Boot and Kafka experience.
What should you evaluate when comparing my resume with a job description?
```

You can also test whether it avoids inventing job listings:

```text
Find Java developer jobs in Noida with a minimum salary of 25 LPA.
Do not invent job listings if no job data is available.
```

The model should follow its configured instructions. However, the custom model by itself cannot query your MySQL database or retrieve live job listings. Those capabilities must be provided by the Spring Boot application.

### Step 6: Rebuild the model after changing the Modelfile

If you change the system prompt or parameters, recreate the model:

```bash
ollama create job-agent-model -f Modelfile
```

Then test it again:

```bash
ollama run job-agent-model
```

## 7. Integrate the Custom Model with Spring AI

Spring AI provides abstractions such as `ChatClient` to interact with LLMs from Java.

Open `src/main/resources/application.yml` and configure Ollama:

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

If the file already contains datasource, Jev AI, server, or other settings, merge this configuration with the existing file rather than replacing everything.

The model name must match the name used in:

```bash
ollama create job-agent-model -f Modelfile
```

Restart the Spring Boot application after changing the configuration.

### How ChatClient uses the model

Example:

```java
@Service
public class ResumeAnalysisService {

    private final ChatClient chatClient;

    public ResumeAnalysisService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String analyzeResume(
            String resume,
            String jobDescription) {

        return chatClient.prompt()
                .system("""
                    You are an expert technical recruiter.
                    Do not invent skills or experience.
                    """)
                .user("""
                    Analyze the resume against the job description.

                    Resume:
                    %s

                    Job Description:
                    %s
                    """.formatted(resume, jobDescription))
                .call()
                .content();
    }
}
```

When this service invokes `chatClient`, Spring AI uses the configured chat model. In this project, that model is `job-agent-model`.

The request-specific system prompt adds instructions for that particular operation alongside the model's default instructions.

## 8. Test the Custom Model Through the Ollama API

You can test Ollama independently of Spring Boot.

```bash
curl http://localhost:11434/api/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "job-agent-model",
    "messages": [
      {
        "role": "user",
        "content": "Explain your role as a job-search and resume-analysis assistant."
      }
    ],
    "stream": false
  }'
```

A successful request returns a JSON response containing the generated message.

This verifies that:

1. Ollama is accessible.
2. The custom model exists.
3. The model can generate a response through the HTTP API.

## 9. How Jev AI and Ollama Work Together

The project uses two different AI integrations for different responsibilities.

### Jev AI — Resume matching

`JevService` calls the external Jev API with the resume, job description, and relevant questions.

The current scoring integration reads fields such as:

- `answers.java_match.noul`
- `answers.job_category.choice`
- `id`

These fields are used to extract match information, category information, and the Jev response identifier. The actual output depends on the API response.

### Ollama — Detailed resume analysis

The custom Ollama model generates detailed text-based analysis, including:

1. Matching technical skills.
2. Missing or unverified requirements.
3. Relevant experience.
4. Alignment with job responsibilities.
5. Resume improvement suggestions.

### Two resume-analysis flows

**Flow 1: Direct Jev scoring**

Endpoint:

```text
POST /api/resume/match
```

The application calls Jev AI directly from Java and then uses Ollama to generate detailed analysis.

**Flow 2: Spring AI tool calling**

Endpoint:

```text
POST /api/resume/agent-analysis
```

The application asks Ollama to invoke `JevResumeScoringTool` through Spring AI tool calling before generating the analysis.

The direct Java service approach is preferable when Jev scoring must happen deterministically in application code. The tool-calling approach demonstrates how an LLM can request that a Java method be executed.

## 10. Job Search with Spring AI Tool Calling

The application includes `JobSearchTools`, which exposes Java methods such as:

- `searchJobs`
- `getJobDetails`

These methods search job records stored in MySQL through the application's repository layer.

The high-level flow is:

```text
User request
     |
     v
Spring Boot Controller
     |
     v
Spring AI ChatClient
     |
     v
LLM determines whether a tool is needed
     |
     v
Java JobSearchTools method
     |
     v
JobRepository
     |
     v
MySQL job records
     |
     v
Tool result returned to the LLM
     |
     v
Response to the user
```

Tool calling does not automatically give the LLM access to arbitrary databases or external websites. The application explicitly defines the tools and the operations they can perform.

## 11. REST API Endpoints

The application is configured to run on port `7096`.

Base URL:

```text
http://localhost:7096
```

### A. Structured Job Search

**Endpoint**

```http
POST /api/agent/jobs
```

Example request:

```json
{
  "intent": "SEARCH",
  "jobTitle": "Java",
  "location": "Noida",
  "minSalaryLpa": 20
}
```

The application searches existing MySQL job records using the configured job search functionality.

### B. Get Job Details

**Endpoint**

```http
POST /api/agent/jobs
```

Example request:

```json
{
  "intent": "DETAILS",
  "jobTitle": "Senior Java Developer"
}
```

The application searches for a matching job title in the available records.

### C. Natural-Language Job Search

**Endpoint**

```http
POST /api/agent/chat
```

Example request:

```json
{
  "message": "Find Java developer jobs in Noida with minimum salary of 20 LPA"
}
```

The LLM extracts search criteria, such as keyword, location, and minimum salary. The application converts the criteria into a search request and queries MySQL.

### D. Resume Match

**Endpoint**

```http
POST /api/resume/match
```

Example request:

```json
{
  "resume": "Java developer with Spring Boot, Kafka, microservices and MySQL experience.",
  "jobDescription": "Senior Java Developer required with Java, Spring Boot, Kafka, microservices and SQL experience."
}
```

This endpoint uses Jev AI for matching information and Ollama for detailed analysis, according to the current implementation.

### E. Resume Agent Analysis

**Endpoint**

```http
POST /api/resume/agent-analysis
```

Example request:

```json
{
  "resume": "Java developer with Spring Boot, Kafka, microservices and MySQL experience.",
  "jobDescription": "Senior Java Developer required with Java, Spring Boot, Kafka, microservices and SQL experience."
}
```

This endpoint demonstrates an agent-style flow in which Ollama is asked to invoke the Jev resume-scoring tool through Spring AI tool calling.

For all endpoints, set:

```http
Content-Type: application/json
```

The exact response schema depends on the application's controller and service implementations.

## 12. Run the Complete Application

Before starting the application, verify that:

- MySQL is running.
- The `jobagent` database exists.
- Job records have been added to MySQL.
- Ollama is installed and running.
- The base model `llama3.2` is available.
- The custom model `job-agent-model` has been created.
- `DB_PASSWORD` is configured.
- `JEV_API_KEY` is configured.

### Start Ollama

On systems where Ollama is not already running as a background service, start it using:

```bash
ollama serve
```

If Ollama is already running, do not start a second server.

### Verify the custom model

```bash
ollama list
```

### Start Spring Boot

From the project root:

```bash
./mvnw spring-boot:run
```

If the Maven wrapper is unavailable:

```bash
mvn spring-boot:run
```

Alternatively, run the main application class from IntelliJ IDEA.

Once the application starts, test the endpoints using Postman.

## 13. Troubleshooting

### Custom model not found

Check the installed models:

```bash
ollama list
```

Recreate the model if necessary:

```bash
ollama create job-agent-model -f Modelfile
```

### Ollama connection refused

Verify that Ollama is running and that the local API is accessible at:

```text
http://localhost:11434
```

### Spring AI still uses the previous model

Check `application.yml` and any profile-specific configuration or environment variables that might override the configured model. Restart Spring Boot after making changes.

### Modelfile changes are not reflected

Recreate the custom model:

```bash
ollama create job-agent-model -f Modelfile
```

Test it again:

```bash
ollama run job-agent-model
```

### MySQL connection failure

Verify:

- MySQL is running.
- The database name is `jobagent`.
- The username and password are correct.
- `DB_PASSWORD` is set in the environment used to start Spring Boot.

### Jev AI request failure

Verify that `JEV_API_KEY` is set correctly, the key is valid, and the API endpoint is reachable. Check the API response and application logs for details.

### No jobs returned

The job search implementation filters records stored in MySQL. Confirm that job records exist and that their titles, locations, and salary values match your search criteria.

## 14. Future Enhancements

Potential future improvements include:

- Integrate an authorized live job source or permitted job API.
- Add job deduplication and persistent resume-job match scores.
- Rank available jobs based on resume compatibility.
- Generate tailored resume versions for specific job descriptions.
- Generate draft application answers for user review.
- Add an application tracker with statuses such as `SAVED`, `APPLIED`, `INTERVIEW`, and `REJECTED`.
- Add a review-and-approval workflow before submitting applications through an authorized integration.
- Add automated tests for Jev AI integration and Spring AI tool calling.
- Add structured output validation and retry handling for AI responses.
- Add observability, error handling, and request timeouts for external AI calls.
- Explore model evaluation and fine-tuning if a suitable dataset and training workflow become available.

## 15. Security and Responsible AI

- Never commit API keys, passwords, or other secrets.
- Use environment variables or a secrets manager for sensitive configuration.
- Avoid logging complete resumes and personal application data.
- Validate incoming REST API requests.
- Treat resumes and external job descriptions as untrusted input.
- Validate AI-generated results before using them in business decisions.
- Do not assume that a match score guarantees an interview or job offer.
- Do not automate job-site login or application submission unless the relevant site permits that integration.

## 16. Useful Ollama Commands

```bash
# Download the base model
ollama pull llama3.2

# List installed models
ollama list

# Create the custom model
ollama create job-agent-model -f Modelfile

# Run the custom model interactively
ollama run job-agent-model

# Remove the custom model if no longer needed
ollama rm job-agent-model
```

## Conclusion

This project demonstrates the integration of **Jev AI, a custom Ollama model, and Spring AI** in a Java backend application.

It combines external AI-based resume matching with local LLM analysis, demonstrates custom model configuration using an Ollama `Modelfile`, and uses Spring AI tool calling to connect LLM workflows with Java services and MySQL-backed job search.

The project provides a practical foundation for exploring LLM integration, agent-style workflows, resume analysis, and AI-powered recruitment applications using the Spring ecosystem.
