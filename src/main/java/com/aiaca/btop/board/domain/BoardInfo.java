package com.aiaca.btop.board.domain;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BoardInfo {
    private long boardNo;
    private LocalDateTime createdAt;
    private String memberNo;

    private int views;
    private String boardTitle;
    private String boardDetail;
    private LocalDateTime updatedAt;
    // 게시물 수정
    public void update(String boardTitle, String boardDetail) {
        this.boardDetail = boardDetail;
        this.boardTitle = boardTitle;
        this.updatedAt = LocalDateTime.now();
    }
    // 조회수
    public void increaseViews() {
        this.views++;
    }
}
