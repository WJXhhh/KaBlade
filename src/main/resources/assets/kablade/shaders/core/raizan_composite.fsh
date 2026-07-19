#version 150
uniform vec4 ColorModulator; uniform float GameTime;
in vec4 vertexColor; in vec2 texCoord0; out vec4 fragColor;
void main(){
 vec2 p=texCoord0*2.0-1.0; float d=length(p);
 float halo=1.0-smoothstep(0.08,1.0,d); float core=1.0-smoothstep(0.0,0.20,d);
 float ring=1.0-smoothstep(0.02,0.12,abs(d-0.52));
 float pulse=0.84+0.16*sin(GameTime*240.0+d*19.0);
 float alpha=vertexColor.a*(halo*0.56+ring*0.72+core)*pulse*ColorModulator.a;
 if(alpha<0.004)discard;
 vec3 color=mix(vertexColor.rgb,vec3(1.0),core*0.92)*(0.86+core*1.78+ring*0.55);
 fragColor=vec4(color*ColorModulator.rgb,clamp(alpha,0.0,1.0));
}
