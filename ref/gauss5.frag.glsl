#version 300 es
precision highp float;
in vec2 v_uv;
uniform sampler2D u_wallpaper;
uniform sampler2D u_overlay;
uniform vec2 u_cover;
uniform vec2 u_sceneSize;
uniform vec4 u_dock;
uniform float u_home;
out vec4 outColor;
void main() {
  vec2 wallpaperUv = (v_uv - 0.5) * u_cover + 0.5;
  vec3 background = texture(u_wallpaper, wallpaperUv).rgb;
  if (u_home < 0.5) { outColor = vec4(background, 1.0); return; }
  vec2 pixel = vec2(v_uv.x, 1.0 - v_uv.y) * u_sceneSize;
  float radius = u_dock.w * 0.47;
  vec2 q = abs(pixel - (u_dock.xy + u_dock.zw * 0.5)) - (u_dock.zw * 0.5 - radius);
  float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
  float mask = 1.0 - smoothstep(-1.0, 1.0, distance);
  if (distance < 2.0) {
    // Frosted wallpaper, sampled in screen space so blur remains isotropic.
    vec3 blurred = vec3(0.0); float weight = 0.0;
    for (int y = -2; y <= 2; y++) for (int x = -2; x <= 2; x++) {
      vec2 offset = vec2(float(x), float(y));
      float w = exp(-dot(offset, offset) / 3.0);
      blurred += textureLod(u_wallpaper, wallpaperUv + offset * (u_dock.w * 0.10) / u_sceneSize * u_cover, 2.0).rgb * w;
      weight += w;
    }
    blurred /= weight;
    float luminance = dot(blurred, vec3(0.2126, 0.7152, 0.0722));
    vec3 glass = mix(vec3(luminance), blurred, 1.25);
    glass = mix(glass, vec3(0.94, 0.96, 1.0), 0.22);
    background = mix(background, glass, mask);
    float rim = (1.0 - smoothstep(0.5, 1.7, abs(distance))) * 0.26;
    background = mix(background, vec3(1.0), rim);
  }
  vec4 overlay = texture(u_overlay, v_uv);
  outColor = vec4(mix(background, overlay.rgb, overlay.a), 1.0);
}
