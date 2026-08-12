# 🔥 Bad API Challenge

A deliberately unreliable Spring Boot API designed to teach resilient programming practices. This challenge simulates real-world production issues like random errors, rate limiting, and pagination requirements.

## 🎯 Challenge Overview

Participants must collect data for **2,000 people** by calling three misbehaving API endpoints, compute a value, and submit a complete CSV file. The first person to submit a correct CSV wins! Expected completion time: ~2 minutes with optimal code.

### The Mission

1. Call three API endpoints to collect:
   - First names
   - Surnames  
   - Ages

2. Compute the final value: `firstName + age + surname`
   - Example: Paul Jones, age 38 → `Paul38Jones`

3. Create a CSV file with all 2,000 records:
```csv
firstName,surname,age,computedValue
John,Smith,25,John25Smith
Mary,Johnson,30,Mary30Johnson
...
(2,000 total records)
```

4. Submit your CSV for validation

## 😈 The Catch

This API intentionally misbehaves to simulate production issues:

### Basic Bad Behaviors
- **Random HTTP Errors** (500, 503, 504) - Requires retry logic with exponential backoff
- **Cursor Pagination** - Requires proper pagination handling

### Network Chaos 🌪️
- **Random Slowness** (1-3 second delays) - 30% of requests are slow
- **Timeouts** - 5% of requests never respond (must implement timeout handling)
- **Variable Rate Limiting** - Rate limits change unpredictably (20-60 req/min) and vary per client

**The challenge is to write resilient code that handles these issues gracefully!**

### Why This Is Hard

1. **Unpredictable Rate Limits**: What worked once might not work again - rate limits change randomly per client
2. **Timeout Handling**: Simple retry won't work - you need proper timeout configuration on every request
3. **Slowness vs Errors**: Is it slow or dead? You need to wait... or retry?
4. **Combined Chaos**: All three issues happen simultaneously - requires sophisticated error handling

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation & Running

1. Clone the repository:
```bash
git clone <repository-url>
cd dariel-bad-api-springboot
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

4. Access the application:
   - **Web UI**: http://localhost:8080
   - **API Docs**: http://localhost:8080/swagger-ui.html
   - **API Base**: http://localhost:8080/api

## 📚 API Endpoints

### Data Endpoints

#### GET `/api/people/firstnames`
Returns all person IDs with first names.

**⚠️ Bad Behavior**: Random HTTP errors (500, 503, 504) with ~35% failure rate

```json
{
  "data": [
    {"id": 1, "firstName": "John"},
    {"id": 2, "firstName": "Mary"}
  ],
  "total": 2000
}
```

#### GET `/api/people/surnames`
Returns all person IDs with surnames.

**⚠️ Bad Behavior**: Rate limiting - 30 requests per minute, varies 20-60 (configurable)

```json
{
  "data": [
    {"id": 1, "surname": "Smith"},
    {"id": 2, "surname": "Johnson"}
  ],
  "total": 2000
}
```

#### GET `/api/people/ages?cursor={cursor}`
Returns paginated person IDs with ages (50 per page).

**⚠️ Bad Behavior**: Requires cursor-based pagination

```json
{
  "data": [
    {"id": 1, "age": 25},
    {"id": 2, "age": 30}
  ],
  "cursor": "50",
  "hasMore": true,
  "total": 2000
}
```

### Submission Endpoints

#### POST `/api/submit`
Submit your CSV solution (as text content).

**Request Parameters**:
- `participantName` (string, required)
- `csvContent` (string, required)

**Special Rule**: Submissions with <10% correct will redirect to a "GTFO" meme page 😅

#### POST `/api/submit/file`
Submit your CSV solution (as file upload).

**Request Parameters**:
- `participantName` (string, required)
- `file` (file, required)

### Leaderboard Endpoints

#### GET `/api/leaderboard`
Returns all successful submissions sorted by time.

#### GET `/api/stats`
Returns challenge statistics.

#### POST `/api/reset?adminKey=reset123`
Admin endpoint to reset all submissions.

## 🛠️ Configuration

Configure bad behavior settings in `application.properties`:

```properties
# Basic Bad Behaviors
badapi.random-error.failure-rate=0.35
badapi.rate-limit.requests-per-minute=30
badapi.rate-limit.window-seconds=60
badapi.pagination.page-size=100

# Network Chaos Configuration
badapi.chaos.random-slowness-enabled=true
badapi.chaos.slowness-probability=0.3        # 30% of requests will be slow
badapi.chaos.min-delay-seconds=1
badapi.chaos.max-delay-seconds=3
badapi.chaos.timeout-probability=0.05        # 5% chance of timeout

# Variable Rate Limiting (makes it unpredictable)
badapi.chaos.variable-rate-limit=true
badapi.chaos.min-rate-limit=20
badapi.chaos.max-rate-limit=60
```

These are the values shipped in `application.properties` (tuned for ~2 minute completion with 2,000 records). The alternate presets below are heavier and meant for the "Hard"/"Nightmare" modes described in `CHALLENGE_SETUP.md`.

### Difficulty Levels

**Easy Mode** (for beginners):
```properties
badapi.random-error.failure-rate=0.1
badapi.chaos.random-slowness-enabled=false
badapi.chaos.timeout-probability=0
badapi.chaos.variable-rate-limit=false
badapi.rate-limit.requests-per-minute=100
```

**Hard Mode** (default - recommended):
```properties
badapi.random-error.failure-rate=0.35
badapi.rate-limit.requests-per-minute=30
badapi.chaos.slowness-probability=0.3
badapi.chaos.min-delay-seconds=1
badapi.chaos.max-delay-seconds=3
badapi.chaos.timeout-probability=0.05
badapi.chaos.variable-rate-limit=true
badapi.chaos.min-rate-limit=20
badapi.chaos.max-rate-limit=60
```

**Nightmare Mode** (for experts):
```properties
badapi.random-error.failure-rate=0.5
badapi.chaos.slowness-probability=0.6
badapi.chaos.min-delay-seconds=10
badapi.chaos.max-delay-seconds=45
badapi.chaos.timeout-probability=0.25
badapi.chaos.min-rate-limit=3
badapi.chaos.max-rate-limit=50
```

## 💡 Resilient Programming Hints

### 1. Retry Logic with Exponential Backoff

```javascript
async function fetchWithRetry(url, maxRetries = 5) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url, { 
        timeout: 60000  // CRITICAL: Set timeout!
      });
      if (response.ok) return response;
      
      // Retry on 5xx errors
      if (response.status >= 500) {
        await sleep(Math.pow(2, i) * 1000); // Exponential backoff
        continue;
      }
      throw new Error(`HTTP ${response.status}`);
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await sleep(Math.pow(2, i) * 1000);
    }
  }
}
```

### 2. Rate Limit Handling (Variable Rates!)

```javascript
if (response.status === 429) {
  const retryAfter = response.headers.get('Retry-After') || 60;
  console.log(`Rate limited. Waiting ${retryAfter}s...`);
  await sleep(retryAfter * 1000);
  // Retry the request
  // WARNING: Rate limit might change on next attempt!
}
```

### 3. Timeout Configuration (CRITICAL!)

```javascript
// Without timeout, your code will hang forever on 5% of requests!
const controller = new AbortController();
const timeoutId = setTimeout(() => controller.abort(), 60000);

try {
  const response = await fetch(url, { signal: controller.signal });
  clearTimeout(timeoutId);
  return response;
} catch (error) {
  if (error.name === 'AbortError') {
    console.log('Request timed out, retrying...');
    // Retry
  }
}
```

### 4. Adaptive Rate Limiting

```javascript
// Since rate limits change, track your actual limits dynamically
let currentRateLimit = 50;  // Start optimistic
let requestCount = 0;
let windowStart = Date.now();

async function adaptiveRateLimitedFetch(url) {
  // Check if we're at our known limit
  if (requestCount >= currentRateLimit) {
    const elapsed = Date.now() - windowStart;
    if (elapsed < 60000) {
      await sleep(60000 - elapsed);
      requestCount = 0;
      windowStart = Date.now();
    }
  }
  
  try {
    const response = await fetchWithRetry(url);
    requestCount++;
    return response;
  } catch (error) {
    if (error.status === 429) {
      // We hit the limit - it's lower than we thought!
      currentRateLimit = Math.floor(requestCount * 0.8);
      console.log(`Rate limit lowered to ${currentRateLimit}`);
      throw error;
    }
  }
}
```

### 5. Circuit Breaker Pattern

```javascript
// If service keeps failing, stop hammering it
class CircuitBreaker {
  constructor(threshold = 5, timeout = 60000) {
    this.failureCount = 0;
    this.threshold = threshold;
    this.timeout = timeout;
    this.state = 'CLOSED';  // CLOSED, OPEN, HALF_OPEN
    this.nextAttempt = Date.now();
  }
  
  async call(fn) {
    if (this.state === 'OPEN') {
      if (Date.now() < this.nextAttempt) {
        throw new Error('Circuit breaker is OPEN');
      }
      this.state = 'HALF_OPEN';
    }
    
    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }
  
  onSuccess() {
    this.failureCount = 0;
    this.state = 'CLOSED';
  }
  
  onFailure() {
    this.failureCount++;
    if (this.failureCount >= this.threshold) {
      this.state = 'OPEN';
      this.nextAttempt = Date.now() + this.timeout;
      console.log('Circuit breaker opened!');
    }
  }
}
```

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

Tests include:
- Validation logic tests
- Bad behavior service tests
- Controller integration tests
- Application context loading test

## 📁 Project Structure

```
src/main/java/com/challenge/badapi/
├── BadApiApplication.java          # Main application
├── config/
│   └── SwaggerConfig.java          # API documentation config
├── model/
│   ├── Person.java                 # Person entity
│   ├── Submission.java             # Submission entity
│   └── ApiResponse.java            # Generic API response wrapper
├── controller/
│   ├── PeopleController.java       # Data endpoints (with bad behaviors)
│   ├── SubmissionController.java   # Submission handling
│   └── LeaderboardController.java  # Leaderboard & stats
├── service/
│   ├── DataService.java            # Generate 2000 unique people
│   ├── BadBehaviorService.java     # Implement misbehaviors
│   ├── ValidationService.java      # CSV validation
│   └── LeaderboardService.java     # Leaderboard management
└── repository/
    └── SubmissionRepository.java   # In-memory submission storage

src/main/resources/
├── application.properties          # Configuration
└── static/                         # Web UI
    ├── index.html                  # Landing page
    ├── docs.html                   # Documentation (exportable)
    ├── submit.html                 # Submission form
    ├── leaderboard.html            # Leaderboard display
    ├── gtfo.html                   # Meme page for <10% success
    └── styles.css                  # Styling
```

## 🎓 Learning Objectives

This challenge teaches:

1. **Retry Logic**: Handling transient failures with exponential backoff
2. **Rate Limiting**: Respecting API rate limits and Retry-After headers
3. **Pagination**: Implementing cursor-based pagination
4. **Error Handling**: Graceful degradation and proper error messages
5. **Concurrency**: Managing parallel requests efficiently
6. **Data Consistency**: Ensuring data integrity across multiple API calls
7. **Resilience Patterns**: Circuit breakers, timeouts, and fallbacks

## 🏆 Winning Strategy

1. **Start Simple**: Get one endpoint working first
2. **Add Retry Logic**: Handle random errors with exponential backoff
3. **Respect Rate Limits**: Don't hammer the API, implement throttling
4. **Handle Pagination**: Loop through all pages for ages endpoint
5. **Validate Locally**: Check your CSV before submitting
6. **Test Incrementally**: Don't wait until you have all 2,000 records

## 🎨 Web Interface

The challenge includes a full web interface:

- **Landing Page**: Challenge instructions and quick start
- **Documentation**: Complete API reference (exportable as Markdown/PDF)
- **Submit Page**: Upload CSV or paste content for validation
- **Leaderboard**: Live rankings and statistics
- **GTFO Page**: Special page for submissions with <10% success 😅

## 🔧 Troubleshooting

### Random Errors Won't Stop
- Implement retry logic with exponential backoff
- Add a maximum retry count to prevent infinite loops

### Getting Rate Limited
- Reduce concurrent requests
- Implement a request queue
- Respect the Retry-After header

### Can't Get All Ages
- The ages endpoint requires pagination
- Use the cursor from each response to get the next page
- Continue until `cursor` is null or `hasMore` is false

### CSV Validation Fails
- Check the header format exactly: `firstName,surname,age,computedValue`
- Verify the computed value formula: `firstName + age + surname`
- Ensure all 2,000 records are present with no duplicates
- Make sure ages are integers, not strings

## 📝 License

This project is for educational purposes.

## 🤝 Contributing

This is a challenge project. If you have ideas for additional "bad behaviors" to teach more resilience patterns, feel free to suggest them!

---

**Good luck, and remember**: The API is meant to frustrate you. That's how you learn! 🚀

