package top.kzre.krro.plugin.modeling.core.viewport;

import java.util.Arrays;

/**
 * 视口数值计算。纯函数，无状态。
 *
 * <p>接收标量参数，输出写入调用方提供的 {@code float[16]}（列主序，OpenGL 兼容）。
 * 不依赖 Clojure 数据结构——Clojure 侧的 viewport->view-mat4 负责解构 map 后调用这里。
 *
 * <p>矩阵约定：列主序，m[12..14] 为平移列。右手系，Y-up，相机朝向 -Z。
 */
public final class ViewportMath {
    private ViewportMath() {
    }

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
            double azimuth,
            double elevation,
            float[] out) {
        final double cosA = Math.cos(azimuth);
        final double sinA = Math.sin(azimuth);
        final double cosE = Math.cos(elevation);
        final double sinE = Math.sin(elevation);

        // 相机位置
        final double ex = tx + distance * cosE * sinA;
        final double ey = ty + distance * sinE;
        final double ez = tz + distance * cosE * cosA;

        // 基向量
        final double rx = cosA, ry = 0.0, rz = -sinA;
        final double ux = -sinA * sinE, uy = cosE, uz = -cosA * sinE;
        final double fx = -cosE * sinA, fy = -sinE, fz = -cosE * cosA;

        // 平移项
        final double m12 = -(rx * ex + ry * ey + rz * ez);
        final double m13 = -(ux * ex + uy * ey + uz * ez);
        final double m14 = (fx * ex + fy * ey + fz * ez);

        Arrays.fill(out, 0.0f);
        out[0] = (float) rx;
        out[1] = (float) ux;
        out[2] = (float) (-fx);
        out[4] = (float) ry;
        out[5] = (float) uy;
        out[6] = (float) (-fy);
        out[8] = (float) rz;
        out[9] = (float) uz;
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
     *
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
     *
     * 展开：
     *   delta.x = -cosA·dx·wpp + (-sinA·sinE)·dy·wpp
     *   delta.y =                  cosE·dy·wpp
     *   delta.z =  sinA·dx·wpp + (-cosA·sinE)·dy·wpp
     * </pre>
     *
     * <p>符号说明：屏幕 y 向下、世界 y 向上，故 dy 直接乘 +up 即得“内容下移”；
     * dx 乘 -right 即得“内容右移”。若需要“相机跟随鼠标”的反向语义，
     * 调用方传入 (-dx, -dy)。
     *
     * <p>前置条件：elevation 严格在 (-π/2, π/2) 内（spec 保证）；outDelta.length &gt;= 3。
     *
     * @param azimuth      绕 Y 轴方位角，弧度
     * @param elevation    仰角，弧度
     * @param dx           屏幕 x 位移，像素
     * @param dy           屏幕 y 位移，像素（向下为正）
     * @param worldPerPixel 每像素世界长度（由上面的 wpp 方法给出）
     * @param outDelta     输出数组，长度 &gt;= 3
     */
    public static void panDelta(
            double azimuth, double elevation,
            double dx, double dy, double worldPerPixel,
            float[] outDelta) {
        final double cosA = Math.cos(azimuth);
        final double sinA = Math.sin(azimuth);
        final double cosE = Math.cos(elevation);
        final double sinE = Math.sin(elevation);

        final double sx = -dx * worldPerPixel;  // 沿 -right 的分量
        final double sy =  dy * worldPerPixel;  // 沿 +up 的分量

        outDelta[0] = (float) (cosA * sx + (-sinA * sinE) * sy);
        outDelta[1] = (float) (cosE * sy);
        outDelta[2] = (float) (-sinA * sx + (-cosA * sinE) * sy);
    }
}