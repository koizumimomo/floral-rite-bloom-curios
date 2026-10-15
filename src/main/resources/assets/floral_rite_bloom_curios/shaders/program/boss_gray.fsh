#version 150

uniform sampler2D DiffuseSampler;
uniform float Strength;

in vec2 texCoord;
out vec4 fragColor;

// 枯萎/时停全屏褪色：亮度加权灰度 + 暗化处理，Strength 控制混合比例（0=原色 1=完全灰）。
// r98：灰度再暗化 35%，让时停氛围更压抑。
void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float g = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 withered = vec3(g) * vec3(0.60, 0.62, 0.60);
    fragColor = vec4(mix(color.rgb, withered, clamp(Strength, 0.0, 1.0)), color.a);
}
