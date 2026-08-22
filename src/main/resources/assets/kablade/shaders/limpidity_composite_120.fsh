#version 120
uniform sampler2D Scene;
uniform sampler2D Effect;
uniform sampler2D Bloom;
uniform vec2 TexelSize;
uniform vec2 UvScale;
varying vec2 vUv;
void main(){vec2 uv=vUv*UvScale;vec4 scene=texture2D(Scene,uv),effect=texture2D(Effect,uv),bloom=texture2D(Bloom,uv);vec3 glow=bloom.rgb*(.58+min(2.2,dot(bloom.rgb,vec3(.299,.587,.114)))*.34);vec3 color=scene.rgb+effect.rgb+glow;gl_FragColor=vec4(color,1.0);}
