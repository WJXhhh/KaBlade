#version 120
uniform float uTime; uniform sampler2D uSampler; varying vec4 vColor; varying vec2 vUv;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){vec2 p=vUv*2.0-1.0;float radius=length(p),ribbon=pow(max(0.0,1.0-abs(p.y)),1.18),radial=1.0-smoothstep(0.04,1.0,radius),circle=1.0-smoothstep(0.68,1.0,radius);
 float mask=texture2D(uSampler,vec2(fract(vUv.x*.73+uTime*.0017),clamp(vUv.y,0.0,1.0))).r,grain=h(floor(vUv*vec2(37.0,13.0))+vec2(floor(uTime*0.09),0)),sil=max(ribbon*(0.54+mask*0.33),radial*0.72)*circle;
 float alpha=vColor.a*sil*smoothstep(0.16,0.62,grain*.36+mask*.36+sil*.42)*(0.91+0.09*sin(uTime*1.73+radius*17.0));
 vec3 color=mix(vec3(0.075,0.008,0.14),vec3(0.28,0.045,0.46),clamp(dot(vColor.rgb,vec3(0.2126,0.7152,0.0722))*1.35+grain*0.22,0.0,1.0))+vColor.rgb*0.16;
 if(alpha<0.003)discard;gl_FragColor=vec4(color,clamp(alpha,0.0,1.0));}
