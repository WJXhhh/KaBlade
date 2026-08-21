#version 120
uniform float uTime; uniform sampler2D uSampler; varying vec4 vColor; varying vec2 vUv;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){float a=abs(vUv.y*2.0-1.0),sheath=pow(max(0.0,1.0-a),1.72),core=1.0-smoothstep(0.035,0.205,a);
 float cell=h(floor(vec2(vUv.x*31.0-uTime*0.17,vUv.y*7.0))),crawl=texture2D(uSampler,fract(vec2(vUv.x*.54-uTime*.0135,vUv.y*1.7+cell*.19))).r,packet=1.0-smoothstep(0.07,0.31,abs(fract(vUv.x*4.0-uTime*0.11+cell*0.2)-0.5));
 float strobe=step(.24,h(vec2(floor(uTime*.94),cell*17.0))),alpha=vColor.a*(sheath*.92+core*.96)*(.80+crawl*.17+packet*.24)*mix(.78,1.0,strobe);
 vec3 color=mix(vColor.rgb,vec3(0.75,0.34,1.0),sheath*0.18);color=mix(color,vec3(1.0,0.985,1.0),core*0.94);
 if(alpha<0.002)discard;gl_FragColor=vec4(color*(0.98+core*1.78+packet*0.24),clamp(alpha,0.0,1.0));}
