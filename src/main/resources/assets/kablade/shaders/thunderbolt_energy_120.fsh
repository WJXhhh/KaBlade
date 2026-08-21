#version 120
uniform float uTime; uniform sampler2D uSampler; varying vec4 vColor; varying vec2 vUv;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(h(i),h(i+vec2(1,0)),f.x),mix(h(i+vec2(0,1)),h(i+vec2(1,1)),f.x),f.y);}
void main(){vec2 p=vUv*2.0-1.0;float across=abs(p.y),halo=1.0-smoothstep(0.22,1.0,across),core=1.0-smoothstep(0.0,0.115,across),radial=1.0-smoothstep(0.06,1.0,length(p));
 float broad=n(vec2(vUv.x*7.0-uTime*0.28,vUv.y*3.6+uTime*0.03)),fine=n(vec2(vUv.x*29.0-uTime*0.61,vUv.y*11.0-uTime*0.07)),mask=texture2D(uSampler,fract(vec2(vUv.x*.78-uTime*.021,vUv.y*.92+uTime*.0037))).r;
 float packets=smoothstep(0.58,0.91,broad*.62+mask*.38),body=max(halo,radial*.62),hot=max(core,radial*radial*.45),pulse=.86+.14*sin(uTime*4.70+vUv.x*15.0);
 float alpha=vColor.a*(body*.58+hot)*(.68+broad*.24+fine*.12+packets*.36)*pulse;
 vec3 color=mix(vColor.rgb,vec3(0.76,0.42,1.0),broad*0.24);color=mix(color,vec3(1.0,0.985,1.0),hot*0.91);
 if(alpha<0.003)discard;gl_FragColor=vec4(color*(0.88+hot*1.65+packets*0.30),clamp(alpha,0.0,1.0));}
