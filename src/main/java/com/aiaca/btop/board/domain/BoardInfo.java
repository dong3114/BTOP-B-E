package com.aiaca.btop.board.domain;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BoardInfo {
    private  long boardNo;
    private  LocalDateTime createdAt;
    private  String memberNo;

    private int views;
    private String boardTitle;
    private String boardDetail;
    private LocalDateTime updatedAt;

    //생성자
    public BoardInfo(long boardNo, String memberNo, String boardTitle, String boardDetail) {
        this.boardNo = boardNo;
        this.memberNo = memberNo;
        this.boardTitle = boardTitle;
        this.boardDetail = boardDetail;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
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
