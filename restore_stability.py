import os, subprocess, re, glob

# List of admin files to restore
files = ['dashboard.html', 'analytics.html', 'users.html', 'leaderboard.html', 'notifications.html', 'settings.html']
railway_api = 'https://repm-project-production.up.railway.app'

for f_name in files:
    path = f'frontend/admin/{f_name}'
    try:
        # Get content from commit 11ff946 (the last user-approved stable state)
        content = subprocess.check_output(['git', 'show', f'11ff946:{path}'], encoding='utf-8')
        
        # Replace localhost with Railway API
        content = content.replace('http://localhost:8080', railway_api)
        
        # Ensure credentials: 'include' is present in all fetch calls
        # Find fetch calls and inject credentials if missing
        content = re.sub(r"fetch\((['\"`][^'\"`]+['\"`])\s*,?\s*\{", r"fetch(\1, { credentials: 'include',", content)
        # Handle simple fetch calls without options
        content = re.sub(r"fetch\((['\"`][^'\"`]+['\"`])\)(?!\s*\{)", r"fetch(\1, { credentials: 'include' })", content)
        
        # Write back
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Restored {f_name}")
    except Exception as e:
        print(f"Failed to restore {f_name}: {e}")

# Also update user files to use Railway API
user_files = glob.glob('frontend/user/*.html')
for path in user_files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Update API constant
    content = re.sub(r"(const API\s*=\s*)(['\"`][^'\"`]*['\"`]);", f"\\1'{railway_api}';", content)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated {path}")
