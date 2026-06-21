#version 120

uniform float u_radius;
uniform float u_kernel[128];
uniform sampler2D u_diffuse_sampler;
uniform sampler2D u_other_sampler;
uniform vec2 u_texel_size;
uniform vec2 u_direction;

void main() {
    vec2 texCoord = gl_TexCoord[0].st;
    vec4 color = texture2D(u_diffuse_sampler, texCoord) * u_kernel[0];
    
    for (int i = 1; i < int(u_radius); i++) {
        vec2 offset = u_direction * u_texel_size * float(i);
        color += texture2D(u_diffuse_sampler, texCoord + offset) * u_kernel[i];
        color += texture2D(u_diffuse_sampler, texCoord - offset) * u_kernel[i];
    }
    
    gl_FragColor = color;
}
