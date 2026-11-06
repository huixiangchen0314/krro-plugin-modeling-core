package top.kzre.krro.plugin.modeling.core.viewport;

import java.util.Arrays;

/**
 * 视口数值计算。纯函数，无状态。
 *
 * <p>接收标量参数，输出写入调用方提供的 {@code float[16]}（列主序，OpenGL 兼容）。
 * 不依赖 Clojure 数据结构——Clojure 侧的 viewport->view-mat4 负责解构 map 后调用这里。
 *
 * <p>矩阵约定：列主序，m[12..14] 为平移列。右手系，Y-up，相机朝向 -Z。
 *
 * <p><b>相机永不倒置</b>：elevation 被 clamp 在 (-π/2, π/2) 内，
 * 不存在越过极点的状态，因此不需要 flipped 参数，right/up 恒用标准公式。
 */
public final class ViewportMath {
    private ViewportMath() {
    }

    /**
     * 仰角钳制的绝对值上限（π/2 - ε）。spec 里的 ::elevation 范围
     * 与 orbitAnglesClamped 的钳制共用这一个常量，避免两处硬编码不一致。
     */
    public static final double ELEVATION_LIMIT = Math.PI / 2 - 1e-6;

    /**
     * 视图矩阵（世界空间 → 相机空间），写入 out。
     *
     * <pre>
     * orbit 相机模型：
     *   eye   = target + distance * (cosE·sinA, sinE, cosE·cosA)
     *   right = (cosA, 0, -sinA)
     *   up    = (-sinA·sinE, cosE, -cosA·sinE)
     *   fwd   = (-cosE·sinA, -sinE, -cosE·cosA)
     *
     * 视图矩阵：
     *   m[0]=right.x  m[4]=right.y  m[8] =right.z  m[12]=-dot(right, eye)
     *   m[1]=up.x     m[5]=up.y     m[9] =up.z     m[13]=-dot(up,    eye)
     *   m[2]=-fwd.x   m[6]=-fwd.y   m[10]=-fwd.z   m[14]= dot(fwd,   eye)
     *   m[3]=0        m[7]=0        m[11]=0        m[15]=1
     * </pre>
     *
     * <p>前置条件（由 Clojure 侧 spec 保证，本方法不重复检查）：
     * distance &gt; 0；elevation 严格在 (-π/2, π/2) 内；out.length &gt;= 16。
     *
     * @param tx        注视点 x
     * @param ty        注视点 y
     * @param tz        注视点 z
     * @param distance  相机到注视点的距离
     * @param azimuth   绕 Y 轴方位角，弧度
     * @param elevation 仰角，弧度
     * @param out       输出数组，长度 &gt;= 16
     */
    public static void viewMat4(
            double tx, double ty, double tz,
            double distance,
            double azimuth, double elevation,
            float[] out) {
        final double cosA = Math.cos(azimuth);
        final double sinA = Math.sin(azimuth);
        final double cosE = Math.cos(elevation);
        final double sinE = Math.sin(elevation);

        final double ex = tx + distance * cosE * sinA;
        final double ey = ty + distance * sinE;
        final double ez = tz + distance * cosE * cosA;

        final double rx = cosA,          ry = 0.0,     rz = -sinA;
        final double ux = -sinA * sinE,  uy = cosE,    uz = -cosA * sinE;
        final double fx = -cosE * sinA,  fy = -sinE,   fz = -cosE * cosA;

        final double m12 = -(rx * ex + ry * ey + rz * ez);
        final double m13 = -(ux * ex + uy * ey + uz * ez);
        final double m14 =  (fx * ex + fy * ey + fz * ez);

        Arrays.fill(out, 0.0f);
        out[0]  = (float) rx;
        out[1]  = (float) ux;
        out[2]  = (float) (-fx);
        out[4]  = (float) ry;
        out[5]  = (float) uy;
        out[6]  = (float) (-fy);
        out[8]  = (float) rz;
        out[9]  = (float) uz;
        out[10] = (float) (-fz);
        out[12] = (float) m12;
        out[13] = (float) m13;
        out[14] = (float) m14;
        out[15] = 1.0f;
    }

    /**
     * 透视投影矩阵（相机空间 → 裁剪空间），写入 out。
     *
     * <pre>
     *   f = 1 / tan(fovY / 2)
     *   m[0]  = f / aspect
     *   m[5]  = f
     *   m[10] = (far + near) / (near - far)
     *   m[11] = -1
     *   m[14] = 2·far·near / (near - far)
     *   m[15] = 0
     * </pre>
     *
     * <p>前置条件：fovY 在 (0, π) 内；near &gt; 0；near &lt; far；aspect &gt; 0。
     *
     * @param fovY   垂直视角，弧度
     * @param near   近裁剪面
     * @param far    远裁剪面
     * @param aspect 宽高比（宽 / 高）
     * @param out    输出数组，长度 &gt;= 16
     */
    public static void perspectiveProjMat4(
            double fovY, double near, double far, double aspect,
            float[] out) {
        final double f = 1.0 / Math.tan(fovY * 0.5);
        final double nf = 1.0 / (near - far);

        Arrays.fill(out, 0.0f);
        out[0] = (float) (f / aspect);
        out[5] = (float) f;
        out[10] = (float) ((far + near) * nf);
        out[11] = -1.0f;
        out[14] = (float) (2.0 * far * near * nf);
        // out[15] = 0
    }

    /**
     * 正交投影矩阵（相机空间 → 裁剪空间），写入 out。
     *
     * <pre>
     * orthoScale 是视野半高：
     *   m[0]  = 1 / (orthoScale · aspect)
     *   m[5]  = 1 / orthoScale
     *   m[10] = -2 / (far - near)
     *   m[14] = -(far + near) / (far - near)
     *   m[15] = 1
     * </pre>
     *
     * <p>前置条件：orthoScale &gt; 0；near &lt; far；aspect &gt; 0。
     * 注意 near 可为负（正交允许相机后方内容可见）。
     *
     * @param orthoScale 视野半高（世界单位）
     * @param near       近裁剪面
     * @param far        远裁剪面
     * @param aspect     宽高比（宽 / 高）
     * @param out        输出数组，长度 &gt;= 16
     */
    public static void orthographicProjMat4(
            double orthoScale, double near, double far, double aspect,
            float[] out) {
        final double fn = 1.0 / (far - near);

        Arrays.fill(out, 0.0f);
        out[0] = (float) (1.0 / (orthoScale * aspect));
        out[5] = (float) (1.0 / orthoScale);
        out[10] = (float) (-2.0 * fn);
        out[14] = (float) (-(far + near) * fn);
        out[15] = 1.0f;
    }

    /**
     * 列主序 4x4 viewProj。相机在 (0, 0, eyeZ) 看向 -Z。
     */
    public static float[] aspectViewProjMat4(int w, int h, double fovYDeg,
                                             float near, float far, float eyeZ) {
        float f      = (float) (1.0 / Math.tan(0.5 * Math.PI * (fovYDeg / 180.0)));
        float aspect = (float) w / h;
        float nf     = near - far;
        float a      = (far + near) / nf;
        float b      = (2.0f * far * near) / nf;

        return new float[] {
                f / aspect, 0f, 0f, 0f,
                0f,         f,  0f, 0f,
                0f,         0f, a,  -1f,
                0f,         0f, a * eyeZ + b, -eyeZ
        };
    }

    // ========================================================================
    // 平移（pan）辅助
    // ========================================================================

    /**
     * 透视模式下，屏幕每像素对应的世界空间长度（在注视点所在平面上）。
     *
     * <pre>
     * 注视点平面上，视口高度 H 像素覆盖的世界高度为：
     *   worldHeight = 2 · distance · tan(fovY / 2)
     * 每像素世界长度：
     *   worldPerPixel = worldHeight / H
     *                 = 2 · distance · tan(fovY / 2) / H
     * </pre>
     *
     * <p>水平方向每像素的世界长度与垂直相同：aspect 在水平/垂直两边约掉，
     * 故返回单个标量，x/y 位移共用。
     *
     * <p>前置条件：distance &gt; 0；fovY 在 (0, π) 内；viewportHeight &gt; 0。
     * 调用方负责保证 viewportHeight 非零（视口未就绪时不应触发 pan）。
     *
     * @param distance       相机到注视点的距离
     * @param fovY           垂直视角，弧度
     * @param viewportHeight 视口高度，像素
     * @return 每像素世界长度
     */
    public static double perspectiveWorldPerPixel(
            double distance, double fovY, int viewportHeight) {
        return 2.0 * distance * Math.tan(fovY * 0.5) / viewportHeight;
    }

    /**
     * 正交模式下，屏幕每像素对应的世界空间长度。
     *
     * <pre>
     * orthoScale 是视野半高，视口高度 H 像素覆盖的世界高度为：
     *   worldHeight = 2 · orthoScale
     * 每像素世界长度：
     *   worldPerPixel = 2 · orthoScale / H
     * </pre>
     *
     * <p>前置条件：orthoScale &gt; 0；viewportHeight &gt; 0。
     *
     * @param orthoScale     视野半高（世界单位）
     * @param viewportHeight 视口高度，像素
     * @return 每像素世界长度
     */
    public static double orthographicWorldPerPixel(
            double orthoScale, int viewportHeight) {
        return 2.0 * orthoScale / viewportHeight;
    }

    /**
     * 屏幕位移 → 世界空间平移量，用于 pan。结果写入 outDelta（长度 &gt;= 3）。
     *
     * <p>内容跟随鼠标（抓住世界拖动）语义：鼠标右拖，内容右移；鼠标下拖，
     * 内容下移。相机沿反方向移动。
     *
     * <pre>
     * 相机基向量（与 viewMat4 一致）：
     *   right = ( cosA,      0,    -sinA     )
     *   up    = (-sinA·sinE, cosE, -cosA·sinE )
     *
     * 屏幕位移 (dx, dy)（像素，dy 向下），世界位移：
     *   delta = right · (-dx · wpp) + up · (dy · wpp)
     * </pre>
     *
     * <p>前置条件：elevation 严格在 (-π/2, π/2) 内（spec 保证）；outDelta.length &gt;= 3。
     *
     * @param azimuth       绕 Y 轴方位角，弧度
     * @param elevation     仰角，弧度
     * @param dx            屏幕 x 位移，像素
     * @param dy            屏幕 y 位移，像素（向下为正）
     * @param worldPerPixel 每像素世界长度（由上面的 wpp 方法给出）
     * @param outDelta      输出数组，长度 &gt;= 3
     */
    public static void panDelta(
            double azimuth, double elevation,
            double dx, double dy, double worldPerPixel,
            float[] outDelta) {
        final double cosA = Math.cos(azimuth);
        final double sinA = Math.sin(azimuth);
        final double cosE = Math.cos(elevation);
        final double sinE = Math.sin(elevation);

        final double sx = -dx * worldPerPixel;
        final double sy =  dy * worldPerPixel;

        outDelta[0] = (float) (cosA * sx + (-sinA * sinE) * sy);
        outDelta[1] = (float) (cosE * sy);
        outDelta[2] = (float) (-sinA * sx + (-cosA * sinE) * sy);
    }

    /**
     * 轨道旋转：极点钳制，永不越过。
     *
     * <p>拖拽映射是世界绝对的，不依赖任何翻转状态：
     * 水平拖拽始终改变 azimuth，垂直拖拽始终改变 elevation。
     * elevation 被 clamp 在 (-ELEVATION_LIMIT, ELEVATION_LIMIT) 内——
     * 到极点停住，不反射、不翻转、不摇摆。
     *
     * <p>前置条件：sensitivity &gt; 0；out.length &gt;= 2。
     *
     * @param azimuth   当前方位角，弧度
     * @param elevation 当前仰角，弧度
     * @param dx        屏幕 x 位移，像素
     * @param dy        屏幕 y 位移，像素（向下为正）
     * @param sensitivity 拖拽灵敏度，弧度/像素
     * @param out       输出 [azimuth, elevation]，长度 &gt;= 2
     */
    public static void orbitAnglesClamped(
            double azimuth, double elevation,
            double dx, double dy,
            double sensitivity,
            float[] out) {
        azimuth   -= dx * sensitivity;
        elevation += dy * sensitivity;
        // Clamp，不允许越过极点
        if (elevation >  ELEVATION_LIMIT) elevation =  ELEVATION_LIMIT;
        if (elevation < -ELEVATION_LIMIT) elevation = -ELEVATION_LIMIT;
        out[0] = (float) azimuth;
        out[1] = (float) elevation;
    }

    // ========================================================================
    // 缩放（dolly）辅助
    // ========================================================================

    /**
     * 指数缩放（dolly）。value' = max(value · exp(-delta · k), minValue)。
     *
     * <p>delta 的语义：正值表示放大（value 减小），负值表示缩小（value 增大）。
     * 调用方负责把不同输入的符号统一到「正 = 放大」：
     * <ul>
     *   <li>滚轮：delta = wheelDelta（约定向上滚为正）</li>
     *   <li>拖拽：delta = -dy（屏幕 y 向下为正，向上拖 dy&lt;0，取负得正 = 放大）</li>
     * </ul>
     *
     * <p>指数映射而非线性：缩放手感在尺度上均匀，不会因为当前 distance
     * 大小导致快慢不一。每次缩放量按比例计算。
     *
     * <p>钳制：value 不能小于 minValue，防止 distance 退化到 0 导致视图矩阵
     * 奇异，或 ortho-scale 退化到 0 导致投影矩阵奇异。
     *
     * <p>前置条件：value &gt; 0；minValue &gt; 0。
     *
     * @param value    当前值（distance 或 ortho-scale）
     * @param delta    缩放增量，正 = 放大
     * @param k        灵敏度，每单位 delta 的指数系数
     * @param minValue 钳制下限，必须为正
     * @return 缩放后的值
     */
    public static double dollyScaled(double value, double delta, double k, double minValue) {
        double scaled = value * Math.exp(-delta * k);
        return Math.max(scaled, minValue);
    }
}