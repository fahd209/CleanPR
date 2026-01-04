package com.fahd.cleanPR.model;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private String side;
    private int line;
    String path;
    String body;
}
