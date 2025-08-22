package com.aiaca.btop.board.mapper;

import com.aiaca.btop.board.domain.BoardInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BoardMapper {
    // 1. 조회
    List<BoardInfo> getBoardList();     // 전체 게시판 로드

    // 특정회원의 작성글 로드
    List<BoardInfo> getBoardList(@Param("memberNo") String memberNo);

    // 특정 게시물 로드
    BoardInfo getBoardInfo(@Param("boardNo") long boardNo);

    // 게시판 총 갯수
    int getBoardCount();

    // 게시물 게시
    void insertBoardInfo(BoardInfo boardInfo);

    // 게시물 수정
    void updateBoardInfo(BoardInfo boardInfo);

    // 게시물 삭제
    void deleteBoardInfo (@Param("boardNo") long boardNo);

    //조회수 로드
    void incresaeViews(@Param("boardNo") long boardNo);

}
