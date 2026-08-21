#version 120
uniform float uTime;
varying vec4 vColor;
varying vec2 vUv;
float hash2(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p),u=f*f*(3.0-2.0*f);return mix(mix(hash2(i),hash2(i+vec2(1,0)),u.x),mix(hash2(i+vec2(0,1)),hash2(i+vec2(1,1)),u.x),u.y);}
float taper(float u){return smoothstep(0.0,.09,u)*(1.0-smoothstep(.91,1.0,u));}
void main(){float kind=floor(vUv.x/2.0),u=fract(vUv.x/2.0)*2.0,v=clamp(vUv.y,0.0,1.0),across=abs(v*2.0-1.0),time=uTime;vec3 tint=vColor.rgb,indigo=vec3(.12,.13,.25),lavender=vec3(.68,.70,1),ivory=vec3(1,.94,.76),pearl=vec3(1,.99,.93);float alpha=vColor.a;
 if(kind<3.5){float body=1.0-smoothstep(.12,1.0,across),core=1.0-smoothstep(0.0,.22,across),needle=1.0-smoothstep(0.0,.055,across),flow=pow(max(0.0,sin(u*34.0-time*.82+v*5.0)),7.0),grain=noise(vec2(u*13.0-time*.18,v*5.0+time*.04)),longitudinal=kind<1.5?taper(u):1.0,opacity=alpha*longitudinal*(body*.34+core*.72+needle*.92+flow*.18)*(.82+grain*.18);if(opacity<.004)discard;vec3 ordered=mix(lavender,ivory,clamp(tint.r-tint.b+.55,0.0,1.0)),color=mix(indigo,mix(tint,ordered,.42),body*.92);color=mix(color,ivory,core*.62+flow*.18);color=mix(color,pearl,needle*.74);gl_FragColor=vec4(color*(1.12+core*1.18+needle*1.54+flow*.42),clamp(opacity,0.0,1.0));return;}
 if(kind<5.0){vec2 p=vec2(u*2.0-1.0,v*2.0-1.0);float d=abs(p.x)*.66+abs(p.y),body=1.0-smoothstep(.72,1.08,d),edge=smoothstep(.38,.68,d)*(1.0-smoothstep(.68,1.08,d)),core=1.0-smoothstep(0.0,.30,d),glint=pow(max(0.0,sin((p.x-p.y)*18.0+time*1.35)),9.0),opacity=alpha*body*(edge*.78+core*.42+glint*.32);if(opacity<.004)discard;vec3 color=mix(tint,ivory,edge*.52+core*.34);color=mix(color,pearl,core*.56+glint*.72);gl_FragColor=vec4(color*(1.10+edge*.82+core*1.18+glint),clamp(opacity,0.0,1.0));return;}
 discard;}
