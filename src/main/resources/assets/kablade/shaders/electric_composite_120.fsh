#version 120
uniform sampler2D Scene;
uniform sampler2D Effect;
uniform sampler2D Bloom;
uniform vec2 TexelSize;
uniform vec2 UvScale;
varying vec2 vUv;
void main(){
    vec2 uv=vUv*UvScale;
    vec4 fx=texture2D(Effect,uv);
    vec3 bloom=texture2D(Bloom,uv).rgb;
    float energy=clamp(dot(bloom,vec3(0.2126,0.7152,0.0722))*1.8,0.0,1.0);
    vec2 radial=vUv-vec2(0.5);
    vec2 warp=radial*energy*0.012;
    vec2 split=normalize(radial+vec2(0.0001))*TexelSize*(1.5+energy*5.0);
    vec2 sceneUv=(vUv-warp)*UvScale;
    vec3 scene;
    scene.r=texture2D(Scene,sceneUv+split).r;
    scene.g=texture2D(Scene,sceneUv).g;
    scene.b=texture2D(Scene,sceneUv-split).b;
    gl_FragColor=vec4(scene+fx.rgb*fx.a+bloom*1.18,1.0);
}
