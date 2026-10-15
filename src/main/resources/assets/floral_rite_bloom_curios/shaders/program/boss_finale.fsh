#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

// 由 Java 侧传入的真实投影-视图矩阵（RenderLevelStageEvent 的 ProjectionMatrix × PoseStack 视图矩阵）
uniform mat4 InverseViewProjection;
uniform mat4 ViewProjection;

uniform vec2 SceneSize;       // 屏幕分辨率
uniform vec3 CenterPos;       // boss 世界坐标（扭曲球心）
uniform vec3 CameraPos;       // 相机世界坐标（射线起点）
uniform float SphereRadius;   // 扭曲球体半径
uniform float iTime;
uniform float Visibility;     // 整体可见度（0~1）

in vec2 texCoord;
out vec4 fragColor;

// 反投影：屏幕 UV + 深度 → 世界坐标
vec3 unproject(vec2 uv, float depth) {
    vec4 p = InverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// 场景深度 → 到相机的距离。
//
// uvTex 必须是【采样空间】坐标（v = 0 在屏幕顶部，与 texCoord / DiffuseSampler 一致）。
// 反投影要的是【NDC 对齐空间】（y 向上），两套坐标差一个垂直镜像，所以这里单独翻一次 y。
// 修复前：调用方传的是 gl_FragCoord.xy / SceneSize（NDC 空间），却被同时用于
// texture(DepthSampler, ...) —— 深度取自屏幕上下镜像的那个像素，导致深度剔除
// 把每个像素都判成「几何体在球体前方」而提前 return，表现就是整屏完全没有扭曲。
float sceneDistance(vec2 uvTex) {
    float depth = texture(DepthSampler, uvTex).r;
    if (depth > 0.999999) return 100000.0;
    vec2 uvNdc = vec2(uvTex.x, 1.0 - uvTex.y);
    return length(unproject(uvNdc, depth) - CameraPos);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    // 两套屏幕坐标，别混用：
    //   uv    = 采样空间（texCoord，v = 0 在屏幕顶部）→ 所有 texture() 采样与 2D 偏移都用它
    //   uvNdc = NDC 对齐空间（y 向上）→ 只喂给 unproject() 做反投影
    vec2 uv = texCoord;
    vec2 uvNdc = gl_FragCoord.xy / SceneSize;

    // 1) 反投影得到相机射线方向：近平面点 → 远平面点
    vec3 nearPt = unproject(uvNdc, 0.0);
    vec3 farPt  = unproject(uvNdc, 1.0);
    vec3 ray = normalize(farPt - nearPt);

    // 2) 射线-球相交（球心 = boss 世界坐标，射线起点 = 相机）
    vec3 oc = CameraPos - CenterPos;
    float closest = dot(-oc, ray);  // 射线到球心最近点的距离
    if (closest <= 0.0) {           // 球在相机后方 → 不扭曲
        fragColor = texture(DiffuseSampler, uv);
        return;
    }
    float miss = length(ray * closest + oc); // 射线到球心的垂直距离
    if (miss >= SphereRadius) {    // 射线没命中球 → 不扭曲
        fragColor = texture(DiffuseSampler, uv);
        return;
    }
    // 球面前交点
    float front = max(0.0, closest - sqrt(max(SphereRadius * SphereRadius - miss * miss, 0.0)));

    // 3) 深度剔除：场景几何体在球体前方 → 不扭曲该像素
    if (sceneDistance(uv) < front) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    // 4) 用 ViewProjection 把球心投影到屏幕 UV
    vec4 projected = ViewProjection * vec4(CenterPos, 1.0);
    if (projected.w <= 0.0) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }
    vec2 centreUv = projected.xy / projected.w * 0.5 + 0.5;
    centreUv.y = 1.0 - centreUv.y;   // NDC 空间 → 采样空间，才能和 uv 相减得到正确的 radial

    // 5) 径向引力透镜扭曲 + 漩涡 + 噪声
    vec2 radial = (uv - centreUv);
    vec2 direction = radial / max(length(radial), 0.001);

    float falloff = pow(1.0 - miss / SphereRadius, 1.5);

    float pull = 0.55 * falloff;
    pull = min(pull, 0.35);

    float swirlAngle = iTime * 3.0 + miss * 8.0;
    vec2 swirlDir = vec2(direction.x * cos(swirlAngle) - direction.y * sin(swirlAngle),
                         direction.x * sin(swirlAngle) + direction.y * cos(swirlAngle));
    float swirl = 0.12 * falloff;

    float n = noise(uv * 50.0 + iTime * 0.8) * 0.6 + 0.4;

    vec2 offset = (direction * pull + swirlDir * swirl) * n;
    offset *= Visibility * falloff;

    vec2 sampleUv = clamp(uv - offset, vec2(0.001), vec2(0.999));

    if (sceneDistance(sampleUv) < front) sampleUv = uv;

    vec3 color = texture(DiffuseSampler, sampleUv).rgb;

    // 中心黑洞阴影 + 紫色光晕
    float coreDist = length(radial);
    float core = smoothstep(SphereRadius * 0.0015, 0.0, coreDist);
    float glow = smoothstep(SphereRadius * 0.004, 0.0, coreDist);
    color *= 1.0 - core * 0.85;
    color += vec3(0.45, 0.25, 0.65) * glow * 0.4 * Visibility;

    fragColor = vec4(color, 1.0);
}
