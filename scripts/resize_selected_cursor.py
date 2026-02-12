from PIL import Image
import os

BASE = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SRC = os.path.join(BASE, 'src', 'main', 'resources', 'static', 'SelectedDogCursor.jpg')
DST = os.path.join(BASE, 'src', 'main', 'resources', 'static', 'SelectedDogCursor32.png')

if not os.path.exists(SRC):
    print('Source not found:', SRC)
    raise SystemExit(1)

os.makedirs(os.path.dirname(DST), exist_ok=True)

with Image.open(SRC) as im:
    im = im.convert('RGBA')
    im_resized = im.resize((32, 32), Image.LANCZOS)
    im_resized.save(DST, format='PNG')
    print('Saved resized cursor to', DST)
