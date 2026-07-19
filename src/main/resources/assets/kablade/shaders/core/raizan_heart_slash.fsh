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
 float halo=pow(max(0.0,1.0-across),1.42);
 float core=1.0-smoothstep(0.0,0.16,across);
 float coarse=noise2(vec2(texCoord0.x*8.0-GameTime*7.0,texCoord0.y*3.0));
 float teeth=noise2(vec2(texCoord0.x*31.0-GameTime*15.0,texCoord0.y*9.0));
 float slashMap=texture(Sampler0,vec2(fract(texCoord0.x*0.82-GameTime*0.42),
                                      clamp(texCoord0.y,0.0,1.0))).r;
 float serration=0.74+coarse*0.22+teeth*0.16;
 float write=smoothstep(0.0,0.055,texCoord0.x)*(1.0-smoothstep(0.94,1.0,texCoord0.x));
 float erodedEdge=smoothstep(0.12,0.78,teeth+halo*0.34);
 float material=mix(0.52,1.18,slashMap);
 float alpha=vertexColor.a*(halo*0.72*erodedEdge+core*0.96)*serration*material*write*ColorModulator.a;
 if(alpha<0.004)discard;
 // Keep red slash bodies saturated. Explicit white-core geometry still reaches white,
 // while secondary red frames no longer all collapse into identical white threads.
 vec3 chroma=mix(vertexColor.rgb,vec3(1.0,0.96,1.0),core*0.52);
 chroma+=vec3(0.18,-0.02,0.24)*(1.0-core)*(coarse*2.0-1.0);
 fragColor=vec4(max(chroma,vec3(0.0))*ColorModulator.rgb*(0.96+core*1.52),clamp(alpha,0.0,1.0));
}
