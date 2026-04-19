import os, glob, re

# Read admin_dash tailwind base layout
with open('frontend/admin/dashboard.html', 'r', encoding='utf-8') as f:
    admin_dash = f.read()

head_top_match = re.search(r'<head>.*?</head>', admin_dash, re.DOTALL)
head_top = head_top_match.group(0) if head_top_match else ''

aside_match = re.search(r'<aside.*?</aside>', admin_dash, re.DOTALL)
aside = aside_match.group(0) if aside_match else ''
# We need to strip the active class from dashboard.html
aside = aside.replace('bg-white/20 font-bold shadow-inner', 'hover:bg-white/10 transition-all opacity-80 hover:opacity-100')

header_match = re.search(r'<header.*?</header>', admin_dash, re.DOTALL)
header_str = header_match.group(0) if header_match else ''
# Remove the custom Add Data / Users button from header.
header_str = re.sub(r'<a href="users\.html".*?</a>', '', header_str, flags=re.DOTALL)

def process_file(filename, short_title, long_title, desc):
    with open('frontend/admin/' + filename, 'r', encoding='utf-8') as f:
        content = f.read()
        
    main_match = re.search(r'<div class="main">(.*?)</div>\s*<script>', content, re.DOTALL)
    main_body = main_match.group(1) if main_match else ''
    
    # Strip some old classes and inline styles from main_body (the "box" classes and tables)
    # Wrap elements nicely
    main_body = main_body.replace('<div class="box">', '<div class="bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-6 mb-6">')
    main_body = main_body.replace('<table id=', '<table class="w-full text-sm text-left text-gray-500 dark:text-gray-400" id=')
    main_body = main_body.replace('<thead><tr', '<thead class="text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400"><tr')
    main_body = main_body.replace('<th>', '<th class="px-6 py-3">')
    main_body = main_body.replace('<td>', '<td class="px-6 py-4 border-b dark:border-gray-700">')
    main_body = main_body.replace('<h2>', '<h3 class="text-lg font-bold text-gray-800 dark:text-white mb-4">')
    main_body = main_body.replace('</h3>', '</h3>')
    
    # For form elements in users/settings
    main_body = main_body.replace('<input ', '<input class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white" ')
    main_body = main_body.replace('<select ', '<select class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:text-white" ')
    main_body = main_body.replace('<button class="btn"', '<button class="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded-lg"')
    main_body = main_body.replace('<button class="btn-alert"', '<button class="bg-red-500 hover:bg-red-600 text-white font-bold py-1 px-3 rounded-lg text-xs"')

    # Fix script tag
    script_match = re.search(r'<script>(.*?)</script>', content, re.DOTALL)
    script_body = script_match.group(1) if script_match else ''
    
    if 'toggleDropdown' not in script_body:
        js = """
        function toggleDropdown() { document.getElementById("dropdownMenu").classList.toggle("hidden"); }
        window.onclick = function(e) { if (!e.target.closest('.relative')) document.getElementById("dropdownMenu")?.classList.add("hidden"); };
        """
        script_body = js + '\n' + script_body

    # Setup the active tab
    active_aside = aside.replace(f'href="{filename}" class="nav-link flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-white/10 transition-all opacity-80 hover:opacity-100"',
                                 f'href="{filename}" class="nav-link flex items-center gap-3 px-4 py-3 rounded-xl bg-white/20 font-bold shadow-inner"')
    
    # Setup header title correctly 
    active_header = header_str.replace('Admin Dashboard', long_title)

    final_html = f"""<!DOCTYPE html>
<html lang="en">
{head_top.replace('Admin Dashboard - REPM', short_title + ' - REPM Admin')}
<body class="flex h-screen bg-gray-50 dark:bg-gray-900 dark:text-white overflow-hidden">
{active_aside}

<div class="flex-1 flex flex-col overflow-hidden">
{active_header}
<div class="flex-1 overflow-y-auto p-8 space-y-6">
{main_body}
</div>
</div>

<script>{script_body}</script>
</body></html>
"""
    with open('frontend/admin/' + filename, 'w', encoding='utf-8') as f:
        f.write(final_html)

process_file('users.html', 'Users', 'Manage Users', 'View and alert users')
process_file('leaderboard.html', 'Leaderboard', 'Leaderboard', 'Top energy efficient users')
process_file('notifications.html', 'Notifications', 'Notifications', 'View recent system activity')
process_file('settings.html', 'Settings', 'Settings', 'Manage admin preferences')
print("All admin files converted to tailwind layout.")
