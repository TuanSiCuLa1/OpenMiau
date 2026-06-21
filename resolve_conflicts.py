import sys
import re

def resolve_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    pattern = re.compile(r'<<<<<<< HEAD\r?\n(.*?)\r?\n=======\r?\n.*?\r?\n>>>>>>> [^\n]*\r?\n', re.DOTALL | re.MULTILINE)
    
    new_content, count = pattern.subn(r'\1\n', content)
    
    if count > 0:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Resolved {count} conflicts in {path}')

for path in sys.argv[1:]:
    resolve_file(path)
