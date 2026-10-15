#version 150

uniform sampler2D DiffuseSampler;
uniform vec3 TintColor;
uniform float Strength;

in vec2 texCoord;
out vec4 fragColor;

// 转阶段全屏主题色浸染：保持亮度结构向阶段色偏移 + 轻微暗角聚焦，
// Strength 控制强度（转阶段时最高，随时间线性渐退）。
void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 tinted = mix(color.rgb, TintColor * (0.22 + 0.9 * lum), clamp(Strength, 0.0, 1.0) * 0.8);
    vec2 uv = texCoord - 0.5;
    float vig = 1.0 - dot(uv, uv) * 0.7;
    tinted *= mix(1.0, vig, clamp(Strength, 0.0, 1.0));
    fragColor = vec4(tinted, color.a);
}
