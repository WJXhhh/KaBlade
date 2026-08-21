#version 120
uniform sampler2D Source;
uniform vec2 TexelSize;
uniform vec2 Direction;
varying vec2 vUv;
void main(){vec2 d=TexelSize*Direction;vec4 c=texture2D(Source,vUv)*.227027;c+=(texture2D(Source,vUv+d*1.384615)+texture2D(Source,vUv-d*1.384615))*.316216;c+=(texture2D(Source,vUv+d*3.230769)+texture2D(Source,vUv-d*3.230769))*.070270;gl_FragColor=c;}
