"""Generates launcher icons and Play Store graphics for Prisma Blocks."""
import os, sys, math
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = sys.argv[1]
RES = os.path.join(ROOT, "app/src/main/res")
STORE = os.path.join(ROOT, "store")
os.makedirs(STORE, exist_ok=True)

CYAN = (34, 211, 238); MAGENTA = (232, 121, 249); LIME = (163, 230, 53)
ORANGE = (251, 146, 60); PINK = (244, 63, 94); VIOLET = (139, 92, 246); GOLD = (250, 204, 21)
BG_TOP = (7, 7, 22); BG_BOT = (20, 12, 51)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vgrad(w, h, top, bot):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        c = lerp(top, bot, y / max(1, h - 1))
        for x in range(w):
            px[x, y] = c
    return img


def radial(w, h, cx, cy, r, color, alpha):
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (int(255 * alpha),))
    return layer.filter(ImageFilter.GaussianBlur(r * 0.5))


def gem(canvas, x, y, s, color, glow=True):
    """Draw a glossy gem block onto RGBA canvas at (x, y) with side s."""
    W, H = canvas.size
    if glow:
        g = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(g).rounded_rectangle([x, y, x + s, y + s], radius=s * 0.22, fill=color + (150,))
        canvas.alpha_composite(g.filter(ImageFilter.GaussianBlur(s * 0.28)))
    body = vgrad(int(s), int(s), lerp(color, (255, 255, 255), 0.28), lerp(color, (0, 0, 0), 0.30)).convert("RGBA")
    mask = Image.new("L", (int(s), int(s)), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, s - 1, s - 1], radius=s * 0.22, fill=255)
    tile = Image.new("RGBA", (int(s), int(s)), (0, 0, 0, 0))
    tile.paste(body, (0, 0), mask)
    # bevel (drawn on its own layer so it blends instead of overwriting pixels)
    bevel = Image.new("RGBA", tile.size, (0, 0, 0, 0))
    ImageDraw.Draw(bevel).rounded_rectangle([s * 0.09, s * 0.09, s * 0.91, s * 0.91], radius=s * 0.16, outline=(255, 255, 255, 60), width=max(1, int(s * 0.045)))
    tile.alpha_composite(bevel)
    d = ImageDraw.Draw(tile)
    # highlight
    hl = Image.new("RGBA", (int(s), int(s)), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hl)
    top, bot = int(s * 0.08), int(s * 0.46)
    for yy in range(top, bot):
        a = int(140 * (1 - (yy - top) / (bot - top)))
        hd.line([(s * 0.14, yy), (s * 0.86, yy)], fill=(255, 255, 255, a))
    hmask = Image.new("L", (int(s), int(s)), 0)
    ImageDraw.Draw(hmask).rounded_rectangle([s * 0.12, top, s * 0.88, bot], radius=s * 0.14, fill=255)
    tile.alpha_composite(Image.composite(hl, Image.new("RGBA", hl.size, (0, 0, 0, 0)), hmask))
    # sparkle
    r = s * 0.06
    sp = Image.new("RGBA", tile.size, (0, 0, 0, 0))
    ImageDraw.Draw(sp).ellipse([s * 0.25 - r, s * 0.23 - r, s * 0.25 + r, s * 0.23 + r], fill=(255, 255, 255, 200))
    tile.alpha_composite(sp)
    canvas.alpha_composite(tile, (int(x), int(y)))


def background(w, h):
    bg = vgrad(w, h, BG_TOP, BG_BOT).convert("RGBA")
    bg.alpha_composite(radial(w, h, w * 0.25, h * 0.2, w * 0.55, (124, 58, 237), 0.55))
    bg.alpha_composite(radial(w, h, w * 0.9, h * 0.7, w * 0.45, (6, 182, 212), 0.35))
    bg.alpha_composite(radial(w, h, w * 0.15, h * 1.0, w * 0.5, (219, 39, 119), 0.40))
    return bg


def icon_blocks(canvas, cx, cy, unit):
    """A tetromino-like cluster: 3 gems in an L + 1 gold accent."""
    s = unit
    gap = unit * 0.08
    step = s + gap
    ox = cx - step
    oy = cy - step
    gem(canvas, ox, oy, s, CYAN)
    gem(canvas, ox + step, oy, s, MAGENTA)
    gem(canvas, ox, oy + step, s, VIOLET)
    gem(canvas, ox + step, oy + step, s, GOLD)


def full_icon(size, round_mask=False):
    S = size * 4
    img = background(S, S)
    icon_blocks(img, S / 2 + S * 0.0, S / 2, S * 0.30)
    img = img.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=size * 0.22, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def foreground(size):
    # adaptive foreground: 108dp canvas, keep art within the 66dp safe zone
    S = size * 4
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    icon_blocks(img, S / 2, S / 2, S * 0.19)
    return img.resize((size, size), Image.LANCZOS)


def adaptive_background(size):
    S = size * 2
    return background(S, S).resize((size, size), Image.LANCZOS)


densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for name, f in densities.items():
    d = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    full_icon(int(48 * f)).save(os.path.join(d, "ic_launcher.png"))
    full_icon(int(48 * f), round_mask=True).save(os.path.join(d, "ic_launcher_round.png"))
    foreground(int(108 * f)).save(os.path.join(d, "ic_launcher_foreground.png"))
    adaptive_background(int(108 * f)).save(os.path.join(d, "ic_launcher_background.png"))

# Play Store icon (512x512, no transparency)
store_icon = full_icon(512)
flat = Image.new("RGB", (512, 512), BG_TOP)
flat.paste(store_icon, (0, 0), store_icon)
background(512, 512).convert("RGB").save(os.path.join(STORE, "_tmp.png"))
full = background(2048, 2048)
icon_blocks(full, 1024, 1024, 2048 * 0.30)
full.convert("RGB").resize((512, 512), Image.LANCZOS).save(os.path.join(STORE, "icon-512.png"))
os.remove(os.path.join(STORE, "_tmp.png"))

# Feature graphic 1024x500
W, H = 2048, 1000
fg = background(W, H)
colors = [CYAN, MAGENTA, LIME, ORANGE, PINK, VIOLET, GOLD]
import random
random.seed(7)
u = 120
# scattered small board on the right
for r in range(5):
    for c in range(6):
        if random.random() < 0.62:
            gem(fg, W * 0.54 + c * (u + 10), H * 0.12 + r * (u + 10), u, random.choice(colors))
# title
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 190)
    font2 = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 78)
except Exception:
    font = ImageFont.load_default(); font2 = font
title = Image.new("RGBA", (W, H), (0, 0, 0, 0))
td = ImageDraw.Draw(title)
td.text((110, 300), "PRISMA", font=font, fill=(255, 255, 255, 255))
td.text((110, 520), "BLOCKS", font=font, fill=(255, 255, 255, 255))
# gradient fill for title
grad = Image.new("RGBA", (W, H))
gp = grad.load()
for x in range(W):
    c = lerp(CYAN, MAGENTA, min(1, x / (W * 0.45)))
    for y in range(H):
        gp[x, y] = c + (255,)
glow = title.filter(ImageFilter.GaussianBlur(24))
glow_col = Image.new("RGBA", (W, H), MAGENTA + (0,))
glow_col.putalpha(glow.split()[3].point(lambda a: int(a * 0.7)))
fg.alpha_composite(glow_col)
fg.alpha_composite(Image.composite(grad, Image.new("RGBA", (W, H), (0, 0, 0, 0)), title.split()[3]))
fg.convert("RGB").resize((1024, 500), Image.LANCZOS).save(os.path.join(STORE, "feature-graphic-1024x500.png"))
print("done")
