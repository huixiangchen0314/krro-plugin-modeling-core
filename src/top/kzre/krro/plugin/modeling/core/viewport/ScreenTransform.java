package top.kzre.krro.plugin.modeling.core.viewport;

import top.kzre.krro.d3.core.geometry.Point2D;
import top.kzre.krro.d3.core.geometry.Ray;

/**
 * 屏幕 ↔ 世界坐标变换。纯函数，无状态。
 *
 * 注意，krro 对世界坐标标准和 GL 一致。
 *
 * <p>矩阵为列主序 float[16]（与 ViewportMath 一致）。屏幕坐标原点在
 * 左上角、y 向下；NDC 原点在中心、y 向上；NDC z 范围 [-1, 1]（OpenGL 约定）。
 *
 * <p>viewProj 的逆矩阵由调用方预先计算（KMath.mat4Inv）。本类不重复求逆——
 * 逆矩阵在同一帧内被多次使用（拾取、框选、拖拽），每次重算浪费。
 *
 * <p>前置条件：w、h 均为正（视口尺寸就绪由上层保证）；传入的矩阵是有效的
 * 列主序 4×4，invViewProj 确实是 viewProj 的逆。
 */
public final class ScreenTransform {

    private ScreenTransform() {}

    /** w 分量低于此值视为点在相机后方或裁剪面上，屏幕坐标无意义。 */
    private static final double W_EPSILON = 1e-9;

    /**
     * 屏幕点 → 世界空间射线。
     *
     * <pre>
     *   1. 屏幕 (x, y) → NDC：
     *        ndcX = 2x/w - 1
     *        ndcY = 1 - 2y/h
     *   2. 近点 NDC = (ndcX, ndcY, -1, 1)
     *      远点 NDC = (ndcX, ndcY, +1, 1)
     *   3. 乘 invViewProj → 世界空间齐次坐标
     *   4. 透视除法 → 世界空间点
     *   5. 射线起点 = 近点，方向 = normalize(远点 - 近点)
     * </pre>
     *
     * <p>方向归一化：后续 ray-AABB / ray-triangle 求交返回的 t 即世界距离，
     * 不依赖方向长度。
     *
     * @param invViewProj viewProj 的逆矩阵，列主序，由调用方计算
     * @param x           屏幕 x，像素（左上原点）
     * @param y           屏幕 y，像素（左上原点，向下为正）
     * @param w           视口宽，像素
     * @param h           视口高，像素
     * @return 世界空间射线，起点在近裁剪面，方向已归一化
     */
    public static Ray screenPointToWorldRay(
            float[] invViewProj, double x, double y,
            int w, int h) {
        final double ndcX = 2.0 * x / w - 1.0;
        final double ndcY = 1.0 - 2.0 * y / h;

        final double[] near = mulVec4(invViewProj, ndcX, ndcY, -1.0, 1.0);
        final double[] far  = mulVec4(invViewProj, ndcX, ndcY,  1.0, 1.0);

        final double nearX = near[0] / near[3];
        final double nearY = near[1] / near[3];
        final double nearZ = near[2] / near[3];
        final double farX  = far[0] / far[3];
        final double farY  = far[1] / far[3];
        final double farZ  = far[2] / far[3];

        double dirX = farX - nearX;
        double dirY = farY - nearY;
        double dirZ = farZ - nearZ;

        return Ray.normalized((float) nearX, (float) nearY, (float) nearZ,
                (float) dirX, (float) dirY, (float) dirZ);
    }

    /**
     * 世界点 → 屏幕点。
     *
     * <pre>
     *   1. 世界点 (x, y, z, 1) 乘 viewProj → 裁剪空间齐次坐标
     *   2. w &lt;= ε：点在相机后方或裁剪面上，返回 null
     *   3. 透视除法 → NDC
     *   4. NDC → 屏幕：
     *        sx = (ndcX + 1) / 2 · w
     *        sy = (1 - ndcY) / 2 · h
     * </pre>
     *
     * <p>返回 null 而不是带 NaN 的 Point2D：调用方需要显式判断可见性，
     * 而非不小心把 NaN 传给下游。这在投影中心附近（w 接近 0）尤其重要。
     *
     * @param viewProj 合并矩阵，列主序
     * @param x        世界坐标 x
     * @param y        世界坐标 y
     * @param z        世界坐标 z
     * @param w        视口宽，像素
     * @param h        视口高，像素
     * @return 屏幕坐标；点在相机后方（w &lt;= ε）时返回 null
     */
    public static Point2D worldPointToScreenPoint(
            float[] viewProj, double x, double y, double z,
            int w, int h) {
        final double[] clip = mulVec4(viewProj, x, y, z, 1.0);
        if (clip[3] <= W_EPSILON) {
            return null;
        }
        final double invW = 1.0 / clip[3];
        final double ndcX = clip[0] * invW;
        final double ndcY = clip[1] * invW;
        final double sx   = (ndcX + 1.0) * 0.5 * w;
        final double sy   = (1.0 - ndcY) * 0.5 * h;
        return new Point2D((float) sx, (float) sy);
    }

    /**
     * 列主序 4×4 矩阵乘齐次列向量。
     *
     * <pre>
     *   out[r] = Σ_c m[r + 4c] · v[c]
     * </pre>
     */
    private static double[] mulVec4(float[] m, double x, double y, double z, double w) {
        // 不使用 KMath, KMath 要求包装为布局float,这种基础数学重复没关系， 后面再整合进KMath
        return new double[] {
                m[0] * x + m[4] * y + m[8]  * z + m[12] * w,
                m[1] * x + m[5] * y + m[9]  * z + m[13] * w,
                m[2] * x + m[6] * y + m[10] * z + m[14] * w,
                m[3] * x + m[7] * y + m[11] * z + m[15] * w
        };
    }
}