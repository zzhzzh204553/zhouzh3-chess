package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.fen.Board;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

public class ImageMatcher {

    public static final int END_ROW = 9;
    public static final int END_COL = 8;

    public static class Result {
        String name;
        String path;
        int hamming;
        double ssim;
        double score;

        public Result(String name, String path, int hamming, double ssim, double score) {
            this.name = name;
            this.path = path;
            this.hamming = hamming;
            this.ssim = ssim;
            this.score = score;
        }

    }

    public List<Result> compareAll(String inputPath, Map<String, String> candidatePaths) throws IOException {
        String inputHash;
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream resourceAsStream = classLoader.getResourceAsStream(inputPath);
        if (resourceAsStream == null) {
            System.out.println("图片不存在" + inputPath);
        }
        BufferedImage inputBufferedImage = ImageIO.read(Objects.requireNonNull(resourceAsStream));
        inputHash = PHash.getHash(inputBufferedImage);

        List<Result> results = new ArrayList<>();

        for (Entry<String, String> candidate : candidatePaths.entrySet()) {
            BufferedImage bufferedImage = ImageIO.read(Objects.requireNonNull(classLoader.getResourceAsStream(candidate.getValue())));
            String candidateHash = PHash.getHash(bufferedImage);
            int hamming = PHash.hammingDistance(inputHash, candidateHash);
            double ssim = SSIM.computeSSIM(inputBufferedImage, ImageIO.read(Objects.requireNonNull(classLoader.getResourceAsStream(candidate.getValue()))));

            // 综合评分：你可以调整权重
            double score = (1.0 / (1 + hamming)) * 0.5 + ssim * 0.5;
            results.add(new Result(candidate.getKey(), candidate.getValue(), hamming, ssim, score));
        }

        // 按 score 排序
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results;
    }

    public static void main(String[] args) throws Exception {

        match();
// 红色
//        System.out.println("\u001B[31m这是红色的文字\u001B[0m");
//        // 绿色
//        System.out.println("\u001B[32m这是绿色的文字\u001B[0m");
//        // 黄色
//        System.out.println("\u001B[33m这是黄色的文字\u001B[0m");
//        // 白色
//        System.out.println("\u001B[37m这是白色的文字\u001B[0m");
    }

    private static void match() throws IOException {
//        List<String> candidates = Arrays.asList(
//                "images/red-ju.png",
//                "images/red-ma.png",
//                "images/red-xiang.png",
//                "images/red-shi.png",
//                "images/red-shuai.png",
//                "images/red-pao.png",
//                "images/red-bing.png",
//                "images/black-ju.png",
//                "images/black-ma.png",
//                "images/black-xiang.png",
//                "images/black-shi.png",
//                "images/black-jiang.png",
//                "images/black-pao.png",
//                "images/black-zu.png"
//        );

        Map<String, String> candidates = Map.ofEntries(
                Map.entry("R红车", "images/red-ju.png"),
                Map.entry("N红马", "images/red-ma.png"),
                Map.entry("B红相", "images/red-xiang.png"),
                Map.entry("A红仕", "images/red-shi.png"),
                Map.entry("K红帅", "images/red-shuai.png"),
                Map.entry("C红炮", "images/red-pao.png"),
                Map.entry("P红兵", "images/red-bing.png"),
                Map.entry("r黑车", "images/black-ju.png"),
                Map.entry("n黑马", "images/black-ma.png"),
                Map.entry("b黑象", "images/black-xiang.png"),
                Map.entry("a黑士", "images/black-shi.png"),
                Map.entry("k黑将", "images/black-jiang.png"),
                Map.entry("c黑炮", "images/black-pao.png"),
                Map.entry("p黑卒", "images/black-zu.png")
        );

        List<List<String>> inputImages = Arrays.asList(
                Arrays.asList("chess/cell_0_0.png", "chess/cell_0_1.png", "chess/cell_0_2.png", "chess/cell_0_3.png", "chess/cell_0_4.png", "chess/cell_0_5.png", "chess/cell_0_6.png", "chess/cell_0_7.png", "chess/cell_0_8.png"),
                Arrays.asList("chess/cell_1_0.png", "chess/cell_1_1.png", "chess/cell_1_2.png", "chess/cell_1_3.png", "chess/cell_1_4.png", "chess/cell_1_5.png", "chess/cell_1_6.png", "chess/cell_1_7.png", "chess/cell_1_8.png"),
                Arrays.asList("chess/cell_2_0.png", "chess/cell_2_1.png", "chess/cell_2_2.png", "chess/cell_2_3.png", "chess/cell_2_4.png", "chess/cell_2_5.png", "chess/cell_2_6.png", "chess/cell_2_7.png", "chess/cell_2_8.png"),
                Arrays.asList("chess/cell_3_0.png", "chess/cell_3_1.png", "chess/cell_3_2.png", "chess/cell_3_3.png", "chess/cell_3_4.png", "chess/cell_3_5.png", "chess/cell_3_6.png", "chess/cell_3_7.png", "chess/cell_3_8.png"),
                Arrays.asList("chess/cell_4_0.png", "chess/cell_4_1.png", "chess/cell_4_2.png", "chess/cell_4_3.png", "chess/cell_4_4.png", "chess/cell_4_5.png", "chess/cell_4_6.png", "chess/cell_4_7.png", "chess/cell_4_8.png"),
                Arrays.asList("chess/cell_5_0.png", "chess/cell_5_1.png", "chess/cell_5_2.png", "chess/cell_5_3.png", "chess/cell_5_4.png", "chess/cell_5_5.png", "chess/cell_5_6.png", "chess/cell_5_7.png", "chess/cell_5_8.png"),
                Arrays.asList("chess/cell_6_0.png", "chess/cell_6_1.png", "chess/cell_6_2.png", "chess/cell_6_3.png", "chess/cell_6_4.png", "chess/cell_6_5.png", "chess/cell_6_6.png", "chess/cell_6_7.png", "chess/cell_6_8.png"),
                Arrays.asList("chess/cell_7_0.png", "chess/cell_7_1.png", "chess/cell_7_2.png", "chess/cell_7_3.png", "chess/cell_7_4.png", "chess/cell_7_5.png", "chess/cell_7_6.png", "chess/cell_7_7.png", "chess/cell_7_8.png"),
                Arrays.asList("chess/cell_8_0.png", "chess/cell_8_1.png", "chess/cell_8_2.png", "chess/cell_8_3.png", "chess/cell_8_4.png", "chess/cell_8_5.png", "chess/cell_8_6.png", "chess/cell_8_7.png", "chess/cell_8_8.png"),
                Arrays.asList("chess/cell_9_0.png", "chess/cell_9_1.png", "chess/cell_9_2.png", "chess/cell_9_3.png", "chess/cell_9_4.png", "chess/cell_9_5.png", "chess/cell_9_6.png", "chess/cell_9_7.png", "chess/cell_9_8.png")
        );

        ImageMatcher imageMatcher = new ImageMatcher();

        List<Tuple> validAdvisors = Arrays.asList(Tuple.of(0, 3), Tuple.of(0, 5), Tuple.of(2, 3), Tuple.of(2, 5));

        Board board = new Board();
        for (int row = 0; row <= END_ROW; row++) {
            System.out.print("row " + row + ": ");
            for (int col = 0; col <= END_COL; col++) {
                String inputImage = inputImages.get(row).get(col);
                System.out.print(col + "-");
                List<Result> results = imageMatcher.compareAll(inputImage, candidates);
//                System.out.println("相似度排名：");
//                for (Result r : results) {
//                    System.out.printf("Image: %s | Hamming: %d | SSIM: %.4f | Score: %.4f%n", r.path, r.hamming, r.ssim, r.score);
//                }
                char name = results.get(0).name.charAt(0);
//                String color = name.contains("红") ? "\u001B[31m" + name.substring(name.length() - 1) + "\u001B[0m" : "\u001B[37m" + name.substring(name.length() - 1) + "\u001B[0m";
//                System.out.print(color + "\t");
                if ('a' == (name) && !validAdvisors.contains(Tuple.of(row, col))) {
                    name = '.';
                } else if ('A' == name && !validAdvisors.contains(Tuple.of(END_ROW - row, col))) {
                    name = '.';
                }

                board.setPiece(row, col, name);
            }
            System.out.println();
        }
        String fen = board.toFen();
        System.out.println(fen);
    }

}
