#version 120
uniform sampler2D Scene;
uniform sampler2D Effect;
uniform sampler2D Bloom;
uniform vec2 TexelSize;
varying vec2 vUv;
void main(){vec4 scene=texture2D(Scene,vUv),effect=texture2D(Effect,vUv),bloom=texture2D(Bloom,vUv);vec3 glow=bloom.rgb*(.58+min(2.2,dot(bloom.rgb,vec3(.299,.587,.114)))*.34);vec3 color=scene.rgb+effect.rgb+glow;color=1.0-exp(-color*.92);gl_FragColor=vec4(color,scene.a);}
