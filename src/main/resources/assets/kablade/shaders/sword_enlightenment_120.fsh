#version 120
uniform float uTime;
varying vec4 vColor;
varying vec2 vUv;
float hash2(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p),u=f*f*(3.0-2.0*f);return mix(mix(hash2(i),hash2(i+vec2(1,0)),u.x),mix(hash2(i+vec2(0,1)),hash2(i+vec2(1,1)),u.x),u.y);}
float taper(float u){return smoothstep(0.0,0.10,u)*(1.0-smoothstep(0.90,1.0,u));}
void main(){
 float kind=floor(vUv.x/2.0),u=fract(vUv.x/2.0)*2.0,v=clamp(vUv.y,0.0,1.0),across=abs(v*2.0-1.0),time=uTime;
 vec3 tint=vColor.rgb;float alpha=vColor.a;
 if(kind<0.5){float body=1.0-smoothstep(.16,1.0,across),core=1.0-smoothstep(0.0,.26,across),hot=1.0-smoothstep(0.0,.075,across);float flow=pow(max(0.0,sin(u*28.0-time*.78+v*4.0)),5.0),torn=noise(vec2(u*9.5-time*.16,v*4.0+time*.045)),feather=1.0-smoothstep(.70,1.0,across+torn*.10);float opacity=alpha*taper(u)*feather*(body*.42+core*.64+hot*.54+flow*.18);if(opacity<.004)discard;vec3 color=mix(vec3(.22,.03,.66),mix(vec3(.58,.22,1.0),tint,.30),body*.82+flow*.14);color=mix(color,vec3(1,.88,1),core*.58+hot*.48+flow*.22);gl_FragColor=vec4(color*(1.04+core*1.10+hot*1.70+flow*.60),clamp(opacity,0.0,1.0));return;}
 if(kind<1.5){float body=1.0-smoothstep(.12,1.0,across),core=1.0-smoothstep(0.0,.18,across),needle=1.0-smoothstep(0.0,.05,across),filament=pow(max(0.0,sin(u*36.0-time*1.05+v*6.0)),7.0),broken=smoothstep(.44,.88,noise(vec2(u*14.0-time*.26,v*6.0)));float opacity=alpha*taper(u)*(body*.30+core*.72+needle*1.10+filament*.22)*(.82+broken*.18);if(opacity<.004)discard;vec3 color=mix(vec3(.34,.04,.92),vec3(.88,.58,1),body);color=mix(color,vec3(1,.92,1),core*.76+needle*.72+filament*.32);gl_FragColor=vec4(color*(1.30+core*1.32+needle*2.10),clamp(opacity,0.0,1.0));return;}
 if(kind<2.5){float band=1.0-smoothstep(.10,1.0,across),core=1.0-smoothstep(0.0,.22,across),rune=pow(max(0.0,sin(u*92.0+time*.32)),10.0)*(1.0-across),broken=smoothstep(.22,.86,noise(vec2(u*20.0+time*.12,v*5.0)));float opacity=alpha*(band*.38+core*.52+rune*.30)*(.72+broken*.28);if(opacity<.004)discard;vec3 color=mix(vec3(.46,.12,1),vec3(1,.84,1),core*.70+rune*.70);gl_FragColor=vec4(color*(1.0+core*1.15+rune*1.40),clamp(opacity,0.0,1.0));return;}
 if(kind<3.5){vec2 p=vec2(u*2.0-1.0,v*2.0-1.0);float r=length(p),disc=1.0-smoothstep(.20,1.0,r),ring=smoothstep(.28,.44,r)*(1.0-smoothstep(.44,.72,r)),cross=1.0-smoothstep(0.0,.060,min(abs(p.x),abs(p.y))),diag=1.0-smoothstep(0.0,.055,min(abs(p.x+p.y),abs(p.x-p.y))),rays=max(cross,diag)*(1.0-smoothstep(.20,1.08,r)),sparkle=pow(max(0.0,sin((p.x-p.y)*18.0+time*1.6)),8.0)*disc;float opacity=alpha*(disc*.22+ring*.68+rays*.92+sparkle*.34);if(opacity<.004)discard;vec3 color=mix(vec3(.60,.16,1),vec3(1,.90,1),ring*.58+rays*.78+sparkle);gl_FragColor=vec4(color*(1.16+rays*1.55+sparkle*1.10),clamp(opacity,0.0,1.0));return;}
 if(kind<4.5){vec2 p=vec2(u*2.0-1.0,v*2.0-1.0);float d=abs(p.x)*.66+abs(p.y),body=1.0-smoothstep(.74,1.08,d),edge=smoothstep(.42,.72,d)*(1.0-smoothstep(.72,1.08,d)),core=1.0-smoothstep(0.0,.34,d),glint=pow(max(0.0,sin((p.x-p.y)*20.0+time*1.8)),10.0),opacity=alpha*body*(edge*.82+core*.36+glint*.42);if(opacity<.004)discard;vec3 color=mix(vec3(.44,.10,1),vec3(.92,.62,1),edge+core*.30);color=mix(color,vec3(1,.92,1),core*.50+glint*.78);gl_FragColor=vec4(color*(1.08+edge*.84+core*1.22+glint*1.50),clamp(opacity,0.0,1.0));return;}
 discard;
}
