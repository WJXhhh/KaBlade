#version 120
uniform float uTime; uniform sampler2D uSampler; varying vec4 vColor; varying vec2 vUv;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){vec2 p=vUv*2.0-1.0;float radius=length(p),diamond=1.0-smoothstep(0.46,1.0,abs(p.x)+abs(p.y));
 float rx=1.0-smoothstep(0.035,0.22,abs(p.x)),ry=1.0-smoothstep(0.035,0.22,abs(p.y)),star=max(rx*(1.0-smoothstep(0.18,1.0,abs(p.y))),ry*(1.0-smoothstep(0.18,1.0,abs(p.x))));
 float core=1.0-smoothstep(0.0,0.22,radius),grain=h(floor(vUv*23.0)+vec2(floor(uTime*0.13),floor(uTime*0.17))),mask=texture2D(uSampler,fract(vUv+vec2(uTime*.0009,-uTime*.0007))).r;
 float alpha=vColor.a*max(max(diamond*smoothstep(0.12,0.67,grain*.42+mask*.40+diamond*.34),star*0.76),core)*(0.72+0.28*sin(uTime*2.39+vUv.x*19.0+grain*2.7));
 vec3 color=mix(vColor.rgb,vec3(0.85,0.54,1.0),grain*0.18);color=mix(color,vec3(1.0),core*0.91);
 if(alpha<0.003)discard;gl_FragColor=vec4(color*(0.9+core*1.48+star*0.38),clamp(alpha,0.0,1.0));}
