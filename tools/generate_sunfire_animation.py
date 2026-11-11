"""Retime the existing heavy-sword rig to Sunfire's 26-tick / 39-frame choreography."""
from pathlib import Path
import math
import struct

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/kablade/combostate"
# 30 FPS: anticipation, rising, apex hold, fast dive, impact hold, recovery.
KNOTS = [(0, 0), (6, 8), (21, 18), (27, 18), (33, 26), (36, 26), (39, 67)]

def source_frame(frame):
    for (a, x), (b, y) in zip(KNOTS, KNOTS[1:]):
        if frame <= b:
            t = (frame - a) / (b - a)
            return x + (y - x) * t
    return 67

def generate(kind):
    source = (ROOT / ("valkyrie_impact_" + kind + ".vmd")).read_bytes()
    count = struct.unpack_from("<I", source, 50)[0]
    tracks = {}
    for i in range(count):
        row = struct.unpack_from("<15sI3f4f64s", source, 54 + 111 * i)
        tracks.setdefault(row[0], {})[row[1]] = row
    rows = []
    for name, keys in tracks.items():
        for frame in range(40):
            at = source_frame(frame)
            a, b = keys[math.floor(at)], keys[math.ceil(at)]
            t = at - math.floor(at)
            xyz = [x + (y - x) * t for x, y in zip(a[2:5], b[2:5])]
            qa, qb = a[5:9], b[5:9]
            if sum(x*y for x,y in zip(qa,qb)) < 0:
                qb = [-x for x in qb]
            q = [x + (y - x)*t for x,y in zip(qa,qb)]
            length = math.sqrt(sum(x*x for x in q))
            q = [x / length for x in q]
            rows.append(struct.pack("<15sI3f4f64s", name, frame, *xyz, *q, a[9]))
    output = source[:50] + struct.pack("<I", len(rows)) + b"".join(rows) + bytes(20)
    (ROOT / ("sunfire_impact_" + kind + ".vmd")).write_bytes(output)

if __name__ == "__main__":
    generate("blade")
    generate("player")
