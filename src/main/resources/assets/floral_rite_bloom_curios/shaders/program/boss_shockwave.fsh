#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Strength;
uniform float iTime;

in vec2 texCoord;
out vec4 fragColor;

// Boss 开场 / 转阶段冲击波：径向挤压扭曲 + 轻微色散 + 波前泛光。
// Strength 由 Java 侧随剩余时间传入（1 -> 0），波环从屏幕中心向外扩散。
void main() {
    float aspect = max(0.0001, InSize.x / max(1.0, InSize.y));
    vec2 centered = texCoord - vec2(0.5);
    centered.x *= aspect;
    float dist = length(centered);
    vec2 dir = dist > 0.0001 ? centered / dist : vec2(0.0);

    float wave = (1.0 - Strength) * 1.55;
    float band = exp(-pow((dist - wave) * 5.5, 2.0));
    float ripple = sin(dist * 26.0 - iTime * 9.0) * 0.004;

    float amount = (band * 0.045 + ripple) * clamp(Strength * 2.0, 0.0, 1.0);
    vec2 offset = dir * amount / vec2(aspect, 1.0);

    vec2 uvG = clamp(texCoord - offset, vec2(0.001), vec2(0.999));
    vec2 uvR = clamp(texCoord - offset * 1.3, vec2(0.001), vec2(0.999));
    vec2 uvB = clamp(texCoord - offset * 0.7, vec2(0.001), vec2(0.999));

    vec4 color = texture(DiffuseSampler, uvG);
    color.r = texture(DiffuseSampler, uvR).r;
    color.b = texture(DiffuseSampler, uvB).b;
    color.rgb += vec3(1.0, 0.98, 0.9) * band * clamp(Strength, 0.0, 1.0) * 0.4;
    fragColor = color;
}
