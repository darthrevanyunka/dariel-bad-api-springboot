# Solution Reference

This directory contains reference solutions for the Bad API Challenge. These demonstrate the resilient programming patterns needed to successfully complete the challenge.

## ⚠️ Important Note

**Do NOT share these solutions with participants before the challenge!** These are for:
- Testing that the API works correctly
- Verifying the challenge is solvable
- Demonstrating best practices after the challenge
- Your own reference

## Available Solutions

### JavaScript/Node.js Solution

**File**: `solution.js`

**Requirements**: Node.js 18+ (with native fetch support)

For older Node versions, install node-fetch:
```bash
npm install node-fetch@2
```
Then uncomment the line at the top of the file.

**Usage**:
```bash
# Make sure the API is running first!
# In another terminal: mvn spring-boot:run

# Run the solution
node solution.js "Your Name"

# Or with default name
node solution.js
```

### Python Solution

**File**: `solution.py`

**Requirements**: Python 3.7+

Install dependencies:
```bash
pip install requests
```

**Usage**:
```bash
# Make sure the API is running first!

# Run the solution
python3 solution.py "Your Name"

# Or with default name
python3 solution.py

# Make executable and run directly (Unix/Mac)
chmod +x solution.py
./solution.py "Your Name"
```

### Java Solution

**Files**: `java/pom.xml`, `java/src/main/java/Solution.java`

**Requirements**: JDK 17+, Maven 3.6+

Uses `java.net.http.HttpClient` (built into the JDK — no HTTP library needed) plus Jackson for JSON parsing, bundled into a runnable jar via `maven-shade-plugin`.

**Usage**:
```bash
# Make sure the API is running first!
# In another terminal: mvn spring-boot:run

cd java
mvn -q package

java -jar target/solution-java.jar "Your Name"

# Or with default name
java -jar target/solution-java.jar
```

## What These Solutions Demonstrate

### 1. Retry Logic with Exponential Backoff
```javascript
// Handles random 5xx errors
if (response.status >= 500) {
    const backoffTime = INITIAL_BACKOFF_MS * Math.pow(2, retries);
    await sleep(backoffTime);
    return fetchWithRetry(url, options, retries + 1);
}
```

### 2. Rate Limit Handling
```javascript
// Respects 429 responses and Retry-After headers
if (response.status === 429) {
    const retryAfter = response.headers.get('Retry-After') || 60;
    await sleep(parseInt(retryAfter) * 1000);
    return fetchWithRetry(url, options, retries);
}
```

### 3. Cursor-Based Pagination
```javascript
// Loops through all pages using cursor
do {
    const url = cursor 
        ? `${API_BASE_URL}/people/ages?cursor=${cursor}`
        : `${API_BASE_URL}/people/ages`;
    
    const response = await fetchWithRetry(url);
    const data = await response.json();
    
    allAges.push(...data.data);
    cursor = data.cursor;
    
} while (cursor !== null);
```

### 4. Data Validation
```javascript
// Ensures all 2000 records are present
if (firstNames.size !== 2000 || surnames.size !== 2000 || ages.size !== 2000) {
    throw new Error('Incomplete data!');
}
```

### 5. Proper CSV Generation
```javascript
// Correct computed value formula
const computedValue = `${firstName}${age}${surname}`;
rows.push(`${firstName},${surname},${age},${computedValue}`);
```

## Output

Both solutions will:
1. Fetch all data from the three endpoints
2. Handle all the "bad behaviors" gracefully
3. Generate a CSV file: `solution_output.csv`
4. Submit the solution to the API
5. Display the validation results

Example output:
```
🚀 Bad API Challenge - Solution Starting...
============================================================

📡 Step 1: Fetching data from all endpoints...

📥 Fetching first names...
✅ Got 2000 first names

📥 Fetching surnames...
⏳ Rate limited. Waiting 10s...
✅ Got 2000 surnames

📥 Fetching ages (paginated)...
  📄 Page 1: Got 100 records (cursor: start)
  📄 Page 2: Got 100 records (cursor: 100)
  ...
  📄 Page 20: Got 100 records (cursor: 1900)
✅ Got 2000 ages total from 20 pages

✔️  Step 2: Validating data...
  First names: 2000
  Surnames: 2000
  Ages: 2000

✔️  Step 3: Generating CSV...
✅ Generated 2000 valid records
💾 Saved CSV to: solution_output.csv

✔️  Step 4: Submitting solution...

============================================================
🎉 SUCCESS! 🎉
✅ Perfect! All 2000 records are correct!
📊 Correct Records: 2000/2000
⏰ Submitted at: 2025-11-03T10:30:00
============================================================

⏱️  Total time: 45.32s

🏆 Challenge completed successfully!
```

## Testing Scenarios

You can use these solutions to test:

1. **Normal Operation**: Run with default config
2. **High Error Rate**: Increase `badapi.random-error.failure-rate` to 0.8
3. **Strict Rate Limiting**: Reduce `badapi.rate-limit.requests-per-minute` to 10
4. **Small Pages**: Reduce `badapi.pagination.page-size` to 10

## Adapting for Other Languages

The patterns demonstrated here can be adapted to any language:

- **Go**: Use `net/http` with custom retry logic
- **C#**: Use HttpClient with Polly for resilience
- **Ruby**: Use Faraday with retry middleware

## Key Takeaways

1. **Always implement retry logic** for transient failures
2. **Respect rate limits** - check headers and back off
3. **Handle pagination properly** - don't assume all data in one request
4. **Validate data** before processing
5. **Log errors** for debugging
6. **Use exponential backoff** to avoid overwhelming the server

## Notes

- All three solutions are functionally equivalent
- Choose the language your participants are most comfortable with as an example
- The solutions are intentionally verbose with logging to show what's happening
- Production code might abstract these patterns into reusable libraries

---

**Remember**: The goal is to teach resilient programming, not perfect code. These solutions prioritize clarity over brevity!

