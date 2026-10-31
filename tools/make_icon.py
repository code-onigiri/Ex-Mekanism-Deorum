"""MOD アイコン (32x32 ドット絵) を書き出す。python3 tools/make_icon.py"""
import zlib, struct, os, sys

W = 32
img = [[(0,0,0,255) for _ in range(W)] for _ in range(W)]

def px(x,y,c):
    if 0 <= x < W and 0 <= y < W: img[y][x] = c

def rect(x0,y0,x1,y1,c):
    for y in range(y0,y1+1):
        for x in range(x0,x1+1): px(x,y,c)

# palette
DARK   = (18,20,24,255)
PLATE  = (56,61,68,255)
PLATE2 = (68,74,82,255)
LIGHT  = (122,129,138,255)
STEEL  = (148,154,162,255)
STEEL_L= (198,203,210,255)
STEEL_D= (92,97,104,255)
EDGE   = (40,43,48,255)
WOOD   = (128,84,44,255)
WOOD_L = (168,116,64,255)
WOOD_D = (82,52,26,255)
LED    = (57,255,161,255)
LED_D  = (22,120,80,255)

rect(0,0,31,31,DARK)
rect(1,1,30,30,PLATE)
# vertical sheen
for y in range(2,10):
    c = tuple(min(255,int(PLATE2[i]+(18-y)*1.5)) for i in range(3))+(255,)
    for x in range(2,30): px(x,y,c)
# bevel
rect(1,1,30,1,LIGHT); rect(1,1,1,30,LIGHT)
rect(1,30,30,30,DARK);rect(30,1,30,30,DARK)
rect(2,2,29,2,EDGE)
for x in range(3,29):
    px(x,29,EDGE)
# corner screws
for (sx,sy) in [(3,3),(26,3),(3,26),(26,26)]:
    rect(sx,sy,sx+1,sy+1,STEEL_D)
    px(sx,sy,STEEL_L)

# hammer head (upright)
rect(8,7,23,7,DARK)          # top outline
rect(8,8,23,13,STEEL)
rect(9,8,22,8,STEEL_L)       # top highlight
rect(8,9,8,13,STEEL_L)       # left highlight
rect(23,8,23,13,STEEL_D)     # right shade
rect(9,13,22,13,STEEL_D)     # bottom shade
rect(8,14,23,14,DARK)        # bottom outline
# claw hints: carve two notches on top
rect(10,7,10,7,DARK); rect(21,7,21,7,DARK)
# handle
rect(13,14,18,26,WOOD_D)
rect(14,15,17,26,WOOD)
rect(14,15,14,26,WOOD_L)
px(18,26,DARK)
# LED strip
rect(9,27,22,27,DARK)
rect(10,28,21,28,LED)
rect(10,29,21,29,LED_D)
rect(9,29,9,29,DARK); rect(22,29,22,29,DARK)

def png(path, scale):
    w = W*scale
    raw = bytearray()
    for y in range(w):
        raw.append(0)
        row = img[y//scale]
        for x in range(w):
            raw.extend(row[x//scale])
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t+d) & 0xffffffff)
    out = b'\x89PNG\r\n\x1a\n'
    out += chunk(b'IHDR', struct.pack('>IIBBBBB', w, w, 8, 6, 0,0,0))
    out += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    out += chunk(b'IEND', b'')
    open(path,'wb').write(out)
    print(path, w)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.makedirs(os.path.join(ROOT, 'src/main/resources'), exist_ok=True)
png(os.path.join(ROOT, 'src/main/resources/icon.png'), 4)   # 128x128 in jar
png(os.path.join(ROOT, 'docs/icon.png'), 16)                # 512x512 for CurseForge
