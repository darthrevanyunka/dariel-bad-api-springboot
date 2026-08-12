# Presentation Notes - Bad API Challenge

## Introduction (5 minutes)

### Hook
"Who here has ever worked with a production API that just... sucked? Random timeouts, weird rate limits, inconsistent behavior? Today, you're going to learn how to deal with that - by building a solution against an API that's *deliberately terrible*."

### The Premise
- This is a **competitive coding challenge**
- The API is intentionally unreliable (like a real overloaded production system)
- Your job: collect data for 2,000 people from 3 endpoints, generate a CSV, submit it
- First correct submission wins! 🏆
- Expected completion time with good code: ~2 minutes

### What Makes It Hard
Show this slide/screen:
```
😈 The Bad API Features:
✓ Random 500/503 errors (~35% of requests)
✓ Random slowness (1-3 second delays on 30% of requests)
✓ Random timeouts (5% never respond at all!)
✓ Variable rate limiting (changes randomly: 20-60 req/min)
✓ Cursor-based pagination (because why make it easy?)
```

## Demo (5 minutes)

### Show the Web Interface
1. Open http://localhost:8080
2. Walk through the landing page
3. Show the documentation page
4. Show the Swagger UI

### Live API Demo
Open Postman/curl and demonstrate:

```bash
# Show first names - might work, might error
curl http://localhost:8080/api/people/firstnames

# Try again if it errored - show randomness
curl http://localhost:8080/api/people/firstnames

# Show rate limiting on surnames
for i in {1..10}; do
  curl http://localhost:8080/api/people/surnames
  echo "Request $i"
done

# Show pagination on ages
curl "http://localhost:8080/api/people/ages"
curl "http://localhost:8080/api/people/ages?cursor=50"
```

**Point out**: "See those errors? That slowness? You need to handle ALL of this!"

## The Challenge (Give Instructions)

### The Goal
"Collect all 2,000 people from the three endpoints, compute a value, and submit a CSV. With optimal resilient code, this should take about 2 minutes."

### Show Expected Format
```csv
firstName,surname,age,computedValue
John,Smith,25,John25Smith
Mary,Johnson,30,Mary30Johnson
```

### Computed Value Formula
**Write this on a board/screen:** `computedValue = firstName + age + surname`

### Time Limit
- **Easy Mode**: 30 minutes (if you configured easy mode)
- **Hard Mode**: 60 minutes (default recommended)
- **Nightmare Mode**: 90 minutes

### Rules
1. Any programming language is fine
2. You can use any libraries you want
3. Use the web form or POST directly to `/api/submit`
4. Leaderboard shows who finished when
5. If you submit with <10% correct... you'll see something special 😈

## During the Challenge

### Monitor & Commentary
- Keep the leaderboard visible on a big screen
- Call out interesting events:
  - "Looks like someone just hit the GTFO page!" (< 10% correct)
  - "We have our first submission! But it's only 1,743/2,000..."
  - "Ooh, someone's getting rate limited hard right now!"

### Hint Schedule

**10 minutes in**:
> "Quick hint: If your code is hanging, you probably didn't set timeouts on your HTTP requests!"

**20 minutes in**:
> "The rate limits aren't constant. They change randomly PER CLIENT. You need adaptive logic!"

**35 minutes in**:
> "Pro tip: Store your progress! Don't restart from scratch every time something fails."

**45 minutes in** (if no one has won):
> "All three chaos modes are active simultaneously. Your code needs to handle errors, timeouts, AND rate limits at the same time."

## When Someone Wins

### Celebrate! 🎉
1. Show their submission on the leaderboard
2. Ask them to briefly share their approach
3. Show your solution code

### Code Review (10-15 minutes)

Walk through the solution.js or solution.py:

#### 1. Retry Logic with Exponential Backoff
```javascript
// Point out the exponential backoff
const backoffTime = INITIAL_BACKOFF_MS * Math.pow(2, retries);
```
**Explain**: "This is crucial for handling random errors. Each retry waits longer."

#### 2. Timeout Handling
```javascript
// Show the timeout configuration
const controller = new AbortController();
const timeoutId = setTimeout(() => controller.abort(), 60000);
```
**Explain**: "Without this, 5% of your requests hang FOREVER. You MUST set timeouts."

#### 3. Rate Limit Handling
```javascript
if (response.status === 429) {
  const retryAfter = response.headers.get('Retry-After');
  await sleep(parseInt(retryAfter) * 1000);
  return fetchWithRetry(url, options, retries);
}
```
**Explain**: "Respect the Retry-After header. And since limits change, don't give up after one 429!"

#### 4. Pagination Loop
```javascript
do {
  const url = cursor ? `${API_BASE_URL}/people/ages?cursor=${cursor}` : `${API_BASE_URL}/people/ages`;
  const response = await fetchWithRetry(url);
  const data = await response.json();
  allAges.push(...data.data);
  cursor = data.cursor;
} while (cursor !== null);
```
**Explain**: "Keep fetching until cursor is null. Each page might fail - that's why we use fetchWithRetry."

### Real-World Applications

**Ask the audience**: "Where have you seen these patterns needed?"

**Examples to mention**:
- **Microservices**: When service A calls service B, service B might be overloaded
- **Third-party APIs**: Stripe, Twilio, etc. - they all rate limit and can fail
- **Cloud services**: AWS, Azure, GCP - they recommend exponential backoff
- **Distributed systems**: Network partitions, cascading failures
- **Mobile apps**: Poor network conditions, intermittent connectivity

## Key Takeaways (5 minutes)

### The Patterns You Learned Today

1. **Retry Logic with Exponential Backoff**
   - Don't give up on first failure
   - Wait longer between retries (exponential)
   - Have a maximum retry count

2. **Timeout Configuration**
   - ALWAYS set timeouts on HTTP requests
   - Balance between too short (false failures) and too long (waste time)
   - 60 seconds is often a good default

3. **Rate Limit Handling**
   - Check for 429 status codes
   - Respect Retry-After headers
   - Consider adaptive rate limiting (tracking what works)

4. **Circuit Breaker Pattern** (bonus)
   - Stop calling a failing service
   - Give it time to recover
   - Try again after a cooldown period

### Libraries That Help

**Mention these tools**:
- **JavaScript**: axios-retry, p-retry, async-retry
- **Python**: tenacity, backoff, retrying
- **Java**: Resilience4j, Hystrix
- **Go**: go-retryablehttp

### The Big Picture

**End with this message**:
> "In production, you can't control what other services do. You CAN control how your service responds to failures. These resilience patterns are the difference between a system that falls over when something goes wrong, and one that gracefully handles problems and keeps working."

## Q&A

Common questions and answers:

**Q**: "Is this realistic?"
**A**: "Absolutely! Cloud services recommend exponential backoff. Rate limiting is everywhere. Network timeouts are reality. This is just concentrated into one challenge."

**Q**: "What if I didn't finish?"
**A**: "That's okay! The point is learning the patterns. Take the solution code, study it, and try implementing these patterns in your next project."

**Q**: "What's the hardest part?"
**A**: "Usually the timeouts. People forget to set them, then their code hangs forever on 5% of requests. Second hardest: variable rate limiting - you can't just assume one limit works for the whole session."

## Closing

"Thanks for participating! The code is available at [repo URL]. Try running it locally, tweak the chaos settings, and experiment with different resilience patterns. And next time you're integrating with a flaky API... you'll know what to do! 🚀"

---

## Technical Setup Checklist

Before presentation:

- [ ] API is running on port 8080
- [ ] Reset the leaderboard (`curl -X POST "http://localhost:8080/api/reset?adminKey=reset123"`)
- [ ] Test solution script works
- [ ] Difficulty level is configured correctly
- [ ] Projector/screen sharing works for leaderboard
- [ ] Have Postman/curl ready for demo
- [ ] Print or share URL: http://localhost:8080

During presentation:
- [ ] Leaderboard visible throughout
- [ ] Monitor API console for interesting logs
- [ ] Have solution code ready to show
- [ ] Keep time and give hints as needed

After presentation:
- [ ] Share solution code with participants
- [ ] Share slides/notes
- [ ] Get feedback on difficulty level for next time

