package com.zhouzh3.chess.util;

import com.zhouzh3.chess.model.Coordinate;
import com.zhouzh3.chess.model.RgbColor;

import java.util.List;
import java.util.Map;

/**
 * @author haig
 */
public class ColorUtil {
    public static Coordinate findClosestColor(
            Map<Coordinate, RgbColor> map, RgbColor target) {

        Map.Entry<Coordinate, RgbColor> closestEntry = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<Coordinate, RgbColor> entry : map.entrySet()) {
            RgbColor c = entry.getValue();
            double distance = squaredDistance(c, target);

            if (distance < minDistance) {
                minDistance = distance;
                closestEntry = entry;
            }
        }
        return closestEntry.getKey();
    }

    /**
     * 用欧几里得距离或曼哈顿距离来衡量平均值与参考颜色的差异。
     * */
    private static double squaredDistance(RgbColor c1, RgbColor c2) {
        return Math.pow(c1.r() - c2.r(), 2)
                + Math.pow(c1.g() - c2.g(), 2)
                + Math.pow(c1.b() - c2.b(), 2);
    }


    public static RgbColor average(List<RgbColor> colors) {
        int size = colors.size();
        if (size == 0) {
            throw new IllegalArgumentException("List is empty");
        }

        long sumR = 0, sumG = 0, sumB = 0;
        for (RgbColor c : colors) {
            sumR += c.r();
            sumG += c.g();
            sumB += c.b();
        }
        return new RgbColor((int) (sumR / size), (int) (sumG / size), (int) (sumB / size));
    }
}
