#version 300 es
      precision highp float;
      uniform sampler2D u_image;
      uniform sampler2D u_previous;
      uniform float u_transition;
      uniform vec4 u_transitionRect;
      uniform vec2 u_imageSize;
      uniform vec2 u_coverScale;
      uniform float u_aspect;
      uniform float u_turn;
      uniform float u_phase;
      uniform vec3 u_blurAngle;
      uniform vec3 u_blurSpread;
      uniform vec3 u_blackGradient;
      in vec2 v_uv;
      out vec4 outColor;

      vec2 imageCoordinates(vec2 uv) {
        return vec2(0.5) + (uv - vec2(0.5)) * u_coverScale;
      }

      vec3 sceneSample(vec2 uv, float lod) {
        vec3 current = textureLod(u_image, uv, lod).rgb;
        // Audience snapshots must never crossfade the poll or other live widgets.
        if (u_transition >= 1.0 || any(lessThan(uv, u_transitionRect.xy)) || any(greaterThan(uv, u_transitionRect.zw))) return current;
        return mix(textureLod(u_previous, uv, lod).rgb, current, u_transition);
      }

      vec3 sampleScreen(vec2 uv, float sigma) {
        uv = imageCoordinates(uv);
        // Centered cover in browser mode; full texture UVs in fullscreen.
        // Each mip contains an actual separable Gaussian blur, prepared once
        // on image upload. Blend levels per pixel in stationary-image space.
        float lod = max(0.0, log2(max(sigma, 1.0)));
        vec3 blurred = sceneSample(uv, max(1.0, lod));
        if (sigma >= 2.0) return blurred;
        return mix(sceneSample(uv, 0.0), blurred, smoothstep(0.0, 2.0, sigma));
      }

      void main() {
        float pixelY = fwidth(v_uv.y);
        vec3 dark = vec3(0.003, 0.004, 0.005);
        float turn = clamp(u_turn, 0.0, 1.0);
        if (turn <= 0.00001) {
          outColor = vec4(sceneSample(imageCoordinates(v_uv), 0.0), 1.0);
          return;
        }

        // The canvas IS the physical display. Project each display pixel into
        // a stationary image plane and sample there. Do not project a smaller
        // screen into this canvas: foreshortening happens on the real device.
        float outer = step(0.5, u_phase);
        float hinge = mix(1.0, 0.0, outer);
        float fromHinge = abs(v_uv.x - hinge);
        // Each 180-degree demo phase takes one face from frontal to edge-on.
        float tilt = turn * 1.570796327;
        float cosine = max(0.0, cos(tilt));
        float sine = sin(tilt);

        // Keep the observer at the hinge's vertical midpoint so the top and
        // bottom perspective margins are symmetric. Distances are in screen
        // heights; the divide compensates depth and horizontal foreshortening.
        float eyeDistance = 2.4 * max(u_aspect, 1.0);
        float depth = fromHinge * u_aspect * sine;
        float perspective = eyeDistance / (eyeDistance - depth);
        vec2 imageUv;
        imageUv.x = hinge + (v_uv.x - hinge) * cosine * perspective;
        imageUv.y = 0.5 + (v_uv.y - 0.5) * perspective;

        // The whole width stays painted. Near edge-on, a narrow image strip is
        // stretched over ALL display pixels; the observer sees it compressed
        // back to its original scale. Only the image's top/bottom bounds can
        // expose black, producing the reference's perspective-shaped margins.
        // Angle: maximum blur, physical angle at full blur, response exponent.
        // Spread: distance to full blur, response exponent, blur at the hinge.
        float blurAngle = pow(smoothstep(0.0, radians(u_blurAngle.y), tilt), u_blurAngle.z);
        float blurSpread = pow(smoothstep(0.0, u_blurSpread.x, fromHinge), u_blurSpread.y);
        float defocus = blurAngle * mix(u_blurSpread.z, 1.0, blurSpread);
        float sigma = u_imageSize.x * u_blurAngle.x * defocus;
        float marginSoftness = pixelY + 2.0 * sigma / u_imageSize.y;
        float verticalMask = 1.0 - smoothstep(0.5 - marginSoftness, 0.5 + marginSoftness, abs(imageUv.y - 0.5));
        float glass = sine * pow(fromHinge, 1.6);
        vec3 content = sampleScreen(imageUv, sigma);
        content *= 1.0 - mix(0.28, 0.06, outer) * glass;
        float reflection = exp(-pow((fromHinge - 0.70) / 0.30, 2.0)) * sine;
        content += vec3(0.82, 0.85, 0.86) * reflection * 0.025;
        // Linear black fade toward the blurred edge; clears with the blur angle.
        float blackFade = clamp((fromHinge - u_blackGradient.y) / max(u_blackGradient.z - u_blackGradient.y, 0.001), 0.0, 1.0);
        content *= 1.0 - u_blackGradient.x * blurAngle * blackFade;
        outColor = vec4(mix(dark, content, verticalMask), 1.0);
      }
