#version 150
uniform vec4 ColorModulator; uniform float GameTime; uniform sampler2D Sampler0;
in vec4 vertexColor; in vec2 texCoord0; out vec4 fragColor;
float hash12(vec2 p){
 vec3 p3=fract(vec3(p.xyx)*vec3(0.1031,0.1030,0.0973));
 p3+=dot(p3,p3.yzx+33.33); return fract((p3.x+p3.y)*p3.z);
}
float noise2(vec2 p){
 vec2 i=floor(p),f=fract(p); f=f*f*(3.0-2.0*f);
 return mix(mix(hash12(i),hash12(i+vec2(1,0)),f.x),
            mix(hash12(i+vec2(0,1)),hash12(i+vec2(1)),f.x),f.y);
}
void main(){
 float across=abs(texCoord0.y*2.0-1.0);
 float halo=1.0-smoothstep(0.28,1.0,across);
 float core=1.0-smoothstep(0.0,0.16,across);
 float axial=noise2(vec2(texCoord0.x*5.2-GameTime*19.0,across*3.1));
 float fine=noise2(vec2(texCoord0.x*17.0-GameTime*41.0,texCoord0.y*7.0));
 float tiled=texture(Sampler0,fract(vec2(texCoord0.x*0.72-GameTime*1.7,
                                       texCoord0.y*0.84+GameTime*0.31))).r;
 axial=mix(axial,tiled,0.58);
 float packet=smoothstep(0.54,0.92,axial)*0.34;
 float flow=0.78+axial*0.24+fine*0.10+packet;
 float pulse=0.82+0.18*sin(GameTime*390.0+texCoord0.x*11.0);
 float edgeErode=smoothstep(0.18,0.72,fine+halo*0.38);
 float alpha=vertexColor.a*(halo*0.54*edgeErode+core)*flow*ColorModulator.a;
 if(alpha<0.004)discard;
 vec3 color=mix(vertexColor.rgb,vec3(0.96,1.0,1.0),core*0.86)*(0.82+core*1.25)*pulse;
 fragColor=vec4(color*ColorModulator.rgb,clamp(alpha,0.0,1.0));
}
