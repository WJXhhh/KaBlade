#version 150
uniform vec4 ColorModulator; uniform float GameTime; uniform sampler2D Sampler0;
in vec4 vertexColor; in vec2 texCoord0; out vec4 fragColor;
float hash12(vec2 p){
 vec3 p3=fract(vec3(p.xyx)*vec3(0.1031,0.1030,0.0973));
 p3+=dot(p3,p3.yzx+31.32); return fract((p3.x+p3.y)*p3.z);
}
void main(){
 float across=abs(texCoord0.y*2.0-1.0);
 float shell=pow(max(0.0,1.0-across),1.48);
 float core=1.0-smoothstep(0.0,0.22,across);
 float spatial=hash12(floor(vec2(texCoord0.x*23.0,texCoord0.y*5.0)));
 float crawl=texture(Sampler0,fract(vec2(texCoord0.x*0.45-GameTime*0.9,
                                       texCoord0.y*1.8+spatial*0.13))).r;
 float flicker=0.94+0.06*sin(GameTime*113.0+spatial*2.4);
 float flow=0.84+0.08*sin(texCoord0.x*18.8495-GameTime*34.0+spatial)+crawl*0.12;
 float alpha=vertexColor.a*(shell*0.98+core*0.82)*flicker*flow*ColorModulator.a;
 if(alpha<0.001)discard;
 vec3 color=mix(vertexColor.rgb,vec3(0.99,0.97,1.0),core*0.82)*(1.02+core*1.42);
 fragColor=vec4(color*ColorModulator.rgb,clamp(alpha,0.0,1.0));
}
