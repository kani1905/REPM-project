import os, glob, re

# === FIX USER PORTAL ===
with open('frontend/user/dashboard.html', 'r', encoding='utf-8') as f:
    dash = f.read()

# Extract profile right side
header_match = re.search(r'<div class="flex items-center gap-4">.*?</header>', dash, re.DOTALL)
header_right = header_match.group(0) if header_match else ''
header_right = re.sub(r'<a href="input.html".*?</a>', '', header_right, flags=re.DOTALL)
header_right = re.sub(r'<div class="flex items-center gap-2 bg-gray-100.*?</button>\s*</div>', '', header_right, flags=re.DOTALL)

for file in glob.glob('frontend/user/*.html'):
    if 'dashboard.html' in file or 'analytics.html' in file: continue
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace header to include profile
    content = re.sub(r'<header class="bg-white dark:bg-gray-800 border-b dark:border-gray-700 px-8 py-4 shadow-sm flex-shrink-0">',
                     r'<header class="bg-white dark:bg-gray-800 border-b dark:border-gray-700 px-8 py-4 flex items-center justify-between shadow-sm flex-shrink-0">\n<div>', content)
    content = re.sub(r'</header>', r'</div>\n' + header_right, content)
    
    if 'toggleDropdown' not in content:
        js = """
        function toggleDropdown() { document.getElementById("dropdownMenu").classList.toggle("hidden"); }
        window.onclick = function(e) { if (!e.target.closest('.relative')) document.getElementById("dropdownMenu")?.classList.add("hidden"); };
        """
        content = content.replace('async function logout', js + '\n        async function logout')
    
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print("User portal fixed.")

# === CONVERT ADMIN PORTAL TO TAILWIND ===

def read_user_file_as_template(filename):
    with open('frontend/user/' + filename, 'r', encoding='utf-8') as f:
        return f.read()

def write_admin_file(filename, content):
    with open('frontend/admin/' + filename, 'w', encoding='utf-8') as f:
        f.write(content)

# Admin Dashboard (built from User Dashboard)
user_dash = read_user_file_as_template('dashboard.html')
# We need to adapt headers and analytics logic, but the user is requesting standard styling.
# Generating Admin copies by directly substituting user dashboard / analytics strings:
admin_dash = user_dash.replace('user/dashboard', 'admin/dashboard')
admin_dash = admin_dash.replace('Dashboard - REPM', 'Admin Dashboard - REPM')
admin_dash = admin_dash.replace('id="headerUsername" class="text-blue-600">User</span>', 'id="headerUsername" class="text-blue-600">Admin</span>')
admin_dash = admin_dash.replace('/user/dashboard', '/admin/dashboard')
admin_dash = admin_dash.replace('/user/report/download', '/admin/report/download')
admin_dash = admin_dash.replace('href="input.html"', 'href="users.html"')
admin_dash = admin_dash.replace('➕ <span>Add Data</span>', '👥 <span>Users</span>')
admin_dash = admin_dash.replace('href="notifications.html"', 'href="leaderboard.html" class="nav-link flex items-center gap-3 px-4 py-3 rounded-xl transition-all opacity-80 hover:opacity-100 hover:bg-white/10">🏆 <span>Leaderboard</span></a></li>\n            <li><a href="notifications.html"')

# We must adjust the fetch logic inside admin_dash to use admin endpoints. 
admin_dash = re.sub(r'\(data.totalProduced\|\|0\).toFixed\(2\)', '(data.avgEfficiency||0).toFixed(2) + "%"', admin_dash)
admin_dash = admin_dash.replace('Total Produced', 'Avg Efficiency').replace('kWh today', '')
admin_dash = re.sub(r'\(data.totalConsumed\|\|0\).toFixed\(2\)', '(data.avgCO2Consumed||0).toFixed(2)', admin_dash)
admin_dash = admin_dash.replace('Total Consumed', 'Avg CO₂ Consumed')
admin_dash = admin_dash.replace('CO₂ Emitted', 'Avg CO₂ Produced')
admin_dash = admin_dash.replace('co2Emitted = (data.todayDataList||[]).reduce((sum,d)=> sum + (d.co2Produced||0),0);', 'co2Emitted = data.avgCO2Produced || 0;')
admin_dash = admin_dash.replace('co2Saved = (data.todayDataList||[]).reduce((sum,d)=> sum + (d.co2Consumed||0),0);', 'co2Saved = data.avgCO2Consumed || 0;')
admin_dash = admin_dash.replace('document.getElementById(\'totalBill\').textContent = (data.totalBill||0)', 'document.getElementById(\'totalBill\').textContent = (data.totalElectricityBill||0)')
admin_dash = admin_dash.replace('data.todayDataList', '[{source: "Solar", efficiency: data.solarEff, energyProduced: 0, energyConsumed: 0, co2Produced: data.solarCO2, co2Consumed: 0, electricityBill: data.solarBill}, {source: "Wind", efficiency: data.windEff, energyProduced: 0, energyConsumed: 0, co2Produced: data.windCO2, co2Consumed: 0, electricityBill: data.windBill}, {source: "Hydro", efficiency: data.hydroEff, energyProduced: 0, energyConsumed: 0, co2Produced: data.hydroCO2, co2Consumed: 0, electricityBill: data.hydroBill}]')

write_admin_file('dashboard.html', admin_dash)

# Admin Analytics
user_analytics = read_user_file_as_template('analytics.html')
admin_analytics = user_analytics.replace('user/analytics', 'admin/analytics')
admin_analytics = admin_analytics.replace('Analytics - REPM', 'Admin Analytics - REPM')
admin_analytics = admin_analytics.replace('/user/analytics', '/admin/analytics')
admin_analytics = admin_analytics.replace('href="input.html"', 'href="users.html"')
admin_analytics = admin_analytics.replace('➕ <span>Add Data</span>', '👥 <span>Users</span>')
admin_analytics = admin_analytics.replace('href="notifications.html"', 'href="leaderboard.html" class="nav-link flex items-center gap-3 px-4 py-3 rounded-xl transition-all opacity-80 hover:opacity-100 hover:bg-white/10">🏆 <span>Leaderboard</span></a></li>\n            <li><a href="notifications.html"')

write_admin_file('analytics.html', admin_analytics)

print("Admin dashboard & analytics generated.")
