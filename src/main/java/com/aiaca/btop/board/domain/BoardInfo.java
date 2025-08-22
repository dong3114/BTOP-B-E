package com.aiaca.btop.board.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BoardInfo {
    private final long boardNo;
    private final LocalDateTime createdAt;

    private int views;
    private String boardTitle;
    private String boardDetail;
    private LocalDateTime updatedAt;
    private int count;

    //생성자
    public BoardInfo(long boardNo, String boardTitle, String boardDetail) {
        this.boardNo = boardNo;
        this.boardTitle = boardTitle;
        this.boardDetail = boardDetail;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    public void update(String boardTitle, String boardDetail) {
        this.boardDetail = boardDetail;
        this.boardTitle = boardTitle;
        this.updatedAt = LocalDateTime.now();
    }
}
