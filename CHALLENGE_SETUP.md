# Challenge Setup Guide

This guide will help you configure and run the Bad API Challenge for your presentation.

## Quick Start

1. **Start the API**:
   ```bash
   cd /Users/jurgenroos/workspace/dariel-bad-api-springboot
   mvn spring-boot:run
   ```

2. **Access the Challenge**:
   - Web UI: http://localhost:8080
   - API Docs: http://localhost:8080/swagger-ui.html
   - Leaderboard: http://localhost:8080/leaderboard.html

3. **Test It Works**:
   ```bash
   cd solution
   node solution.js "Test User"
   ```

## Difficulty Levels

### Easy Mode (For Beginners)
Good for learning the basics of retry logic and pagination.

Edit `src/main/resources/application.properties`:
```properties
badapi.random-error.failure-rate=0.1
badapi.chaos.random-slowness-enabled=false
badapi.chaos.timeout-probability=0
badapi.chaos.variable-rate-limit=false
badapi.rate-limit.requests-per-minute=100
```

**Challenge time**: ~5-10 minutes for experienced developers

### Hard Mode (Default - Recommended)
The full network chaos experience! Tuned for ~2 minute completion with 2,000 records.

```properties
badapi.random-error.failure-rate=0.35
badapi.rate-limit.requests-per-minute=30
badapi.chaos.random-slowness-enabled=true
badapi.chaos.slowness-probability=0.3
badapi.chaos.min-delay-seconds=1
badapi.chaos.max-delay-seconds=3
badapi.chaos.timeout-probability=0.05
badapi.chaos.variable-rate-limit=true
badapi.chaos.min-rate-limit=20
badapi.chaos.max-rate-limit=60
badapi.pagination.page-size=100
```

**Challenge time**: ~2 minutes for optimal resilient code, 5-15 minutes for first-timers

### Nightmare Mode (For Experts)
Maximum chaos - for senior engineers who think they've seen it all.

```properties
badapi.random-error.failure-rate=0.5
badapi.chaos.slowness-probability=0.6
badapi.chaos.min-delay-seconds=10
badapi.chaos.max-delay-seconds=45
badapi.chaos.timeout-probability=0.25
badapi.chaos.min-rate-limit=3
badapi.chaos.max-rate-limit=50
```

**Challenge time**: 1-2 hours+ even for experts

## Running the Challenge

### Before the Presentation

1. **Test the API** with the solution scripts to verify everything works
2. **Reset the leaderboard**:
   ```bash
   curl -X POST "http://localhost:8080/api/reset?adminKey=reset123"
   ```
3. **Choose your difficulty level** and restart the API
4. **Prepare handouts** - print or share the landing page URL

### During the Presentation

1. **Introduction** (5 min):
   - Explain the premise: Bad API that needs resilient code
   - Show the web interface
   - Explain the CSV format and validation

2. **Demo the Problems** (5 min):
   - Make a few API calls in browser/Postman to show:
     - Random errors
     - Rate limiting
     - Slow responses
   - Show the Swagger docs

3. **Set Time Limit**:
   - Easy Mode: 30 minutes
   - Hard Mode: 60 minutes
   - Nightmare Mode: 90 minutes

4. **Monitor Progress**:
   - Watch the leaderboard at http://localhost:8080/leaderboard.html
   - Keep an eye on the console logs for chaos mode triggers

### After Someone Wins

1. **Show the solution code** (solution.js or solution.py)
2. **Discuss the patterns**:
   - Retry logic with exponential backoff
   - Timeout handling
   - Rate limit adaptation
   - Data consistency checks
3. **Show real-world examples** where these patterns are needed

## Tips for Presenting

### Make It Engaging

- **Live leaderboard**: Display it on a big screen
- **Commentary**: Call out when someone hits the GTFO page (<10% correct)
- **Hints**: Drop hints every 10-15 minutes if people are struggling

### Common Hints to Give

**After 10 minutes**:
"Have you implemented timeout handling? Without it, you'll hang forever!"

**After 20 minutes**:
"The rate limits are not what they seem... they change randomly! You need adaptive logic."

**After 30 minutes**:
"Remember: All three chaos modes are active! You need timeouts, adaptive rate limiting, AND retry logic!"

### Things to Watch For

1. **People getting stuck on timeouts**: They'll see their code hanging
   - Hint: "Set a timeout on your HTTP requests!"

2. **Rate limit frustration**: They think they figured it out, then it changes
   - Hint: "The rate limit isn't constant... track what actually works!"

3. **Getting partway through then timing out**: Losing progress
   - Hint: "Store your partial results! Don't start from scratch on every failure!"

## Configuration Reference

### All Available Settings

```properties
# Basic Behaviors
badapi.random-error.failure-rate=0.3          # 0.0 to 1.0
badapi.rate-limit.requests-per-minute=50      # Base rate limit
badapi.rate-limit.window-seconds=60           # Rate limit window
badapi.pagination.page-size=50                # Records per page

# Network Chaos
badapi.chaos.random-slowness-enabled=true     # Enable/disable slowness
badapi.chaos.slowness-probability=0.4         # 0.0 to 1.0
badapi.chaos.min-delay-seconds=5              # Minimum delay
badapi.chaos.max-delay-seconds=30             # Maximum delay
badapi.chaos.timeout-probability=0.15         # 0.0 to 1.0

# Variable Rate Limiting
badapi.chaos.variable-rate-limit=true         # Enable/disable
badapi.chaos.min-rate-limit=5                 # Minimum rate
badapi.chaos.max-rate-limit=100               # Maximum rate
```

## Troubleshooting

### API Won't Start
- Check port 8080 is available: `lsof -i :8080`
- Check Java version: `java -version` (needs Java 17+)
- Check Maven: `mvn -version`

### Solution Scripts Fail
- **JavaScript**: Requires Node 18+ with native fetch
- **Python**: Requires Python 3.7+ and `requests` library
  ```bash
  pip install requests
  ```

### Leaderboard Not Updating
- Refresh the page (auto-refresh is every 10 seconds)
- Check API logs for errors
- Verify submissions with: `curl http://localhost:8080/api/stats`

### Challenge Too Easy/Hard
- Adjust probabilities in `application.properties`
- Restart the API: `mvn spring-boot:run`
- Higher probabilities = harder challenge

## Advanced: Custom Chaos Scenarios

Want to create a specific scenario? Here are some ideas:

**The Timeout Trap** (Forces timeout handling):
```properties
badapi.chaos.timeout-probability=0.5
badapi.chaos.random-slowness-enabled=false
badapi.random-error.failure-rate=0.1
```

**The Rate Limit Roller Coaster**:
```properties
badapi.chaos.variable-rate-limit=true
badapi.chaos.min-rate-limit=1
badapi.chaos.max-rate-limit=10
```

**The Slowpoke** (Forces patience and proper timeout handling):
```properties
badapi.chaos.random-slowness-enabled=true
badapi.chaos.slowness-probability=0.8
badapi.chaos.min-delay-seconds=15
badapi.chaos.max-delay-seconds=45
badapi.chaos.timeout-probability=0.3
badapi.random-error.failure-rate=0.1
```

## Cleanup

After the challenge:

1. **Save the leaderboard results** (screenshot or curl the API)
2. **Reset for next time**:
   ```bash
   curl -X POST "http://localhost:8080/api/reset?adminKey=reset123"
   ```
3. **Share the solution** files with participants

---

**Remember**: The goal is to teach, not to torture. If everyone is stuck after 30 minutes, consider dropping the difficulty or providing more hints!

Good luck with your presentation! 🚀

