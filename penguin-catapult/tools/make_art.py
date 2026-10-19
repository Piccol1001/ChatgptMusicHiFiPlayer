"""Generate original, replaceable SVG game art. No frames or sprites are extracted."""
from pathlib import Path
import random
ROOT=Path(__file__).resolve().parents[1]/'assets'
ROOT.mkdir(exist_ok=True)
def svg(name,w,h,body,defs=''):
 (ROOT/name).write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs>{defs}</defs>{body}</svg>')
rng=random.Random(771)
# Dense, irregular pebbles are a key visual feature of the reference cliffs.
body='<rect width="160" height="160" fill="#4a777e"/>'
colors=['#607b7e','#87937f','#728996','#92a89f','#a4ab91','#5c949c','#b5b28b']
for y in range(-8,180,15):
 for x in range(-10,180,18):
  xx=x+rng.randint(-4,4)+(8 if (y//15)%2 else 0); yy=y+rng.randint(-3,3)
  rx=rng.randint(7,11);ry=rng.randint(6,9); color=rng.choice(colors)
  body+=f'<ellipse cx="{xx}" cy="{yy}" rx="{rx}" ry="{ry}" fill="{color}" stroke="#3e696d" stroke-width="1.4"/><path d="M{xx-rx+2} {yy-1} Q{xx} {yy-ry-3} {xx+rx-2} {yy-2}" fill="none" stroke="#d2d3b2" stroke-opacity=".45" stroke-width="1.4"/>'
svg('stone.svg',160,160,body)
body='<rect width="96" height="96" fill="url(#ice)"/>'
for i in range(45):
 x,y=rng.randrange(96),rng.randrange(96)
 body+=f'<path d="M{x} {y}l{rng.randrange(-20,20)} {rng.randrange(6,27)}l{rng.randrange(-20,20)} {rng.randrange(6,27)}" fill="none" stroke="{rng.choice(["#83f0ff","#148eca","#b5fbff"])}" stroke-width="{rng.choice([1,2,3])}" opacity=".75"/>'
for x,y in [(18,25),(55,70),(75,22)]:body+=f'<path d="M{x-6} {y}l6 -6l6 6l-6 6Z" fill="#ffe780" stroke="#cfb14a" stroke-width="1"/>'
svg('ice.svg',96,96,body,'<linearGradient id="ice" x2=".8" y2="1"><stop stop-color="#78eaff"/><stop offset=".5" stop-color="#2cbce9"/><stop offset="1" stop-color="#0985c6"/></linearGradient>')
# Orange-to-yellow sky, layered warm clouds, frozen sea and distant ice stacks.
body='<rect width="960" height="704" fill="url(#sky)"/>'
for x,y,s in [(60,320,1),(340,245,1.4),(720,310,1.2),(880,210,.8)]:
 body+=f'<g transform="translate({x} {y}) scale({s})"><path d="M-110 18Q-95-5-75 0Q-56-30-35-8Q-20-48 0-18Q24-32 35-5Q63-22 80 5L110 18Z" fill="url(#cloud)"/><path d="M-90 21H100M-55 29H50" stroke="#ffeb00" stroke-width="3" opacity=".65"/></g>'
body+='<rect y="526" width="960" height="178" fill="url(#sea)"/>'
for i in range(24):
 x=rng.randrange(960);y=rng.randrange(547,704)
 body+=f'<path d="M{x} {y}l{rng.randrange(10,45)} -3l8 9l-25 4Z" fill="#ecffff" opacity=".63"/>'
for x,y,w,h in [(40,492,65,60),(130,518,53,55),(690,502,100,80),(840,487,76,64),(430,545,80,70)]:
 body+=f'<path d="M{x} {y+h}V{y}l{w*.2}-16l{w*.2} 17l{w*.2}-22l{w*.2} 22l{w*.2}-6V{y+h}Z" fill="#d3f4fa"/><path d="M{x+w*.2} {y-10}v{h+9}m{w*.4} {-h-12}v{h+20}" stroke="#a0d4e6" stroke-width="8"/><path d="M{x} {y+h}l{w} 4l-17 15l-{w} -5Z" fill="#a3d8e1" opacity=".7"/>'
svg('sunset.svg',960,704,body,'<linearGradient id="sky" x2="0" y2="1"><stop stop-color="#db641e"/><stop offset=".56" stop-color="#f8af08"/><stop offset=".84" stop-color="#fff600"/><stop offset="1" stop-color="#eaf76c"/></linearGradient><linearGradient id="cloud" x2="0" y2="1"><stop stop-color="#d6721a"/><stop offset="1" stop-color="#fff200"/></linearGradient><linearGradient id="sea" x2="0" y2="1"><stop stop-color="#dceea4"/><stop offset="1" stop-color="#8fd2d9"/></linearGradient>')
# Ice village: usable behind the launch cavern, with depth and subtle shading.
body='<rect width="960" height="704" fill="url(#air)"/>'
for i in range(15):
 x=i*85-80; y=220+rng.randrange(-35,50)
 body+=f'<path d="M{x-70} {y+90}Q{x-100} {y-10} {x-45} {y+12}Q{x-35} {y-70} {x+5} {y-16}Q{x+60} {y-54} {x+65} {y+20}Q{x+130} {y-15} {x+140} {y+90}Z" fill="#b9afbe" stroke="#ecd4d7" stroke-width="5"/>'
body+='<rect y="346" width="960" height="358" fill="url(#snow)"/>'
for i in range(26):
 x=rng.randrange(-20,960); y=rng.randrange(370,650);w=rng.randrange(42,90);h=rng.randrange(65,170)
 body+=f'<path d="M{x-w/2} {y}v-{h*.5}Q{x-w*.5} {y-h} {x} {y-h}Q{x+w*.5} {y-h} {x+w*.5} {y-h*.5}V{y}Z" fill="url(#igloo)" stroke="#9dc8e5" stroke-width="2"/><path d="M{x-12} {y}v-24q0-26 24 0v24" fill="#628fb4" stroke="#c9ebfb" stroke-width="5"/><ellipse cx="{x-10}" cy="{y-h+14}" rx="{w*.28}" ry="8" fill="#f6feff" opacity=".85"/>'
svg('village.svg',960,704,body,'<linearGradient id="air" x2="0" y2="1"><stop stop-color="#59b9e6"/><stop offset="1" stop-color="#eaf4ff"/></linearGradient><linearGradient id="snow" x2="0" y2="1"><stop stop-color="#e6f4ff"/><stop offset="1" stop-color="#b1dff2"/></linearGradient><linearGradient id="igloo"><stop stop-color="#769fc8"/><stop offset=".4" stop-color="#dcf4ff"/><stop offset="1" stop-color="#a4cce5"/></linearGradient>')
# Penguin with olive helmet, cream belly, expressive eyes, and no invented scarf.
svg('penguin.svg',80,112,'''<ellipse cx="40" cy="105" rx="30" ry="5" fill="#4c7b88" opacity=".2"/><path d="M19 73Q-5 62 5 48L27 60M61 73Q85 62 75 48L53 60" fill="#17232e" stroke="#101c26" stroke-width="2"/><path d="M15 101L36 96L40 105L9 106ZM44 99L64 98L72 106L43 107Z" fill="#f5be3f" stroke="#7f6028" stroke-width="2"/><path d="M17 62Q14 38 25 22Q40 8 55 22Q66 38 63 62Q77 99 43 102Q5 103 17 62Z" fill="url(#black)" stroke="#182738" stroke-width="3"/><ellipse cx="40" cy="76" rx="21" ry="23" fill="url(#cream)"/><ellipse cx="29" cy="39" rx="10" ry="13" fill="#fffef0"/><ellipse cx="48" cy="39" rx="10" ry="13" fill="#fffef0"/><ellipse cx="32" cy="40" rx="3.5" ry="5" fill="#171e2b"/><ellipse cx="49" cy="40" rx="3.5" ry="5" fill="#171e2b"/><circle cx="33" cy="38" r="1.3" fill="white"/><circle cx="50" cy="38" r="1.3" fill="white"/><path d="M27 52Q43 45 56 52L42 64Z" fill="#ffcd51" stroke="#876032" stroke-width="2"/><path d="M18 29Q17 5 40 5Q64 5 64 29Z" fill="url(#helmet)" stroke="#3b4f37" stroke-width="3"/><path d="M14 30Q40 24 67 31" stroke="#354936" stroke-width="6" fill="none"/><path d="M25 17Q36 7 51 15" fill="none" stroke="#c1cb8c" stroke-width="3" opacity=".8"/>''','<linearGradient id="black"><stop stop-color="#4d5863"/><stop offset=".45" stop-color="#142331"/><stop offset="1" stop-color="#0b1620"/></linearGradient><linearGradient id="cream" x2="0" y2="1"><stop stop-color="#ffffff"/><stop offset="1" stop-color="#d9e0d7"/></linearGradient><linearGradient id="helmet" x2="0" y2="1"><stop stop-color="#adb986"/><stop offset="1" stop-color="#5d774f"/></linearGradient>')
svg('bear.svg',110,155,'''<ellipse cx="60" cy="149" rx="43" ry="5" fill="#425d67" opacity=".18"/><path d="M42 67Q36 48 39 30L30 24L24 27L13 19L19 11L42 14Q48 2 61 12Q76 21 68 42L78 69Q100 92 91 126L95 144Q82 154 67 146L61 132L56 147Q37 154 30 143L36 123Q25 95 42 67Z" fill="url(#fur)" stroke="#8d9f98" stroke-width="2.5"/><path d="M50 68Q76 70 76 108Q75 133 56 131Q31 126 40 99Z" fill="#d7ddbd"/><path d="M42 58Q19 47 20 60Q19 74 43 84Q59 81 55 68" fill="#f9fbea" stroke="#a3afa1" stroke-width="2"/><path d="M72 62Q89 65 87 82Q83 95 74 81" fill="#eef2dc" stroke="#a3afa1" stroke-width="2"/><ellipse cx="58" cy="15" rx="6" ry="7" fill="#f9fbea" stroke="#8d9f98" stroke-width="2"/><circle cx="57" cy="16" r="3" fill="#c5cdb8"/><path d="M12 17Q7 12 16 12L22 15Z" fill="#293f43"/><circle cx="36" cy="23" r="2.8" fill="#2a393c"/><path d="M27 31Q33 34 38 30M32 15L41 18" fill="none" stroke="#627c79" stroke-width="2"/><path d="M36 143L45 143M69 144L83 144" stroke="#c3cbb7" stroke-width="2"/>''','<linearGradient id="fur"><stop stop-color="#fffef0"/><stop offset=".6" stop-color="#f4f5df"/><stop offset="1" stop-color="#cdd6be"/></linearGradient>')
body='<rect width="160" height="160" fill="#44b7db"/>'
for i in range(210):
 x,y=rng.randrange(160),rng.randrange(160)
 dx,dy=rng.randrange(-45,45),rng.randrange(9,55)
 body+=f'<path d="M{x} {y}l{dx} {dy}l{-dx//2} {dy//2}" fill="none" stroke="{rng.choice(["#d6f9ff","#88deef","#1f91b8","#b8f3f6"])}" stroke-width="{rng.choice([1,1,2])}" opacity=".65"/>'
svg('frost.svg',160,160,body)
print('Generated 7 original SVG assets.')
