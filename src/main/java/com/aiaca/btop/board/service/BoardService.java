package com.aiaca.btop.board.service;

import com.aiaca.btop.board.domain.BoardInfo;

import java.util.List;

public interface BoardService {
    List<BoardInfo> getBoardList();
    List<BoardInfo> getBoardList(String memberNo);
    BoardInfo getBoardInfo(long boardNo);
    int getBoardCount();
    void insertBoardInfo(BoardInfo boardInfo);
    void updateBoard(BoardInfo boardInfo);
    void deleteBoard(long boardNo);
    void increaseViws(long boardNo);
}
