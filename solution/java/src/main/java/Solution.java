import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Bad API Challenge - Reference Solution (Java)
 *
 * Demonstrates the resilience patterns the challenge is built to teach:
 *   - Client-side request timeouts (java.net.http.HttpClient)
 *   - Retry logic with exponential backoff (5xx and 408)
 *   - Rate limit handling via 429 + Retry-After
 *   - Cursor-based pagination
 *
 * Run:
 *   mvn -q package
 *   java -jar target/solution-java.jar "Your Name"
 */
public class Solution {

    private static final String API_BASE_URL = "http://localhost:8080/api";
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1000;

    // CRITICAL: the API simulates a hung request by sleeping ~2 minutes
    // server-side before it ever returns 408. The client timeout must be
    // well under that, or every timed-out request stalls the whole run.
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int EXPECTED_RECORD_COUNT = 2000;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String participantName = args.length > 0 ? args[0] : "Solution Bot (Java)";

        System.out.println("🚀 Bad API Challenge - Solution Starting...");
        System.out.println("=".repeat(60));

        try {
            System.out.println("\n📡 Step 1: Fetching data from all endpoints...");

            Map<Long, String> firstNames = fetchFirstNames();
            Map<Long, String> surnames = fetchSurnames();
            Map<Long, Integer> ages = fetchAges();

            System.out.println("\n✔️  Step 2: Validating data...");
            System.out.println("  First names: " + firstNames.size());
            System.out.println("  Surnames: " + surnames.size());
            System.out.println("  Ages: " + ages.size());

            if (firstNames.size() != EXPECTED_RECORD_COUNT
                    || surnames.size() != EXPECTED_RECORD_COUNT
                    || ages.size() != EXPECTED_RECORD_COUNT) {
                throw new IllegalStateException(
                        "Incomplete data! Expected " + EXPECTED_RECORD_COUNT + " records from each endpoint.");
            }

            System.out.println("\n✔️  Step 3: Generating CSV...");
            String csv = generateCsv(firstNames, surnames, ages);
            Path outFile = Path.of("solution_output.csv");
            Files.writeString(outFile, csv, StandardCharsets.UTF_8);
            System.out.println("💾 Saved CSV to: " + outFile);

            System.out.println("\n✔️  Step 4: Submitting solution...");
            boolean success = submitSolution(participantName, csv);

            double duration = (System.currentTimeMillis() - start) / 1000.0;
            System.out.printf(Locale.US, "⏱️  Total time: %.2fs%n", duration);

            if (success) {
                System.out.println("\n🏆 Challenge completed successfully!");
                System.exit(0);
            } else {
                System.out.println("\n⚠️  Solution validation failed. Check the output above.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("\n💥 Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Fetch with retry logic, exponential backoff, and a client-side timeout. */
    private static HttpResponse<String> fetchWithRetry(String url) throws IOException, InterruptedException {
        int retries = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response;
            try {
                response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException e) {
                // Client-side timeout: the request took too long, likely a simulated hang
                if (retries >= MAX_RETRIES) {
                    throw new IOException("Max retries exceeded: request kept timing out", e);
                }
                long backoff = INITIAL_BACKOFF_MS * (1L << retries);
                System.out.printf("⏱️  Request timed out after %ds. Retrying in %dms... (attempt %d/%d)%n",
                        REQUEST_TIMEOUT.toSeconds(), backoff, retries + 1, MAX_RETRIES);
                sleep(backoff);
                retries++;
                continue;
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response;
            }

            if (status == 429) {
                long retryAfter = header(response, "Retry-After").map(Long::parseLong).orElse(60L);
                System.out.println("⏳ Rate limited. Waiting " + retryAfter + "s...");
                sleep(retryAfter * 1000);
                continue; // retry without burning an attempt
            }

            if (status >= 500 || status == 408) {
                if (retries >= MAX_RETRIES) {
                    throw new IOException("Max retries exceeded. Last status: " + status);
                }
                long backoff = INITIAL_BACKOFF_MS * (1L << retries);
                System.out.printf("❌ Server error %d. Retrying in %dms... (attempt %d/%d)%n",
                        status, backoff, retries + 1, MAX_RETRIES);
                sleep(backoff);
                retries++;
                continue;
            }

            throw new IOException("HTTP " + status + ": " + response.body());
        }
    }

    private static Map<Long, String> fetchFirstNames() throws IOException, InterruptedException {
        System.out.println("\n📥 Fetching first names...");
        JsonNode data = JSON.readTree(fetchWithRetry(API_BASE_URL + "/people/firstnames").body()).get("data");
        Map<Long, String> result = new LinkedHashMap<>();
        for (JsonNode item : data) {
            result.put(item.get("id").asLong(), item.get("firstName").asText());
        }
        System.out.println("✅ Got " + result.size() + " first names");
        return result;
    }

    private static Map<Long, String> fetchSurnames() throws IOException, InterruptedException {
        System.out.println("\n📥 Fetching surnames...");
        JsonNode data = JSON.readTree(fetchWithRetry(API_BASE_URL + "/people/surnames").body()).get("data");
        Map<Long, String> result = new LinkedHashMap<>();
        for (JsonNode item : data) {
            result.put(item.get("id").asLong(), item.get("surname").asText());
        }
        System.out.println("✅ Got " + result.size() + " surnames");
        return result;
    }

    private static Map<Long, Integer> fetchAges() throws IOException, InterruptedException {
        System.out.println("\n📥 Fetching ages (paginated)...");
        Map<Long, Integer> result = new LinkedHashMap<>();
        String cursor = null;
        int page = 0;

        do {
            String url = cursor == null
                    ? API_BASE_URL + "/people/ages"
                    : API_BASE_URL + "/people/ages?cursor=" + cursor;

            JsonNode root = JSON.readTree(fetchWithRetry(url).body());
            JsonNode data = root.get("data");
            page++;
            System.out.println("  📄 Page " + page + ": Got " + data.size()
                    + " records (cursor: " + (cursor == null ? "start" : cursor) + ")");

            for (JsonNode item : data) {
                result.put(item.get("id").asLong(), item.get("age").asInt());
            }

            boolean hasMore = root.get("hasMore").asBoolean();
            cursor = hasMore ? root.get("cursor").asText() : null;

            if (cursor != null) {
                sleep(100);
            }
        } while (cursor != null);

        System.out.println("✅ Got " + result.size() + " ages total from " + page + " pages");
        return result;
    }

    private static String generateCsv(Map<Long, String> firstNames, Map<Long, String> surnames, Map<Long, Integer> ages) {
        System.out.println("\n🔨 Generating CSV...");

        TreeMap<Long, Boolean> sortedIds = new TreeMap<>();
        firstNames.keySet().forEach(id -> sortedIds.put(id, true));
        surnames.keySet().forEach(id -> sortedIds.put(id, true));
        ages.keySet().forEach(id -> sortedIds.put(id, true));
        System.out.println("  Total unique IDs: " + sortedIds.size());

        StringBuilder csv = new StringBuilder("firstName,surname,age,computedValue\n");
        int valid = 0;
        for (Long id : sortedIds.keySet()) {
            String firstName = firstNames.get(id);
            String surname = surnames.get(id);
            Integer age = ages.get(id);
            if (firstName != null && surname != null && age != null) {
                String computedValue = firstName + age + surname;
                csv.append(firstName).append(',').append(surname).append(',').append(age).append(',').append(computedValue).append('\n');
                valid++;
            } else {
                System.out.printf("  ⚠️  Missing data for ID %d: firstName=%s, surname=%s, age=%s%n",
                        id, firstName, surname, age);
            }
        }
        System.out.println("✅ Generated " + valid + " valid records");
        return csv.toString();
    }

    private static boolean submitSolution(String participantName, String csvContent) throws IOException, InterruptedException {
        System.out.println("\n📤 Submitting solution for: " + participantName);

        String form = "participantName=" + urlEncode(participantName) + "&csvContent=" + urlEncode(csvContent);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE_URL + "/submit"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode result = JSON.readTree(response.body());
        JsonNode submission = result.get("submission");
        boolean success = result.get("success").asBoolean();

        System.out.println("\n" + "=".repeat(60));
        if (success) {
            System.out.println("🎉 SUCCESS! 🎉");
            System.out.println("✅ " + result.get("message").asText());
            System.out.println("📊 Correct Records: " + submission.get("correctRecords").asInt()
                    + "/" + submission.get("totalRecords").asInt());
            System.out.println("⏰ Submitted at: " + submission.get("submittedAt").asText());
        } else {
            System.out.println("❌ VALIDATION FAILED");
            System.out.println("📊 Correct Records: " + submission.get("correctRecords").asInt()
                    + "/" + submission.get("totalRecords").asInt());
            System.out.println("💡 Message: " + result.get("message").asText());
            if (result.path("redirectToMeme").asBoolean(false)) {
                System.out.println("🚫 Less than 10% correct - you would be redirected to the GTFO page!");
            }
        }
        System.out.println("=".repeat(60) + "\n");
        return success;
    }

    private static Optional<String> header(HttpResponse<String> response, String name) {
        return response.headers().firstValue(name);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
