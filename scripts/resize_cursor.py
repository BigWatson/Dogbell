from PIL import Image
import os

SRC = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'static', 'DogCursor.png')
DST = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'static', 'DogCursor32.png')

SRC = os.path.abspath(SRC)
DST = os.path.abspath(DST)

os.makedirs(os.path.dirname(DST), exist_ok=True)

if not os.path.exists(SRC):
    print('Source image not found:', SRC)
    raise SystemExit(1)

with Image.open(SRC) as im:
    im = im.convert('RGBA')
    im_resized = im.resize((32, 32), Image.LANCZOS)
    im_resized.save(DST, format='PNG')
    print('Saved resized cursor to', DST)
