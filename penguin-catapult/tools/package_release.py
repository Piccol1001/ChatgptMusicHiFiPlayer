"""Package a previously exported Windows executable and a clean source archive."""
from pathlib import Path
import hashlib, shutil, zipfile
root = Path(__file__).resolve().parents[1]
exe = root/'builds/windows/Snowbound.exe'
if not exe.is_file():
    raise SystemExit('Export builds/windows/Snowbound.exe before packaging.')
windows = root/'builds/Snowbound-Windows-x86_64.zip'
with zipfile.ZipFile(windows, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    z.write(exe, 'Snowbound/Snowbound.exe')
    for name in ['README.md','REFERENCE.md','VALIDATION.md','VERSION']:
        z.write(root/name, 'Snowbound/'+name)
    for p in (root/'licenses').iterdir():
        z.write(p, 'Snowbound/licenses/'+p.name)
    for p in (root/'previews').iterdir():
        z.write(p, 'Snowbound/previews/'+p.name)
downloads=root/'downloads'
downloads.mkdir(exist_ok=True)
shutil.copy2(windows, downloads/windows.name)
source=root/'builds/Snowbound-Godot-Source.zip'
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(root.rglob('*')):
        rel=p.relative_to(root)
        if p.is_file() and not any(part in ('.godot','builds','downloads') for part in rel.parts):
            z.write(p, Path('penguin-catapult')/rel)
shutil.copy2(source,downloads/source.name)
lines=[]
for p in [exe, windows, source]:
    lines.append(hashlib.file_digest(p.open('rb'),'sha256').hexdigest()+'  '+str(p.relative_to(root/'builds')))
(root/'builds/SHA256SUMS.txt').write_text('\n'.join(lines)+'\n')
(downloads/'SHA256SUMS.txt').write_text(''.join(hashlib.file_digest(p.open('rb'),'sha256').hexdigest()+'  '+p.name+'\n' for p in [windows,source]))
for p in [windows,source,downloads/windows.name,downloads/source.name]:
    with zipfile.ZipFile(p) as z:
        assert z.testzip() is None, f'Corrupt ZIP: {p}'
    print(p.relative_to(root),p.stat().st_size,'bytes — archive integrity passed')
