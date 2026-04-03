package com.zhouzh3.chess.vision;

import lombok.Getter;

/**
 * @author haig
 */

@Getter
public enum ImageType {
    // jpg, png
    JPG("jpg"), PNG("png");

    private final String extension;

    ImageType(String extension) {
        this.extension = extension;
    }
}
