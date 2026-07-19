#version 150
uniform vec4 ColorModulator; uniform float GameTime; uniform sampler2D Sampler0;
in vec4 vertexColor; in vec2 texCoord0; out vec4 fragColor;
float hash12(vec2 p){
 vec3 p3=fract(vec3(p.xyx)*vec3(0.1031,0.1030,0.0973));
 p3+=dot(p3,p3.yzx+31.32); return fract((p3.x+p3.y)*p3.z);
}
void main(){
 vec2 p=texCoord0*2.0-1.0;
 float diamond=1.0-smoothstep(0.55,1.0,abs(p.x)+abs(p.y));
 float core=1.0-smoothstep(0.0,0.24,length(p));
 float grain=hash12(floor(texCoord0*19.0)+vec2(floor(GameTime*7.0)));
 float particleMap=texture(Sampler0,fract(texCoord0+vec2(GameTime*0.07,-GameTime*0.05))).r;
 float pulse=0.76+0.24*sin(GameTime*191.0+texCoord0.x*17.0+grain);
 float dissolve=smoothstep(0.08,0.42+vertexColor.a*0.46,
                          grain*0.44+particleMap*0.56+diamond*0.46);
 float alpha=vertexColor.a*max(diamond*dissolve,core)*pulse*ColorModulator.a;
 if(alpha<0.004)discard;
 vec3 color=mix(vertexColor.rgb,vec3(1.0),core*0.84)*(0.86+core*1.24);
 fragColor=vec4(color*ColorModulator.rgb,clamp(alpha,0.0,1.0));
}
