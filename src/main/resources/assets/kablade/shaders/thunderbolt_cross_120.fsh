#version 120
uniform float uTime; uniform sampler2D uSampler; varying vec4 vColor; varying vec2 vUv;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(h(i),h(i+vec2(1,0)),f.x),mix(h(i+vec2(0,1)),h(i+vec2(1,1)),f.x),f.y);}
void main(){float a=abs(vUv.y*2.0-1.0),body=pow(max(0.0,1.0-a),1.34),core=1.0-smoothstep(0.018,0.135,a);
 float serr=n(vec2(vUv.x*39.0-uTime*0.23,vUv.y*10.0)),turb=n(vec2(vUv.x*11.0+uTime*0.08,vUv.y*4.0)),gradient=texture2D(uSampler,vec2(fract(vUv.x*.84-uTime*.0056),clamp(vUv.y,0.0,1.0))).r;
 float written=smoothstep(.002,.048,vUv.x)*(1.0-smoothstep(.925,.998,vUv.x)),torn=smoothstep(.24,.76,serr+body*.43),notch=smoothstep(.10,.55,n(vec2(vUv.x*67.0-uTime*.47,a*13.0)));
 float alpha=vColor.a*(body*torn*(.70+notch*.32)+core)*(.64+gradient*.60)*written,hot=core*.94;vec3 color=mix(vec3(.36,.055,.68),vColor.rgb,.58+turb*.22);color=mix(color,vec3(.78,.34,1.0),body*.28);color=mix(color,vec3(1.0,.97,1.0),hot);
 if(alpha<.003)discard;gl_FragColor=vec4(color*(.96+core*1.92+notch*.18),clamp(alpha,0.0,1.0));}
