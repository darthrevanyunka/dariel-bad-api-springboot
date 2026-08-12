/**
 * Bad API Challenge - Reference Solution
 * 
 * This solution demonstrates resilient programming patterns:
 * - Retry logic with exponential backoff
 * - Rate limit handling
 * - Cursor-based pagination
 * - Concurrent request management
 * - Error handling and recovery
 */

const fs = require('fs');

// Configuration
const API_BASE_URL = 'http://localhost:8080/api';
const MAX_RETRIES = 5;
const INITIAL_BACKOFF_MS = 1000;
const MAX_CONCURRENT_REQUESTS = 10;
// CRITICAL: the API simulates a "hung" request by sleeping ~2 minutes server-side
// before ever returning 408. Without a client-side timeout shorter than that,
// every timed-out request stalls the whole run for 2 minutes.
const REQUEST_TIMEOUT_MS = 10000;

// Helper function: Sleep/delay
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Fetch with retry logic, exponential backoff, and a client-side timeout
 */
async function fetchWithRetry(url, options = {}, retries = 0) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    try {
        const response = await fetch(url, { ...options, signal: controller.signal });
        clearTimeout(timeoutId);

        // Success!
        if (response.ok) {
            return response;
        }

        // Handle rate limiting (429)
        if (response.status === 429) {
            const retryAfter = response.headers.get('Retry-After') || 60;
            console.log(`⏳ Rate limited. Waiting ${retryAfter}s...`);
            await sleep(parseInt(retryAfter) * 1000);
            return fetchWithRetry(url, options, retries); // Retry without incrementing counter
        }

        // Handle server errors (5xx) and request timeouts (408) with exponential backoff
        if (response.status >= 500 || response.status === 408) {
            if (retries < MAX_RETRIES) {
                const backoffTime = INITIAL_BACKOFF_MS * Math.pow(2, retries);
                console.log(`❌ Server error ${response.status}. Retrying in ${backoffTime}ms... (attempt ${retries + 1}/${MAX_RETRIES})`);
                await sleep(backoffTime);
                return fetchWithRetry(url, options, retries + 1);
            }
            throw new Error(`Max retries exceeded. Last status: ${response.status}`);
        }

        // Other errors
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);

    } catch (error) {
        clearTimeout(timeoutId);

        // Client-side timeout: the request took too long, likely a simulated hang
        if (error.name === 'AbortError') {
            if (retries < MAX_RETRIES) {
                const backoffTime = INITIAL_BACKOFF_MS * Math.pow(2, retries);
                console.log(`⏱️  Request timed out after ${REQUEST_TIMEOUT_MS}ms. Retrying in ${backoffTime}ms... (attempt ${retries + 1}/${MAX_RETRIES})`);
                await sleep(backoffTime);
                return fetchWithRetry(url, options, retries + 1);
            }
            throw new Error('Max retries exceeded: request kept timing out');
        }

        // Network errors, etc.
        if (retries < MAX_RETRIES && (error.name === 'TypeError' || error.code === 'ECONNREFUSED')) {
            const backoffTime = INITIAL_BACKOFF_MS * Math.pow(2, retries);
            console.log(`🔌 Network error: ${error.message}. Retrying in ${backoffTime}ms... (attempt ${retries + 1}/${MAX_RETRIES})`);
            await sleep(backoffTime);
            return fetchWithRetry(url, options, retries + 1);
        }
        throw error;
    }
}

/**
 * Fetch all first names
 */
async function fetchFirstNames() {
    console.log('\n📥 Fetching first names...');
    const response = await fetchWithRetry(`${API_BASE_URL}/people/firstnames`);
    const data = await response.json();
    console.log(`✅ Got ${data.data.length} first names`);
    
    // Convert to map for easy lookup
    const map = new Map();
    data.data.forEach(item => {
        map.set(item.id, item.firstName);
    });
    return map;
}

/**
 * Fetch all surnames with rate limit handling
 */
async function fetchSurnames() {
    console.log('\n📥 Fetching surnames...');
    const response = await fetchWithRetry(`${API_BASE_URL}/people/surnames`);
    const data = await response.json();
    console.log(`✅ Got ${data.data.length} surnames`);
    
    // Convert to map for easy lookup
    const map = new Map();
    data.data.forEach(item => {
        map.set(item.id, item.surname);
    });
    return map;
}

/**
 * Fetch all ages with cursor pagination
 */
async function fetchAges() {
    console.log('\n📥 Fetching ages (paginated)...');
    const allAges = new Map();
    let cursor = null;
    let pageCount = 0;
    
    do {
        const url = cursor 
            ? `${API_BASE_URL}/people/ages?cursor=${cursor}`
            : `${API_BASE_URL}/people/ages`;
        
        const response = await fetchWithRetry(url);
        const data = await response.json();
        
        pageCount++;
        console.log(`  📄 Page ${pageCount}: Got ${data.data.length} records (cursor: ${cursor || 'start'})`);
        
        // Add to map
        data.data.forEach(item => {
            allAges.set(item.id, item.age);
        });
        
        cursor = data.cursor;
        
        // Small delay between pages to be nice to the API
        if (cursor) {
            await sleep(100);
        }
        
    } while (cursor !== null);
    
    console.log(`✅ Got ${allAges.size} ages total from ${pageCount} pages`);
    return allAges;
}

/**
 * Combine all data and generate CSV
 */
function generateCSV(firstNames, surnames, ages) {
    console.log('\n🔨 Generating CSV...');
    
    const rows = ['firstName,surname,age,computedValue'];
    let validCount = 0;
    
    // Get all IDs (should be 1-2000)
    const allIds = new Set([
        ...firstNames.keys(),
        ...surnames.keys(),
        ...ages.keys()
    ]);
    
    console.log(`  Total unique IDs: ${allIds.size}`);
    
    // Sort IDs for consistent output
    const sortedIds = Array.from(allIds).sort((a, b) => a - b);
    
    for (const id of sortedIds) {
        const firstName = firstNames.get(id);
        const surname = surnames.get(id);
        const age = ages.get(id);
        
        // Validate we have all data
        if (firstName && surname && age !== undefined) {
            const computedValue = `${firstName}${age}${surname}`;
            rows.push(`${firstName},${surname},${age},${computedValue}`);
            validCount++;
        } else {
            console.warn(`  ⚠️  Missing data for ID ${id}: firstName=${firstName}, surname=${surname}, age=${age}`);
        }
    }
    
    console.log(`✅ Generated ${validCount} valid records`);
    return rows.join('\n');
}

/**
 * Submit the solution
 */
async function submitSolution(participantName, csvContent) {
    console.log(`\n📤 Submitting solution for: ${participantName}`);
    
    const formData = new URLSearchParams();
    formData.append('participantName', participantName);
    formData.append('csvContent', csvContent);
    
    const response = await fetch(`${API_BASE_URL}/submit`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: formData.toString()
    });
    
    const result = await response.json();
    
    console.log('\n' + '='.repeat(60));
    if (result.success) {
        console.log('🎉 SUCCESS! 🎉');
        console.log(`✅ ${result.message}`);
        console.log(`📊 Correct Records: ${result.submission.correctRecords}/${result.submission.totalRecords}`);
        console.log(`⏰ Submitted at: ${result.submission.submittedAt}`);
    } else {
        console.log('❌ VALIDATION FAILED');
        console.log(`📊 Correct Records: ${result.submission.correctRecords}/${result.submission.totalRecords}`);
        console.log(`💡 Message: ${result.message}`);
        
        if (result.redirectToMeme) {
            console.log('🚫 Less than 10% correct - you would be redirected to the GTFO page!');
        }
    }
    console.log('='.repeat(60) + '\n');
    
    return result;
}

/**
 * Main execution
 */
async function main() {
    const startTime = Date.now();
    
    console.log('🚀 Bad API Challenge - Solution Starting...');
    console.log('='.repeat(60));
    
    try {
        // Step 1: Fetch all data in parallel (where possible)
        console.log('\n📡 Step 1: Fetching data from all endpoints...');
        
        // Fetch first names and surnames in parallel
        // Ages must be fetched sequentially due to pagination
        const [firstNames, surnames] = await Promise.all([
            fetchFirstNames(),
            fetchSurnames()
        ]);
        
        // Fetch ages separately (pagination required)
        const ages = await fetchAges();
        
        // Step 2: Validate we have all data
        console.log('\n✔️  Step 2: Validating data...');
        console.log(`  First names: ${firstNames.size}`);
        console.log(`  Surnames: ${surnames.size}`);
        console.log(`  Ages: ${ages.size}`);
        
        if (firstNames.size !== 2000 || surnames.size !== 2000 || ages.size !== 2000) {
            throw new Error('Incomplete data! Expected 2000 records from each endpoint.');
        }
        
        // Step 3: Generate CSV
        console.log('\n✔️  Step 3: Generating CSV...');
        const csvContent = generateCSV(firstNames, surnames, ages);
        
        // Save to file
        const filename = 'solution_output.csv';
        fs.writeFileSync(filename, csvContent);
        console.log(`💾 Saved CSV to: ${filename}`);
        
        // Step 4: Submit
        const participantName = process.argv[2] || 'Solution Bot';
        console.log(`\n✔️  Step 4: Submitting solution...`);
        const result = await submitSolution(participantName, csvContent);
        
        // Summary
        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`⏱️  Total time: ${duration}s`);
        
        if (result.success) {
            console.log('\n🏆 Challenge completed successfully!');
            process.exit(0);
        } else {
            console.log('\n⚠️  Solution validation failed. Check the output above.');
            process.exit(1);
        }
        
    } catch (error) {
        console.error('\n💥 Fatal error:', error.message);
        console.error(error);
        process.exit(1);
    }
}

// Run if called directly
if (require.main === module) {
    // Check for node-fetch availability
    if (typeof fetch === 'undefined') {
        console.log('⚠️  This script requires Node.js 18+ with native fetch, or you need to install node-fetch');
        console.log('   For older Node versions, run: npm install node-fetch@2');
        console.log('   Then add at top of file: const fetch = require("node-fetch");');
        process.exit(1);
    }
    
    main();
}

module.exports = { fetchWithRetry, fetchFirstNames, fetchSurnames, fetchAges, generateCSV };

