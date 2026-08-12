#!/usr/bin/env python3
"""
Bad API Challenge - Reference Solution (Python)

This solution demonstrates resilient programming patterns:
- Retry logic with exponential backoff
- Rate limit handling
- Cursor-based pagination
- Error handling and recovery
"""

import requests
import time
import csv
import sys
from typing import Dict, Optional

# Configuration
API_BASE_URL = 'http://localhost:8080/api'
MAX_RETRIES = 5
INITIAL_BACKOFF_MS = 1000

class BadAPIClient:
    def __init__(self, base_url: str = API_BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'BadAPI-Solution/1.0'
        })
    
    def fetch_with_retry(self, url: str, retries: int = 0) -> requests.Response:
        """Fetch URL with retry logic and exponential backoff"""
        try:
            response = self.session.get(url, timeout=10)
            
            # Success!
            if response.status_code == 200:
                return response
            
            # Handle rate limiting (429)
            if response.status_code == 429:
                retry_after = int(response.headers.get('Retry-After', 60))
                print(f"⏳ Rate limited. Waiting {retry_after}s...")
                time.sleep(retry_after)
                return self.fetch_with_retry(url, retries)  # Retry without incrementing
            
            # Handle server errors (5xx) with exponential backoff
            if response.status_code >= 500:
                if retries < MAX_RETRIES:
                    backoff_time = (INITIAL_BACKOFF_MS / 1000) * (2 ** retries)
                    print(f"❌ Server error {response.status_code}. "
                          f"Retrying in {backoff_time:.1f}s... "
                          f"(attempt {retries + 1}/{MAX_RETRIES})")
                    time.sleep(backoff_time)
                    return self.fetch_with_retry(url, retries + 1)
                raise Exception(f"Max retries exceeded. Last status: {response.status_code}")
            
            # Other errors
            raise Exception(f"HTTP {response.status_code}: {response.reason}")
            
        except (requests.ConnectionError, requests.Timeout) as e:
            # Network errors
            if retries < MAX_RETRIES:
                backoff_time = (INITIAL_BACKOFF_MS / 1000) * (2 ** retries)
                print(f"🔌 Network error: {e}. "
                      f"Retrying in {backoff_time:.1f}s... "
                      f"(attempt {retries + 1}/{MAX_RETRIES})")
                time.sleep(backoff_time)
                return self.fetch_with_retry(url, retries + 1)
            raise
    
    def fetch_first_names(self) -> Dict[int, str]:
        """Fetch all first names"""
        print('\n📥 Fetching first names...')
        url = f'{self.base_url}/people/firstnames'
        response = self.fetch_with_retry(url)
        data = response.json()
        
        first_names = {item['id']: item['firstName'] for item in data['data']}
        print(f"✅ Got {len(first_names)} first names")
        return first_names
    
    def fetch_surnames(self) -> Dict[int, str]:
        """Fetch all surnames with rate limit handling"""
        print('\n📥 Fetching surnames...')
        url = f'{self.base_url}/people/surnames'
        response = self.fetch_with_retry(url)
        data = response.json()
        
        surnames = {item['id']: item['surname'] for item in data['data']}
        print(f"✅ Got {len(surnames)} surnames")
        return surnames
    
    def fetch_ages(self) -> Dict[int, int]:
        """Fetch all ages with cursor pagination"""
        print('\n📥 Fetching ages (paginated)...')
        all_ages = {}
        cursor = None
        page_count = 0
        
        while True:
            url = f'{self.base_url}/people/ages'
            if cursor:
                url += f'?cursor={cursor}'
            
            response = self.fetch_with_retry(url)
            data = response.json()
            
            page_count += 1
            print(f"  📄 Page {page_count}: Got {len(data['data'])} records "
                  f"(cursor: {cursor or 'start'})")
            
            # Add to dictionary
            for item in data['data']:
                all_ages[item['id']] = item['age']
            
            cursor = data.get('cursor')
            if not cursor:
                break
            
            # Small delay between pages
            time.sleep(0.1)
        
        print(f"✅ Got {len(all_ages)} ages total from {page_count} pages")
        return all_ages
    
    def submit_solution(self, participant_name: str, csv_content: str) -> dict:
        """Submit the solution"""
        print(f"\n📤 Submitting solution for: {participant_name}")
        
        url = f'{self.base_url}/submit'
        data = {
            'participantName': participant_name,
            'csvContent': csv_content
        }
        
        response = self.session.post(url, data=data, timeout=30)
        result = response.json()
        
        print('\n' + '=' * 60)
        if result.get('success'):
            print('🎉 SUCCESS! 🎉')
            print(f"✅ {result['message']}")
            submission = result['submission']
            print(f"📊 Correct Records: {submission['correctRecords']}/{submission['totalRecords']}")
            print(f"⏰ Submitted at: {submission['submittedAt']}")
        else:
            print('❌ VALIDATION FAILED')
            submission = result['submission']
            print(f"📊 Correct Records: {submission['correctRecords']}/{submission['totalRecords']}")
            print(f"💡 Message: {result['message']}")
            
            if result.get('redirectToMeme'):
                print('🚫 Less than 10% correct - you would be redirected to the GTFO page!')
        print('=' * 60 + '\n')
        
        return result


def generate_csv(first_names: Dict[int, str], 
                surnames: Dict[int, str], 
                ages: Dict[int, int]) -> str:
    """Combine all data and generate CSV"""
    print('\n🔨 Generating CSV...')
    
    # Get all unique IDs
    all_ids = set(first_names.keys()) | set(surnames.keys()) | set(ages.keys())
    print(f"  Total unique IDs: {len(all_ids)}")
    
    # Sort IDs for consistent output
    sorted_ids = sorted(all_ids)
    
    rows = []
    valid_count = 0
    
    for person_id in sorted_ids:
        first_name = first_names.get(person_id)
        surname = surnames.get(person_id)
        age = ages.get(person_id)
        
        # Validate we have all data
        if first_name and surname and age is not None:
            computed_value = f"{first_name}{age}{surname}"
            rows.append({
                'firstName': first_name,
                'surname': surname,
                'age': age,
                'computedValue': computed_value
            })
            valid_count += 1
        else:
            print(f"  ⚠️  Missing data for ID {person_id}: "
                  f"firstName={first_name}, surname={surname}, age={age}")
    
    print(f"✅ Generated {valid_count} valid records")
    
    # Convert to CSV string
    output = 'firstName,surname,age,computedValue\n'
    for row in rows:
        output += f"{row['firstName']},{row['surname']},{row['age']},{row['computedValue']}\n"
    
    return output


def save_csv_file(csv_content: str, filename: str = 'solution_output.csv'):
    """Save CSV content to file"""
    with open(filename, 'w') as f:
        f.write(csv_content)
    print(f"💾 Saved CSV to: {filename}")


def main():
    """Main execution"""
    start_time = time.time()
    
    print('🚀 Bad API Challenge - Solution Starting...')
    print('=' * 60)
    
    try:
        client = BadAPIClient()
        
        # Step 1: Fetch all data
        print('\n📡 Step 1: Fetching data from all endpoints...')
        first_names = client.fetch_first_names()
        surnames = client.fetch_surnames()
        ages = client.fetch_ages()
        
        # Step 2: Validate we have all data
        print('\n✔️  Step 2: Validating data...')
        print(f"  First names: {len(first_names)}")
        print(f"  Surnames: {len(surnames)}")
        print(f"  Ages: {len(ages)}")
        
        if len(first_names) != 2000 or len(surnames) != 2000 or len(ages) != 2000:
            raise Exception('Incomplete data! Expected 2000 records from each endpoint.')
        
        # Step 3: Generate CSV
        print('\n✔️  Step 3: Generating CSV...')
        csv_content = generate_csv(first_names, surnames, ages)
        
        # Save to file
        filename = 'solution_output.csv'
        save_csv_file(csv_content, filename)
        
        # Step 4: Submit
        participant_name = sys.argv[1] if len(sys.argv) > 1 else 'Solution Bot (Python)'
        print(f'\n✔️  Step 4: Submitting solution...')
        result = client.submit_solution(participant_name, csv_content)
        
        # Summary
        duration = time.time() - start_time
        print(f"⏱️  Total time: {duration:.2f}s")
        
        if result.get('success'):
            print('\n🏆 Challenge completed successfully!')
            sys.exit(0)
        else:
            print('\n⚠️  Solution validation failed. Check the output above.')
            sys.exit(1)
            
    except Exception as e:
        print(f'\n💥 Fatal error: {e}')
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()

