"""Generates frontend/src/assets/network-bg.svg — a deterministic abstract network for the login page.
Run: python3 scripts/generate_login_background.py  (no third-party dependencies)."""
import math, random, pathlib

W, H = 1920, 1080
rnd = random.Random(7)
nodes = []
while len(nodes) < 95:
    x, y = rnd.uniform(-40, W + 40), rnd.uniform(-40, H + 40)
    if all((x - a) ** 2 + (y - b) ** 2 > 95 ** 2 for a, b, _ in nodes):
        nodes.append((x, y, rnd.choice([1.6, 2.2, 2.8, 3.6])))

edges = set()
for i, (x, y, _) in enumerate(nodes):
    near = sorted(range(len(nodes)), key=lambda j: (nodes[j][0] - x) ** 2 + (nodes[j][1] - y) ** 2)[1:4]
    for j in near:
        edges.add(tuple(sorted((i, j))))

# A highlighted directed cycle (the "deadlock") on the right side of the frame.
cx, cy, r = 1600, 150, 105
cycle = [(cx + r * math.cos(a), cy + r * math.sin(a)) for a in (-math.pi / 2, math.pi / 6, 5 * math.pi / 6)]

out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" preserveAspectRatio="xMidYMid slice">',
       '<defs>',
       '<radialGradient id="g1" cx="78%" cy="40%" r="55%"><stop offset="0" stop-color="#123a5c" stop-opacity=".85"/><stop offset="1" stop-color="#080D1A" stop-opacity="0"/></radialGradient>',
       '<radialGradient id="g2" cx="15%" cy="85%" r="50%"><stop offset="0" stop-color="#103d3a" stop-opacity=".6"/><stop offset="1" stop-color="#080D1A" stop-opacity="0"/></radialGradient>',
       '<radialGradient id="g3" cx="45%" cy="5%" r="40%"><stop offset="0" stop-color="#2a1f5c" stop-opacity=".45"/><stop offset="1" stop-color="#080D1A" stop-opacity="0"/></radialGradient>',
       '<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>',
       '<filter id="soft"><feGaussianBlur stdDeviation="14"/></filter>',
       '<pattern id="grid" width="48" height="48" patternUnits="userSpaceOnUse"><path d="M48 0H0V48" fill="none" stroke="#9AAAC4" stroke-opacity=".05" stroke-width="1"/></pattern>',
       '<marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="#54D6FF"/></marker>',
       '</defs>',
       f'<rect width="{W}" height="{H}" fill="#080D1A"/>',
       f'<rect width="{W}" height="{H}" fill="url(#g1)"/><rect width="{W}" height="{H}" fill="url(#g2)"/><rect width="{W}" height="{H}" fill="url(#g3)"/>',
       f'<rect width="{W}" height="{H}" fill="url(#grid)"/>',
       '<g stroke="#54D6FF" stroke-width="1">']
for i, j in sorted(edges):
    (x1, y1, _), (x2, y2, _) = nodes[i], nodes[j]
    op = 0.07 + 0.13 * rnd.random()
    out.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke-opacity="{op:.2f}"/>')
out.append('</g><g>')
for x, y, s in nodes:
    col = rnd.choice(["#54D6FF", "#55E6C1", "#9AAAC4", "#54D6FF"])
    out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{s * 3:.1f}" fill="{col}" opacity=".08"/>')
    out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{s:.1f}" fill="{col}" opacity="{0.35 + 0.4 * rnd.random():.2f}"/>')
out.append('</g>')
# cycle
out.append(f'<circle cx="{cx}" cy="{cy}" r="{r + 60}" fill="#54D6FF" opacity=".06" filter="url(#soft)"/>')
out.append('<g fill="none" stroke="#54D6FF" stroke-width="2.2" filter="url(#glow)" stroke-opacity=".9">')
for k in range(3):
    (x1, y1), (x2, y2) = cycle[k], cycle[(k + 1) % 3]
    dx, dy = x2 - x1, y2 - y1
    d = math.hypot(dx, dy)
    sx, sy, ex, ey = x1 + dx / d * 26, y1 + dy / d * 26, x2 - dx / d * 30, y2 - dy / d * 30
    mx, my = (sx + ex) / 2 + dy / d * 28, (sy + ey) / 2 - dx / d * 28
    out.append(f'<path d="M{sx:.1f} {sy:.1f} Q{mx:.1f} {my:.1f} {ex:.1f} {ey:.1f}" marker-end="url(#arr)"/>')
out.append('</g>')
for k, (x, y) in enumerate(cycle):
    out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="20" fill="#0B1528" stroke="{["#54D6FF","#55E6C1","#54D6FF"][k]}" stroke-width="2" filter="url(#glow)"/>')
    out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="5" fill="#F2F6FF"/>')
out.append('</svg>')
p = pathlib.Path(__file__).resolve().parent.parent / "frontend/src/assets/network-bg.svg"
p.write_text("\n".join(out))
print(p, p.stat().st_size, "bytes")
