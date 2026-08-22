#version 120
uniform float uTime;
uniform int uMaterial;
varying vec2 vUv;
varying vec4 vColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){
    vec2 p=vUv*2.0-1.0;
    float across=abs(p.y);
    float core=1.0-smoothstep(0.0,0.18,across);
    float edge;
    if(uMaterial==4){
        // 重磁暴：与 1.20 raiden_cyclone 一致的横向软边和首尾收束。
        float softEdge=1.0-smoothstep(0.48,1.0,across);
        float taper=smoothstep(0.0,0.10,vUv.x)*(1.0-smoothstep(0.88,1.0,vUv.x));
        edge=softEdge*taper;
    }else if(uMaterial==2){
        // 天殛主斩面：宽光晕仍必须在几何边界处归零。
        float halo=pow(max(0.0,1.0-across),1.42);
        float taper=smoothstep(0.0,0.055,vUv.x)*(1.0-smoothstep(0.94,1.0,vUv.x));
        edge=(halo*0.72+core*0.96)*taper;
    }else if(uMaterial==3){
        // 粒子/冲击核心使用菱形遮罩，不能显示粒子四边形底板。
        float diamond=1.0-smoothstep(0.55,1.0,abs(p.x)+abs(p.y));
        float radial=1.0-smoothstep(0.0,0.24,length(p));
        edge=max(diamond,radial);
    }else if(uMaterial==1){
        // 雷电保留连续端点，仅在宽度方向完全淡出。
        float shell=pow(max(0.0,1.0-across),1.48);
        edge=min(1.0,shell*0.98+core*0.82);
    }else{
        float halo=1.0-smoothstep(0.28,1.0,across);
        edge=min(1.0,halo*0.54+core);
    }
    if(edge<=0.003)discard;
    float flicker=0.92+0.08*sin(uTime*0.31+hash(floor(vUv*23.0))*6.2831);
    vec3 color=vColor.rgb;
    if(uMaterial==0) color*=vec3(0.82,0.92,1.18);
    else if(uMaterial==1) color=mix(color,vec3(0.76,0.38,1.0),0.18);
    else if(uMaterial==2) color=mix(color,vec3(1.0,0.30,0.82),0.10);
    else if(uMaterial==3) color=mix(color,vec3(0.72,0.96,1.0),0.14);
    else color=mix(color,vec3(0.18,0.82,1.0),0.10);
    gl_FragColor=vec4(color*(0.86+core*0.72)*flicker,vColor.a*edge);
}
