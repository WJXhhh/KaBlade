#!/usr/bin/env python3
"""Generate plunge_blade.vmd - blade plunge into ground animation."""
import struct, math, os, sys

INPUT = os.path.join(os.path.dirname(__file__), 'combostate', 'motion.vmd')
OUTPUT = os.path.join(os.path.dirname(__file__), 'combostate', 'plunge_blade.vmd')

# ── Quaternion helpers ──
def q_axis_angle(axis, deg):
    """axis (x,y,z), degrees -> (qx,qy,qz,qw)"""
    r = math.radians(deg)
    s = math.sin(r / 2)
    c = math.cos(r / 2)
    return (axis[0]*s, axis[1]*s, axis[2]*s, c)

def q_mul(a, b):
    """Hamilton product (qx,qy,qz,qw)"""
    return (
        a[3]*b[0] + a[0]*b[3] + a[1]*b[2] - a[2]*b[1],
        a[3]*b[1] - a[0]*b[2] + a[1]*b[3] + a[2]*b[0],
        a[3]*b[2] + a[0]*b[1] - a[1]*b[0] + a[2]*b[3],
        a[3]*b[3] - a[0]*b[0] - a[1]*b[1] - a[2]*b[2],
    )

def q_slerp(a, b, t):
    dot = sum(ai*bi for ai, bi in zip(a, b))
    if dot < 0:
        b = tuple(-x for x in b)
        dot = -dot
    if dot > 0.9995:
        r = tuple(ai + t*(bi-ai) for ai, bi in zip(a, b))
        n = math.sqrt(sum(x*x for x in r))
        return tuple(x/n for x in r)
    th = math.acos(max(-1, min(1, dot)))
    s = math.sin(th)
    wa = math.sin((1-t)*th) / s
    wb = math.sin(t*th) / s
    return tuple(wa*ai + wb*bi for ai, bi in zip(a, b))

# ── Interp presets (64 bytes bezier curves) ──
LINEAR = bytes([0x14,0x14,0x00,0x00,0x14,0x14,0x14,0x14,
                0x6b,0x6b,0x6b,0x6b,0x6b,0x6b,0x6b,0x6b]*4)
SMOOTH = bytes([0x33,0x33,0x00,0x00,0x33,0x33,0x33,0x33,
                0x57,0x57,0x57,0x57,0x57,0x57,0x57,0x57]*4)
EASE_OUT = bytes([0x14,0x40,0x00,0x00,0x14,0x00,0x14,0x14,
                  0x6b,0x40,0x6b,0x6b,0x6b,0x7f,0x6b,0x6b]*4)

# ── Read motion.vmd frame 0 ──
with open(INPUT, 'rb') as f:
    data = f.read()

offset = 50
motion_count = struct.unpack_from('<I', data, offset)[0]
offset += 4

bone_frame0 = {}
bone_order = []
for i in range(motion_count):
    bone_raw = data[offset:offset+15]
    bone_key = bone_raw[:15]
    frame = struct.unpack_from('<I', data, offset+15)[0]
    px, py, pz = struct.unpack_from('<3f', data, offset+19)
    qx, qy, qz, qw = struct.unpack_from('<4f', data, offset+31)
    interp = data[offset+47:offset+111]
    offset += 111
    if frame == 0 and bone_key not in bone_frame0:
        bone_frame0[bone_key] = (px, py, pz, qx, qy, qz, qw, interp)
        bone_order.append(bone_key)

assert len(bone_order) == 9, f'Expected 9 bones, got {len(bone_order)}'

# Extract initial values per bone
def init(name):
    return bone_frame0[bone_order[bone_order.index(next(
        bk for bk in bone_order if bk.rstrip(b'\x00') == name.encode()))]]

# Bone references by name (match on prefix since padding varies)
def bone(name_enc):
    return next(bk for bk in bone_order if bk[:len(name_enc)] == name_enc)

BK_CENTER = bone_order[0]  # Shift_JIS name, raw bytes
BK_JA1 = bone(b'JointA1')
BK_JA2 = bone(b'JointA2')
BK_JA3 = bone(b'JointA3')
BK_HPA = bone(b'hardpointA')
BK_JB1 = bone(b'JointB1')
BK_JB2 = bone(b'JointB2')
BK_JB3 = bone(b'JointB3')
BK_HPB = bone(b'hardpointB')

# Initial pose data
d_center = bone_frame0[BK_CENTER]
d_ja1 = bone_frame0[BK_JA1]
d_ja2 = bone_frame0[BK_JA2]
d_ja3 = bone_frame0[BK_JA3]
d_hpa = bone_frame0[BK_HPA]
d_jb1 = bone_frame0[BK_JB1]
d_jb2 = bone_frame0[BK_JB2]
d_jb3 = bone_frame0[BK_JB3]
d_hpb = bone_frame0[BK_HPB]

# Initial rotation as tuples (qx,qy,qz,qw)
r_ja2_init = (d_ja2[3], d_ja2[4], d_ja2[5], d_ja2[6])
r_ja3_init = (d_ja3[3], d_ja3[4], d_ja3[5], d_ja3[6])
R_IDENTITY = (0.0, 0.0, 0.0, 1.0)
R_HP = (0.7071, 0.0, 0.7071, 0.0)  # hardpoint constant 90deg Y

# ── Animation plan ──
# JointA1 controls the main swing (Y=up, Z=back in MMD local)
# Rotate ~90deg around X axis to swing blade from "held up" to "driven into ground"
# Also shift Y down and Z forward to bring the whole blade toward ground level

keyframes = []

def kf(bone_key, frame, pos, rot, interp=LINEAR):
    keyframes.append((bone_key, frame, *pos, *rot, interp))

# ── Frame 0: Same as motion.vmd initial pose ──
for bk in bone_order:
    d = bone_frame0[bk]
    kf(bk, 0, (d[0], d[1], d[2]), (d[3], d[4], d[5], d[6]), d[7])

# ── Frame 30: Wind-up (raise blade back, slight lean) ──
kf(BK_CENTER, 30, (0, 0, 0), R_IDENTITY, SMOOTH)
kf(BK_JA1, 30, (0, 7.5, -2.0), q_axis_angle((1,0,0), -20), EASE_OUT)
kf(BK_JA2, 30, (d_ja2[0], d_ja2[1], d_ja2[2]), r_ja2_init, SMOOTH)
kf(BK_JA3, 30, (d_ja3[0], d_ja3[1], d_ja3[2]), r_ja3_init, SMOOTH)
kf(BK_HPA, 30, (0,0,0), R_HP, SMOOTH)
kf(BK_JB1, 30, (d_jb1[0], d_jb1[1], d_jb1[2]),
   (d_jb1[3], d_jb1[4], d_jb1[5], d_jb1[6]), SMOOTH)
kf(BK_JB2, 30, (d_jb2[0], d_jb2[1], d_jb2[2]),
   (d_jb2[3], d_jb2[4], d_jb2[5], d_jb2[6]), SMOOTH)
kf(BK_JB3, 30, (d_jb3[0], d_jb3[1], d_jb3[2]),
   (d_jb3[3], d_jb3[4], d_jb3[5], d_jb3[6]), SMOOTH)
kf(BK_HPB, 30, (0,0,0), R_HP, SMOOTH)

# ── Frame 60: Plunge (blade driven into ground) ──
# JointA1: rotate 90deg around X + lower Y + push Z forward
kf(BK_CENTER, 60, (0, 0, -2), R_IDENTITY, SMOOTH)  # root shifts forward
kf(BK_JA1, 60, (0, 3.5, -1.0), q_axis_angle((1,0,0), 90), EASE_OUT)
kf(BK_JA2, 60, (2.95, 0, 0), q_mul(r_ja2_init, q_axis_angle((0,0,1), -35)), SMOOTH)
kf(BK_JA3, 60, (0, 0, -4.55), q_mul(r_ja3_init, q_axis_angle((1,0,0), 25)), SMOOTH)
kf(BK_HPA, 60, (0,0,0), R_HP, SMOOTH)
# B chain: sheath follows slightly forward
kf(BK_JB1, 60, (0, 5.0, 2.0), q_axis_angle((1,0,0), 20), SMOOTH)
kf(BK_JB2, 60, (d_jb2[0], d_jb2[1], d_jb2[2]),
   (d_jb2[3], d_jb2[4], d_jb2[5], d_jb2[6]), SMOOTH)
kf(BK_JB3, 60, (d_jb3[0], d_jb3[1], d_jb3[2]),
   (d_jb3[3], d_jb3[4], d_jb3[5], d_jb3[6]), SMOOTH)
kf(BK_HPB, 60, (0,0,0), R_HP, SMOOTH)

# ── Frame 68: Impact bounce (slight recoil upward) ──
kf(BK_CENTER, 68, (0, 0.3, -1.5), R_IDENTITY, SMOOTH)
kf(BK_JA1, 68, (0, 4.2, -1.5), q_axis_angle((1,0,0), 80), SMOOTH)
kf(BK_JA2, 68, (2.95, 0, 0), q_mul(r_ja2_init, q_axis_angle((0,0,1), -30)), SMOOTH)
kf(BK_JA3, 68, (0, 0, -4.55), q_mul(r_ja3_init, q_axis_angle((1,0,0), 20)), SMOOTH)
kf(BK_HPA, 68, (0,0,0), R_HP, SMOOTH)
kf(BK_JB1, 68, (0, 5.0, 2.0), q_axis_angle((1,0,0), 20), SMOOTH)
kf(BK_JB2, 68, (d_jb2[0], d_jb2[1], d_jb2[2]),
   (d_jb2[3], d_jb2[4], d_jb2[5], d_jb2[6]), SMOOTH)
kf(BK_JB3, 68, (d_jb3[0], d_jb3[1], d_jb3[2]),
   (d_jb3[3], d_jb3[4], d_jb3[5], d_jb3[6]), SMOOTH)
kf(BK_HPB, 68, (0,0,0), R_HP, SMOOTH)

# ── Frame 100: Settle (hold blade in ground, slight breathing) ──
kf(BK_CENTER, 100, (0, 0, -2), R_IDENTITY, SMOOTH)
kf(BK_JA1, 100, (0, 3.5, -1.0), q_axis_angle((1,0,0), 88), SMOOTH)
kf(BK_JA2, 100, (2.95, 0, 0), q_mul(r_ja2_init, q_axis_angle((0,0,1), -33)), SMOOTH)
kf(BK_JA3, 100, (0, 0, -4.55), q_mul(r_ja3_init, q_axis_angle((1,0,0), 23)), SMOOTH)
kf(BK_HPA, 100, (0,0,0), R_HP, SMOOTH)
kf(BK_JB1, 100, (0, 5.0, 2.0), q_axis_angle((1,0,0), 18), SMOOTH)
kf(BK_JB2, 100, (d_jb2[0], d_jb2[1], d_jb2[2]),
   (d_jb2[3], d_jb2[4], d_jb2[5], d_jb2[6]), SMOOTH)
kf(BK_JB3, 100, (d_jb3[0], d_jb3[1], d_jb3[2]),
   (d_jb3[3], d_jb3[4], d_jb3[5], d_jb3[6]), SMOOTH)
kf(BK_HPB, 100, (0,0,0), R_HP, SMOOTH)

# ── Sort: by bone order in PMD, then by frame ──
bone_rank = {bk: i for i, bk in enumerate(bone_order)}
keyframes.sort(key=lambda k: (bone_rank[k[0]], k[1]))

# ── Stats ──
from collections import Counter
counts = Counter()
for kf_entry in keyframes:
    counts[kf_entry[0]] += 1
print(f'Total keyframes: {len(keyframes)}')
for bk in bone_order:
    name = bk.rstrip(b'\x00').decode('ascii', errors='replace')
    if not name.isascii():
        name = '(center)'
    print(f'  {name:12s}: {counts[bk]} frames')

# ── Write VMD ──
with open(OUTPUT, 'wb') as f:
    # Header
    f.write(b'Vocaloid Motion Data 0002'.ljust(30, b'\x00'))
    f.write(b'BladeHolder'.ljust(20, b'\x00'))
    # Motion count
    f.write(struct.pack('<I', len(keyframes)))
    # Keyframes
    for entry in keyframes:
        bone_key = entry[0]
        frame = entry[1]
        px, py, pz = entry[2], entry[3], entry[4]
        qx, qy, qz, qw = entry[5], entry[6], entry[7], entry[8]
        interp = entry[9]
        f.write(bone_key)                          # 15 bytes
        f.write(struct.pack('<I', frame))           # 4 bytes
        f.write(struct.pack('<3f', px, py, pz))     # 12 bytes
        f.write(struct.pack('<4f', qx, qy, qz, qw)) # 16 bytes
        f.write(interp)                             # 64 bytes
    # Face count = 0
    f.write(struct.pack('<I', 0))

size = os.path.getsize(OUTPUT)
print(f'\nWritten: {OUTPUT}')
print(f'Size: {size} bytes')
print(f'Expected: {30 + 20 + 4 + len(keyframes) * 111 + 4} bytes (header + keyframes + face_count)')
